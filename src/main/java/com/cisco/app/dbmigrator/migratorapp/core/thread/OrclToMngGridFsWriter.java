package com.cisco.app.dbmigrator.migratorapp.core.thread;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.core.event.SyncError;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncMarker;
import com.cisco.app.dbmigrator.migratorapp.core.job.NodeBalancer;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoGridData;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncEventDao;
import com.cisco.app.dbmigrator.migratorapp.mail.Mailer;
import com.cisco.app.dbmigrator.migratorapp.utilities.cache.DBCacheManager;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;

public class OrclToMngGridFsWriter implements Runnable {
	private final Logger logger = Logger.getLogger(getClass());

	private SyncMarker marker;

	private final String mongoUserName;
	private final String mongoDbName;
	private final String collectionName;
	private final ObjectId eventId;
	private final SyncEventDao eventDao;
	private final CountDownLatch latch;
	private BlockingQueue<List<MongoGridData>> dataBuffer;

	public OrclToMngGridFsWriter(SyncMarker marker, String mongoUserName, String mongoDbName, String collectionName,
			ObjectId eventId, CountDownLatch latch, BlockingQueue<List<MongoGridData>> dataBuffer) {
		super();
		this.marker = marker;
		this.mongoUserName = mongoUserName;
		this.mongoDbName = mongoDbName;
		this.collectionName = collectionName;
		this.eventId = eventId;
		this.eventDao = new SyncEventDao();
		this.latch = latch;
		this.dataBuffer = dataBuffer;
	}

	@Override
	public void run() {
		GridFSBucket gridFSBucket = GridFSBuckets.create(
				DBCacheManager.INSTANCE.getCachedMongoPool(mongoDbName, mongoUserName).getDatabase(mongoDbName),
				collectionName);
		GridFSUploadOptions options = new GridFSUploadOptions().chunkSizeBytes(2048);
		List<MongoGridData> dataList = null;
		try {
			while (true) {
				if ((marker.getRowsDumped() == marker.getTotalRows()) || marker.isFailed()) {
					break;
				}
				dataList = dataBuffer.take();
				if (dataList == null || dataList.isEmpty()) {
					continue;
				}
				for (MongoGridData data : dataList) {
					InputStream is = new ByteArrayInputStream(data.getBinData());
					try {
						if (data.getGridMetaData() != null) {
							options.metadata(data.getGridMetaData());
						}
						gridFSBucket.uploadFromStream(data.getFileName(), is, options);
						marker.setRowsDumped(marker.getRowsDumped() + 1);
						eventDao.updateMarker(eventId, marker);
						logger.info("Inserted documentbatch successfully");
					} catch (Exception e) {
						logger.error("Error while getting processed Document", e);
						Mailer.sendmail(eventId, String.valueOf(data), e, Mailer.FAILURE);
					}finally{
						is.close();
					}
				}
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
			Mailer.sendmail(eventId, String.valueOf(dataList), e, Mailer.FAILURE);
		} finally {
			marker = null;
			dataBuffer = null;
			latch.countDown();
		}
	}
}