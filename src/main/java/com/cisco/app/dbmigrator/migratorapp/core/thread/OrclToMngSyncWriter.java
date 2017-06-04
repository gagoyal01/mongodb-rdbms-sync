package com.cisco.app.dbmigrator.migratorapp.core.thread;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.constants.SyncConstants;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncMarker;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoAttribute;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncAttrs;
import com.cisco.app.dbmigrator.migratorapp.mail.Mailer;
import com.cisco.app.dbmigrator.migratorapp.utilities.cache.DBCacheManager;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndReplaceOptions;

public class OrclToMngSyncWriter implements Runnable{
	private final Logger logger = Logger.getLogger(getClass());
	private String targetDbName;
	private String targetUserName;
	private String collectionName;
	private final List<MongoAttribute> keyAttributes;
	private BlockingQueue<List<Document>> dataBuffer;
	private int retryCount=0;
	private MongoCollection<Document> collection;
	private SyncMarker marker;
	private final ObjectId eventId;
	private long waitTime = 1;
	
	public OrclToMngSyncWriter(String targetDbName, String targetUserName, String collectionName,
			List<MongoAttribute> keyAttributes, BlockingQueue<List<Document>> dataBuffer, SyncMarker marker, ObjectId eventId) {
		super();
		this.targetDbName = targetDbName;
		this.targetUserName = targetUserName;
		this.collectionName = collectionName;
		this.keyAttributes = keyAttributes;
		this.dataBuffer = dataBuffer;
		this.marker = marker;
		this.eventId= eventId;
	}

	@Override
	public void run() {
		logger.info("OrclToMngSyncWriter started "+Thread.currentThread().getName());
		List<Document> docs = null;
		FindOneAndReplaceOptions options = new FindOneAndReplaceOptions();
		options.upsert(true);
		while(retryCount<5){
			try {
				if(marker.isFailed()){
					releaseResources();
					return;
				}
				waitTime=30000;
				docs = dataBuffer.take();
				if (docs == null || docs.isEmpty()) {
					continue;
				}
				if(collection==null){
					refreshCollectionHandle();
				}
				for(Document doc : docs){
					if(SyncConstants.DELETE.equals(doc.get(SyncConstants.OPERATION))){
						collection.findOneAndDelete(getFilter(doc));
						logger.info("Document deleted");
					}else{
						collection.findOneAndReplace(getFilter(doc), doc, options);
						logger.info("Document upserted");
					}					
				}				
			} catch (Exception e) {
				logger.error("Error while upserting document",e);
				collection =null;
				Mailer.sendmail(eventId, String.valueOf(docs), e, Mailer.FAILURE);
				retryCount++;
				waitTime*=retryCount;
				try {
					Thread.sleep(waitTime);
				} catch (InterruptedException e1) {
					logger.error("Thread interuupted in sleep", e1);
				}
			}	
		}
		logger.info("OrclToMngSyncWriter terminated " + Thread.currentThread().getName());
	}
	
	private Bson getFilter(Document doc){
		Document filter = new Document();
		//TODO : for nested identifier , logic needs to be built
		for(MongoAttribute keyAttribute : keyAttributes){
			if(keyAttribute.isIdentifier()){
				filter.append(SyncAttrs.ID, doc.get(SyncAttrs.ID));
			}else{
				filter.append(keyAttribute.getAttributeName(), doc.get(keyAttribute.getAttributeName()));
			}			
		}
		return filter;
	}
	
	private void refreshCollectionHandle() {
		collection = DBCacheManager.INSTANCE.getCachedMongoPool(targetDbName, targetUserName)
				.getDatabase(targetDbName).getCollection(collectionName);
	}
	
	private void releaseResources(){
		dataBuffer=null;
		marker=null;
		collection=null;
	}
	
}
