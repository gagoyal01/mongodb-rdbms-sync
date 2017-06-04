package com.cisco.app.dbmigrator.migratorapp.logging.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.Document;

import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncAttrs;
import com.cisco.app.dbmigrator.migratorapp.logging.connection.ApplicationCollections;
import com.cisco.app.dbmigrator.migratorapp.logging.connection.MongoConnection;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncNode;
import com.cisco.app.dbmigrator.migratorapp.logging.entity.SyncNodeDetails;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UnwindOptions;

public class SyncNodeDetailsDao {

	MongoCollection<SyncNode> migrationNodeMapping = MongoConnection.INSTANCE.getMongoDataBase()
			.getCollection(String.valueOf(ApplicationCollections.SyncNodeMapping), SyncNode.class);

	public List<SyncNodeDetails> getNodeDetails(String lifeCycle) {
		UnwindOptions options = new UnwindOptions();
		options.preserveNullAndEmptyArrays(true);
		Document group = new Document("$group",
				new Document(SyncAttrs.ID, new Document("_id", "$_id").append("host","$host").append("node","$node").append("state","$state")
						.append("concurrencyLevel","$concurrencyLevel").append("totalHeapSize", "$totalHeapSize")
						.append("usedHeapSize", "$usedHeapSize").append("lifeCycle", "$lifeCycle"))
						.append("eventArr", new Document("$addToSet", "$event_docs")));
		return migrationNodeMapping.aggregate(Arrays.asList(Aggregates.match(Filters.eq(SyncAttrs.LIFE_CYCLE,lifeCycle)),
				Aggregates.unwind("$activeEvents",options),
				Aggregates.lookup("SyncEvents", "activeEvents", "_id", "event_docs"),
				Aggregates.unwind("$event_docs", options),
				group,Aggregates.project(new Document("node", "$_id").append("events","$eventArr").append("_id", false))), SyncNodeDetails.class)
				.into(new ArrayList<SyncNodeDetails>());
	}
}
