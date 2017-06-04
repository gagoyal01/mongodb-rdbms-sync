package com.cisco.app.dbmigrator.migratorapp.logging.dao;

import org.apache.log4j.Logger;
import org.bson.conversions.Bson;

import com.cisco.app.dbmigrator.migratorapp.config.SyncConfig;
import com.cisco.app.dbmigrator.migratorapp.constants.SyncConstants;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncAttrs;
import com.cisco.app.dbmigrator.migratorapp.logging.connection.ApplicationCollections;
import com.cisco.app.dbmigrator.migratorapp.logging.connection.MongoConnection;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncNode;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;

public class SyncNodeDao {

	private final Logger logger = Logger.getLogger(getClass());
	MongoCollection<SyncNode> syncNodeMapping = MongoConnection.INSTANCE.getMongoDataBase()
			.getCollection(String.valueOf(ApplicationCollections.SyncNodeMapping), SyncNode.class);

	public SyncNode updateNodeDetails(SyncNode nodeMapper) {
		return syncNodeMapping.findOneAndReplace(Filters.eq(SyncAttrs.ID, nodeMapper.getId()), nodeMapper,
				new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(true));
	}

	public SyncNode getNodeDetails(SyncNode nodeMapper) {
		Bson filter = Filters.eq(SyncAttrs.UUID, nodeMapper.getUUID());
		logger.info("Getting node with filter " + filter);
		return syncNodeMapping.findOneAndUpdate(filter, Updates.unset(SyncAttrs.FAILURE_TIME),
				new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
	}

	public SyncNode getFailedNode(long lastPingTime) {
		SyncNode failedNode = syncNodeMapping.findOneAndUpdate(
				Filters.and(Filters.lte(SyncAttrs.LAST_PING_TIME, lastPingTime),
						Filters.eq(SyncAttrs.LIFE_CYCLE, SyncConfig.INSTANCE.getDbProperty(SyncConstants.LIFE))),
				Updates.set(SyncAttrs.LAST_PING_TIME, System.currentTimeMillis()),
				new FindOneAndUpdateOptions().upsert(false).returnDocument(ReturnDocument.BEFORE));
		if (failedNode != null && failedNode.getFailureTime() == 0) {
			syncNodeMapping.findOneAndUpdate(Filters.eq(SyncAttrs.ID, failedNode.getId()),
					Updates.set(SyncAttrs.FAILURE_TIME, failedNode.getLastPingTime()));
		}
		return failedNode;
	}
}
