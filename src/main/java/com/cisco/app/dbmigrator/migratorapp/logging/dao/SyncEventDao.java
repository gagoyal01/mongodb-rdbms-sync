package com.cisco.app.dbmigrator.migratorapp.logging.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.bson.BsonTimestamp;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.core.event.EventType;
import com.cisco.app.dbmigrator.migratorapp.core.event.O2MSyncDataLoader;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncError;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncEvent;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncMarker;
import com.cisco.app.dbmigrator.migratorapp.core.job.SyncStatus;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncAttrs;
import com.cisco.app.dbmigrator.migratorapp.logging.connection.ApplicationCollections;
import com.cisco.app.dbmigrator.migratorapp.logging.connection.MongoConnection;
import com.cisco.app.dbmigrator.migratorapp.mail.Mailer;
import com.mongodb.Block;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;

@SuppressWarnings("rawtypes")
public class SyncEventDao {

	private final MongoCollection<SyncEvent> syncEvents = MongoConnection.INSTANCE.getMongoDataBase()
			.getCollection(String.valueOf(ApplicationCollections.SyncEvents), SyncEvent.class);

	private final MongoCollection<Document> syncEventDoc = MongoConnection.INSTANCE.getMongoDataBase()
			.getCollection(String.valueOf(ApplicationCollections.SyncEvents), Document.class);

