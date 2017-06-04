package com.cisco.app.dbmigrator.migratorapp.core.thread;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.bson.BsonTimestamp;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.core.event.SyncMarker;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncEventDao;
import com.cisco.app.dbmigrator.migratorapp.mail.Mailer;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.QueryConstants;
import com.cisco.app.dbmigrator.migratorapp.utilities.cache.DBCacheManager;
import com.mongodb.Block;
import com.mongodb.CursorType;
import com.mongodb.MongoClient;
import com.mongodb.ReadPreference;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

public class MngOpLogReader implements Runnable {
	private final String ns;
	private final String mongoDbName;
	private final String mongoUserName;
	private BlockingQueue<Document> dataBuffer;
	private static final String oplogRs = "oplog.rs";
	private static final String localDb = "local";
	private static final String NS = "ns";
	private static final String TS = "ts";
	private BsonTimestamp lastReadTime;
	private SyncMarker marker;
	private final CountDownLatch latch;
	private final SyncEventDao eventDao;
	private final ObjectId eventId;
	private int retryCount =0;
	private long waitTime =1;
	public MngOpLogReader(String collectionName, String mongoDbName, String mongoUserName,
			BlockingQueue<Document> dataBuffer, BsonTimestamp lastReadTime, SyncMarker marker, CountDownLatch latch , ObjectId eventId) {
		super();
		this.ns = mongoDbName + QueryConstants.DOT + collectionName;
		this.mongoDbName = mongoDbName;
		this.mongoUserName = mongoUserName;
		this.dataBuffer = dataBuffer;
		this.lastReadTime = lastReadTime;
		this.marker = marker;
		this.latch=latch;
		this.eventId=eventId;
		this.eventDao= new SyncEventDao();
	}
	
	private FindIterable<Document> getCursor(){
		MongoClient client = DBCacheManager.INSTANCE.getCachedMongoPool(mongoDbName, mongoUserName);
		//MongoClient client = DBCacheManager.INSTANCE.getCachedMongoPool(mongoDbName, "ccwOplRO");
		client.setReadPreference(ReadPreference.secondary());
		MongoCollection<Document> collection =client.getDatabase(localDb).getCollection(oplogRs);
		FindIterable<Document> it = collection.find(Filters.and(Filters.eq(NS, ns),Filters.gt(TS, lastReadTime)))
				.cursorType(CursorType.TailableAwait).noCursorTimeout(true).maxAwaitTime(30, TimeUnit.MINUTES);
		return it;
	}

	@Override
	public void run() {
		while(retryCount<=5){
			try{
				FindIterable<Document> it =getCursor();
				retryCount=0;
				waitTime=30000;
				it.forEach(new Block<Document>() {
					@Override
					public void apply(Document t) {
						try {
							if(marker.isFailed()){							
								return;
							}
							dataBuffer.put(t);						
							lastReadTime = (BsonTimestamp) t.get(TS);
							eventDao.updateLastReadTime(eventId, lastReadTime);
						} catch (InterruptedException e) {
							e.printStackTrace();
							Mailer.sendmail(eventId, null, e, Mailer.FAILURE);
						}
					}
				});
			}catch(Exception e){
				e.printStackTrace();
				Mailer.sendmail(eventId, null, e, Mailer.FAILURE);
				retryCount++;
				try {
					waitTime*=retryCount;
					Thread.sleep(waitTime);					
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}	
		}		
		dataBuffer=null;
		marker=null;
		latch.countDown();
	}
}
