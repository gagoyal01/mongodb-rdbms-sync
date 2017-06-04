package com.cisco.app.dbmigrator.migratorapp.core.map;

import org.bson.BsonDocument;
import org.bson.BsonDocumentWrapper;
import org.bson.codecs.configuration.CodecRegistry;

import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoObject;

public class OracleToMongoMap extends SyncMap{
	private MongoObject mapObject;
	
	public MongoObject getMapObject() {
		return mapObject;
	}
	public void setMapObject(MongoObject mapObject) {
		this.mapObject = mapObject;
	}
	@Override
	public <TDocument> BsonDocument toBsonDocument(Class<TDocument> arg0, CodecRegistry codecRegistry) {
		return new BsonDocumentWrapper<OracleToMongoMap>(this, codecRegistry.get(OracleToMongoMap.class));
	}
}
