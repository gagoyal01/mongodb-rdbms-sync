package com.cisco.app.dbmigrator.migratorapp.core.thread;

import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.core.event.SyncError;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncMarker;
import com.cisco.app.dbmigrator.migratorapp.core.job.NodeBalancer;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncAttrs;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncEventDao;
import com.cisco.app.dbmigrator.migratorapp.mail.Mailer;
import com.cisco.app.dbmigrator.migratorapp.utilities.cache.DBCacheManager;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;

public class OrclToMngWriter implements Runnable {

	private final Logger logger = Logger.getLogger(OrclToMngWriter.class);

	private BlockingQueue<List<Document>> dataBuffer;
	private SyncMarker marker;

	private final String mongoUserName;
	private final String mongoDbName;
	private final String collectionName;
	private final ObjectId eventId;
	private final SyncEventDao eventDao;
	private final CountDownLatch latch;
	private final UpdateOptions options = new UpdateOptions().upsert(true);

	public OrclToMngWriter(BlockingQueue<List<Document>> dataBuffer, String mongoDbName, String mongoUserName,
			SyncMarker marker, ObjectId eventId, String collectionName, CountDownLatch latch) {
		super();
		this.dataBuffer = dataBuffer;
		this.mongoUserName = mongoUserName;
		this.mongoDbName = mongoDbName;
		this.marker = marker;
		this.collectionName = collectionName;
		this.eventId = eventId;
		this.latch = latch;
		this.eventDao = new SyncEventDao();
	}

	@Override
	public void run() {
		MongoCollection<Document> collection = DBCacheManager.INSTANCE.getCachedMongoPool(mongoDbName, mongoUserName)
				.getDatabase(mongoDbName).getCollection(collectionName);
		List<Document> docs = null;
		try {
			while (true) {
				if ((marker.getRowsDumped() == marker.getTotalRows()) || marker.isFailed()) {
					break;
				}

				docs = dataBuffer.take();
				if (docs == null || docs.isEmpty()) {
					continue;
				}
				try {
					collection.insertMany(docs);
				} catch (Exception e) {
					logger.warn("Error while bulk inserting documents. Retry inserting one document at a time", e);
					collection= DBCacheManager.INSTANCE.getCachedMongoPool(mongoDbName, mongoUserName)
					.getDatabase(mongoDbName).getCollection(collectionName);
					for (Document doc : docs) {
						try {
							if (doc.get(SyncAttrs.ID) != null) {
								collection.replaceOne(Filters.eq(SyncAttrs.ID, doc.get(SyncAttrs.ID)), doc, options);
							} else {
								collection.insertOne(doc);
							}
						} catch (Exception e1) {
							logger.error("Error while inserting document ", e1);
							collection= DBCacheManager.INSTANCE.getCachedMongoPool(mongoDbName, mongoUserName)
									.getDatabase(mongoDbName).getCollection(collectionName);
							Mailer.sendmail(eventId, String.valueOf(docs), e1, Mailer.FAILURE);
						}
					}
				}
				marker.setRowsDumped(marker.getRowsDumped() + docs.size());
				eventDao.updateMarker(eventId, marker);
				docs = null;
				logger.info("Inserted documentbatch successfully");
			}
			logger.info("Writer Finished writing");
			marker.setEndTime(new Date());
			eventDao.updateMarker(eventId, marker);
			NodeBalancer.INSTANCE.markEventAsCompleted(eventId);
		} catch (Exception e) {
			logger.error("Error while getting processed Document", e);
			marker.setFailed(true);
			SyncError error = new SyncError(e);
			error.setThreadName(Thread.currentThread().getName());
			eventDao.updateMarker(eventId, marker);
			eventDao.pushError(eventId, error);
			NodeBalancer.INSTANCE.markEventAsFailed(eventId);
			Mailer.sendmail(eventId, String.valueOf(docs), e, Mailer.FAILURE);
		} finally {
			marker = null;
			dataBuffer = null;
			latch.countDown();
		}
	}
}
