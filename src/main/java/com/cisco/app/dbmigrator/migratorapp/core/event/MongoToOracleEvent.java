/**
 * 
 */
package com.cisco.app.dbmigrator.migratorapp.core.event;

import java.util.concurrent.LinkedBlockingQueue;

import org.bson.BsonDocument;
import org.bson.BsonDocumentWrapper;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;

import com.cisco.app.dbmigrator.migratorapp.core.map.MongoToOracleMap;
import com.cisco.app.dbmigrator.migratorapp.core.thread.MngToOrclReader;
import com.cisco.app.dbmigrator.migratorapp.core.thread.MngToOrclWriter;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncMapDao;

/**
 * Runnable class to process MongoDB to OracleDB migration (one time data transfer) events.
 * 
 * @author pnilayam
 *
 */
public class MongoToOracleEvent extends SyncEvent<Document>{
	private String collectionName;
	@Override
	public void run() {
		mappingDao = new SyncMapDao();
		dataBuffer= new LinkedBlockingQueue<Document>(batchSize);
		MongoToOracleMap map = (MongoToOracleMap) mappingDao.getMapping(mapId);
		MngToOrclReader reader = new MngToOrclReader(dataBuffer, map.getSourceDbName(), map.getSourceUserName(), map.getCollectionName(), batchSize);
		Thread readerThread = new Thread(reader);
		readerThread.setName(eventName+"_Reader");
		readerThread.start();
		MngToOrclWriter writer = new MngToOrclWriter(dataBuffer, map);
		Thread writerThread = new Thread(writer);
		writerThread.setName(eventName+"_Writer");
		writerThread.start();
	}
	
	@Override
	public <TDocument> BsonDocument toBsonDocument(Class<TDocument> arg0, CodecRegistry codecRegistry) {
		return new BsonDocumentWrapper<MongoToOracleEvent>(this, codecRegistry.get(MongoToOracleEvent.class));
	}

	public String getCollectionName() {
		return collectionName;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}
	
}
