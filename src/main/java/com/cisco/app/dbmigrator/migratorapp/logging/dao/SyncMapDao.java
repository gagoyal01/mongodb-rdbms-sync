package com.cisco.app.dbmigrator.migratorapp.logging.dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.core.map.SyncMap;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncAttrs;
import com.cisco.app.dbmigrator.migratorapp.logging.connection.ApplicationCollections;
import com.cisco.app.dbmigrator.migratorapp.logging.connection.MongoConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;

public class SyncMapDao{
	MongoCollection<SyncMap> syncMappings = MongoConnection.INSTANCE.getMongoDataBase()
			.getCollection(String.valueOf(ApplicationCollections.SyncMappings), SyncMap.class);

	public SyncMap saveMapping(SyncMap map) {
		// TODO : check why this is needed
		if (map.getMapId() == null) {
			map.setMapId(new ObjectId());
		}
		return syncMappings.findOneAndReplace(Filters.eq(SyncAttrs.ID, map.getMapId()), map,
				new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(true));
	}

	public SyncMap getMapping(ObjectId mappingId) {
		return syncMappings.find(Filters.eq(SyncAttrs.ID, mappingId)).first();
	}

	public List<SyncMap> getAllMapping() {
		List<SyncMap> mapList = new ArrayList<SyncMap>();
		syncMappings.find().projection(Projections.exclude(SyncAttrs.MAP_OBJECT)).sort(Sorts.descending(SyncAttrs.CREATED_ON)).into(mapList);
		return mapList;
	}

	public void deleteMapping(ObjectId objectId) {
		syncMappings.deleteOne(Filters.eq(SyncAttrs.ID, objectId));
	}
	
	public List<SyncMap> getMappingByMapType(String mapType) {
		List<SyncMap> mapList = new ArrayList<SyncMap>();
		syncMappings.find(Filters.eq(SyncAttrs.MAP_TYPE, mapType)).projection(Projections.exclude(SyncAttrs.MAP_OBJECT)).sort(Sorts.descending(SyncAttrs.CREATED_ON)).into(mapList);
		return mapList;
	}
}
