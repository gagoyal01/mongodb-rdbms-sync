package com.cisco.app.dbmigrator.migratorapp.core.event;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.bson.BsonTimestamp;
import org.bson.Document;

import com.cisco.app.dbmigrator.migratorapp.core.job.NodeBalancer;
import com.cisco.app.dbmigrator.migratorapp.core.job.SyncStatus;
import com.cisco.app.dbmigrator.migratorapp.core.map.MongoToOracleMap;
import com.cisco.app.dbmigrator.migratorapp.core.thread.MngOpLogReader;
import com.cisco.app.dbmigrator.migratorapp.core.thread.MngToOrclSyncWriter;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncEventDao;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncMapDao;
import com.cisco.app.dbmigrator.migratorapp.mail.Mailer;

/**
 * Runnable class to process MongoDB to OracleDB replication (data synchronization) events.
 * 
 * @author pnilayam
 *
 */
public class MongoToOracleSyncEvent extends SyncEvent<Document> {
	private final Logger logger = Logger.getLogger(getClass());
	private static final String OP_READER = "_Op_Reader";
	private static final String SYNC_WRITER = "_Sync_Writer";
	private volatile BsonTimestamp lastReadTime;
	private boolean isRestrictedSyncEnabled;
	private String collectionName;
	private final CountDownLatch latch;

	public MongoToOracleSyncEvent() {
		super();
		this.latch = new CountDownLatch(2);
	}

	/**
	 * @return the isRestrictedSyncEnabled
	 */
	public boolean isRestrictedSyncEnabled() {
		return isRestrictedSyncEnabled;
	}

	/**
	 * @param isRestrictedSyncEnabled the isRestrictedSyncEnabled to set
	 */
	public void setRestrictedSyncEnabled(boolean isRestrictedSyncEnabled) {
		this.isRestrictedSyncEnabled = isRestrictedSyncEnabled;
	}

	/**
	 * @return the collectionName
	 */
	public String getCollectionName() {
		return collectionName;
	}

	/**
	 * @param collectionName the collectionName to set
	 */
	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	public BsonTimestamp getLastReadTime() {
		return lastReadTime;
	}

	public void setLastReadTime(BsonTimestamp lastReadTime) {
		this.lastReadTime = lastReadTime;
	}

	@Override
	public void run() {
		Timer timer=null;
		try {
			if (marker == null) {
				marker = new SyncMarker();
				marker.setStartTime(new Date());
			}
			mappingDao = new SyncMapDao();
			dataBuffer = new LinkedBlockingQueue<Document>(10);
			eventDao = new SyncEventDao();

			MongoToOracleMap map = (MongoToOracleMap) mappingDao.getMapping(mapId);

			MngOpLogReader reader = new MngOpLogReader(map.getCollectionName(), map.getSourceDbName(),
					map.getSourceUserName(), dataBuffer, lastReadTime, marker, latch, eventId);
			final Thread readerThread = new Thread(reader);
			readerThread.setName(map.getCollectionName() + OP_READER);
			readerThread.start();

			MngToOrclSyncWriter writer = new MngToOrclSyncWriter(dataBuffer, map, marker, latch , isRestrictedSyncEnabled,eventId);
			final Thread writerThread = new Thread(writer);
			writerThread.setName(map.getCollectionName() + SYNC_WRITER);
			writerThread.start();
			timer = new Timer(true);
			timer.scheduleAtFixedRate(new TimerTask() {				
				@Override
				public void run() {
					if(marker.isFailed()){
						latch.countDown();
						latch.countDown();
						readerThread.interrupt();
						writerThread.interrupt();
					}
				}
			}, new Date(), 5000);
			latch.await();
			Mailer.sendmail(this, null, null, Mailer.CANCELLED);
		} catch (InterruptedException e) {
			e.printStackTrace();
			Mailer.sendmail(this, null, e, Mailer.FAILURE);
		} catch(Exception e){
			e.printStackTrace();
			Mailer.sendmail(this, null, e, Mailer.FAILURE);
		} finally{
			dataBuffer=null;
			if(timer!=null){
				timer.cancel();
			}			
			logger.info("MongoToOracleSyncEvent terminated for eventName :" +eventName);
			eventDao.updateEventStatus(eventId, SyncStatus.FAILED);
			NodeBalancer.INSTANCE.markEventAsFailed(eventId);
		}
	}
}