	public SyncEvent getPendingEvent(List<String> eventTypes) {
		return syncEvents.findOneAndUpdate(
				Filters.and(Filters.eq(SyncAttrs.STATUS, SyncStatus.PENDING),
						Filters.in(SyncAttrs.EVENT_TYPE, eventTypes)),
				Updates.set(SyncAttrs.STATUS, SyncStatus.IN_PROGRESS),
				new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
	}

	public SyncEvent saveEvent(SyncEvent event) {
		if (event.getEventId() == null) {
			event.setEventId(new ObjectId());
		}
		return syncEvents.findOneAndReplace(Filters.eq(SyncAttrs.ID, event.getEventId()), event,
				new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(true));
	}

	public void bulkInsert(List<SyncEvent> eventList) {
		syncEvents.insertMany(eventList);
	}

	public List<SyncEvent> getAllEvents() {
		List<SyncEvent> eventList = new ArrayList<SyncEvent>();
		syncEvents.find().into(eventList);
		return eventList;
	}

	public List<SyncEvent> getAllEventsForUser(String userId) {
		List<SyncEvent> eventList = new ArrayList<SyncEvent>();
		syncEvents.find(Filters.eq(SyncAttrs.CREATED_BY, userId)).sort(Sorts.descending(SyncAttrs.CREATED_ON))
				.into(eventList);
		return eventList;
	}

	public void updateEventStatus(ObjectId eventId, String status) {
		syncEvents.updateOne(Filters.eq(SyncAttrs.ID, eventId), Updates.set(SyncAttrs.STATUS, status));
	}

	public SyncEvent getEvent(ObjectId eventId) {
		return syncEvents.find(Filters.eq(SyncAttrs.ID, eventId)).first();
	}

	public void deleteEvent(ObjectId eventId) {
		syncEvents.deleteOne(Filters.eq(SyncAttrs.ID, eventId));
		syncEvents.deleteMany(Filters.eq(SyncAttrs.PARENT_EVENT_ID, eventId));
	}

	public void markEventAsFailed(ObjectId eventId) {
		updateEventStatus(eventId, SyncStatus.FAILED);
		SyncEvent event = getEvent(eventId);
		if (event != null && event.getParentEventId() != null) {
			updateEventStatus(event.getParentEventId(), SyncStatus.FAILED);
		}
	}

	public void markEventAsCompleted(ObjectId eventId) {
		updateEventStatus(eventId, SyncStatus.COMPLETE);
		SyncEvent event = getEvent(eventId);
		long incompleteCount = syncEvents.count(Filters.and(Filters.ne(SyncAttrs.STATUS, SyncStatus.COMPLETE),
				Filters.ne(SyncAttrs.ID, event.getParentEventId()),
				Filters.eq(SyncAttrs.PARENT_EVENT_ID, event.getParentEventId())));
		if (incompleteCount == 0) {
			updateEventStatus(event.getParentEventId(), SyncStatus.COMPLETE);
			Mailer.sendmail(event.getParentEventId(), null, null, Mailer.COMPLETED);
		}
	}

	public void updateMarker(ObjectId eventId, SyncMarker marker) {
		syncEvents.findOneAndUpdate(Filters.eq(SyncAttrs.ID, eventId), Updates.set(SyncAttrs.MARKER, marker));
	}

	public void updateErrors(ObjectId eventId, List<SyncError> errors) {
		syncEvents.findOneAndUpdate(Filters.eq(SyncAttrs.ID, eventId), Updates.set(SyncAttrs.ERRORS, errors));
	}

	public void pushError(ObjectId eventId, SyncError error) {
		syncEvents.findOneAndUpdate(Filters.eq(SyncAttrs.ID, eventId), Updates.push(SyncAttrs.ERRORS, error));
		markEventAsFailed(eventId);
	}

	public SyncMarker getEventStats(ObjectId eventId) {
		Document group = new Document("$group",
				new Document(SyncAttrs.ID, null).append(SyncAttrs.TOTAL_ROWS, new Document("$sum", "$marker.totalRows"))
						.append(SyncAttrs.ROWS_READ, new Document("$sum", "$marker.rowsRead"))
						.append(SyncAttrs.ROWS_DUMPED, new Document("$sum", "$marker.rowsDumped"))
						.append(SyncAttrs.START_TIME, new Document("$min", "$marker.startTime"))
						.append(SyncAttrs.END_TIME, new Document("$max", "$marker.endTime")));
		return syncEvents.aggregate(Arrays.asList(Aggregates.match(Filters.eq(SyncAttrs.PARENT_EVENT_ID, eventId)),
				Aggregates.project(Projections.include(SyncAttrs.MARKER)), group), SyncMarker.class).first();
	}

	public List<SyncError> getEventErrors(ObjectId eventId) {
		Document group = new Document("$group",
				new Document(SyncAttrs.ID, null).append(SyncAttrs.ERRORS, new Document("$addToSet", "$errors")));
		return syncEvents.aggregate(
				Arrays.asList(Aggregates.match(Filters.eq(SyncAttrs.PARENT_EVENT_ID, eventId)),
						Aggregates.unwind("$errors"),
						Aggregates
								.project(Projections.include(SyncAttrs.ERRORS)),
						group, Aggregates.unwind("$errors"),
						Aggregates.project(new Document(SyncAttrs.ERROR_MESSAGE, "$errors.errorMessage")
								.append(SyncAttrs.TRACE, "$errors.trace")
								.append(SyncAttrs.THREAD_NAME, "$errors.threadName"))),
				SyncError.class).allowDiskUse(true).into(new ArrayList<SyncError>());
	}

	public void updateLastReadTime(ObjectId eventId, BsonTimestamp lastReadTime) {
		syncEvents.findOneAndUpdate(Filters.eq(SyncAttrs.ID, eventId),
				Updates.set(SyncAttrs.LAST_READ_TIME, lastReadTime));
	}

	public SyncEvent retryEvent(ObjectId eventId, boolean retryFailed, boolean retryEntire, boolean dropCollection) {
		Document updateQuery = new Document();
		updateQuery.append("$set", new Document(SyncAttrs.STATUS, SyncStatus.PENDING).append(SyncAttrs.IS_RETRY, true))
				.append("$unset", new Document(SyncAttrs.ERRORS, true).append(SyncAttrs.MARKER, true));
		if (retryFailed) {
			syncEvents.updateMany(
					Filters.and(Filters.eq(SyncAttrs.PARENT_EVENT_ID, eventId),
							Filters.eq(SyncAttrs.STATUS, SyncStatus.FAILED), Filters.ne(SyncAttrs.ID, eventId)),
					updateQuery);
			syncEvents.updateOne(Filters.eq(SyncAttrs.ID, eventId),
					Updates.set(SyncAttrs.STATUS, SyncStatus.IN_PROGRESS));
		} else {
			if (retryEntire) {
				syncEvents.updateMany(Filters.eq(SyncAttrs.PARENT_EVENT_ID, eventId),
						Updates.set(SyncAttrs.STATUS, SyncStatus.CANCELLED));
				syncEvents.updateOne(Filters.eq(SyncAttrs.ID, eventId), updateQuery);
			}
		}
		return getEvent(eventId);
	}

	public SyncEvent cancelEvent(ObjectId eventId) {
		syncEvents.updateMany(
				Filters.or(Filters.eq(SyncAttrs.PARENT_EVENT_ID, eventId), Filters.eq(SyncAttrs.ID, eventId)),
				Updates.set(SyncAttrs.STATUS, SyncStatus.CANCELLED));
		return getEvent(eventId);
	}

	public String checkEventStatus(ObjectId eventId) {
		return syncEvents.find(Filters.eq(SyncAttrs.ID, eventId), Document.class)
				.projection(Projections.include(SyncAttrs.STATUS)).first().getString(SyncAttrs.STATUS);
	}

	public List<ObjectId> checkCancelledEvents(final Set<ObjectId> activeEventList) {
		final List<ObjectId> cancelledEvents = new ArrayList<ObjectId>();
		syncEvents
				.find(Filters.and(Filters.in(SyncAttrs.ID, activeEventList),
						Filters.eq(SyncAttrs.STATUS, SyncStatus.CANCELLED)), Document.class)
				.projection(Projections.include(SyncAttrs.ID)).forEach(new Block<Document>() {
					@Override
					public void apply(Document arg0) {
						cancelledEvents.add(arg0.getObjectId(SyncAttrs.ID));
					}
				});
		return cancelledEvents;
	}

	public O2MSyncDataLoader getPendingDataLoader() {
		O2MSyncDataLoader loader = null;
		Document document = syncEventDoc.findOneAndUpdate(
				Filters.and(Filters.eq(SyncAttrs.STATUS, SyncStatus.PENDING),
						Filters.eq(SyncAttrs.EVENT_TYPE, String.valueOf(EventType.System))),
				Updates.set(SyncAttrs.STATUS, SyncStatus.IN_PROGRESS),
				new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
						.projection(Projections.include(SyncAttrs.SOURCE_DB_NAME, SyncAttrs.SOURCE_USER_NAME)));
		if (document != null && !document.isEmpty()) {
			Object interval = document.get(SyncAttrs.INTERVAL);
			String appName = document.getString(SyncAttrs.APPLICATION_NAME);
			if(interval!=null && interval instanceof Long){
				loader = new O2MSyncDataLoader((Long)interval, appName);
			}else{
				loader = new O2MSyncDataLoader(120000, appName);
			}
			loader.setEventId(document.getObjectId(SyncAttrs.ID));
			loader.setDbName(document.getString(SyncAttrs.SOURCE_DB_NAME));
			loader.setDbUserName(document.getString(SyncAttrs.SOURCE_USER_NAME));
			loader.setStatus(document.getString(SyncAttrs.STATUS));
		}
		return loader;
	}

	public O2MSyncDataLoader getDataLoader(ObjectId eventId) {
		O2MSyncDataLoader loader = null;
		Document document = syncEventDoc
				.find(Filters.and(Filters.eq(SyncAttrs.ID, eventId))).first();
		if (document != null && !document.isEmpty()) {
			Object interval = document.get(SyncAttrs.INTERVAL);
			String appName = document.getString(SyncAttrs.APPLICATION_NAME);
			if(interval!=null && interval instanceof Long){
				loader = new O2MSyncDataLoader((Long)interval , appName);
			}else{
				loader = new O2MSyncDataLoader(120000 , appName);
			}
			loader.setEventId(document.getObjectId(SyncAttrs.ID));
			loader.setDbName(document.getString(SyncAttrs.SOURCE_DB_NAME));
			loader.setDbUserName(document.getString(SyncAttrs.SOURCE_USER_NAME));
			loader.setStatus(document.getString(SyncAttrs.STATUS));
		}
		return loader;
	}

	public void updateLastReadTime(ObjectId eventId, Date lastReadTime) {
		syncEvents.findOneAndUpdate(Filters.eq(SyncAttrs.ID, eventId),
				Updates.set("pollInfo.lastReadTime", lastReadTime));
	}
}
