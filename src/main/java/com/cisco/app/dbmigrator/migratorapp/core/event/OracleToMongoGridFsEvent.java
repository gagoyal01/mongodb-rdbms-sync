package com.cisco.app.dbmigrator.migratorapp.core.event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.core.job.NodeBalancer;
import com.cisco.app.dbmigrator.migratorapp.core.job.SyncStatus;
import com.cisco.app.dbmigrator.migratorapp.core.map.OracleToMongoGridFsMap;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoGridData;
import com.cisco.app.dbmigrator.migratorapp.core.thread.OrclToMngGridFsReader;
import com.cisco.app.dbmigrator.migratorapp.core.thread.OrclToMngGridFsWriter;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncEventDao;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncMapDao;
import com.cisco.app.dbmigrator.migratorapp.mail.Mailer;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.MatchAble;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.queries.SelectQueryBuilder;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.Operations;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.SQLFilters;
import com.cisco.app.dbmigrator.migratorapp.utilities.cache.DBCacheManager;
import com.cisco.app.dbmigrator.migratorapp.utilities.oracle.DbResourceUtils;

public class OracleToMongoGridFsEvent extends SyncEvent<List<MongoGridData>> implements Cloneable{
	private final Logger logger = Logger.getLogger(getClass());
	private String collectionName;
	private boolean saveNulls;
	private SQLFilters rangeFilter;
	public SQLFilters getRangeFilter() {
		return rangeFilter;
	}

	public void setRangeFilter(SQLFilters rangeFilter) {
		this.rangeFilter = rangeFilter;
	}

	private final CountDownLatch latch = new CountDownLatch(2);

	public String getCollectionName() {
		return collectionName;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	public boolean isSaveNulls() {
		return saveNulls;
	}

	public void setSaveNulls(boolean saveNulls) {
		this.saveNulls = saveNulls;
	}

	/*public SQLFilters getRangeFilter() {
		return rangeFilter;
	}

	public void setRangeFilter(SQLFilters rangeFilter) {
		this.rangeFilter = rangeFilter;
	}*/

	@Override
	public void run() {
		try {
			if (marker == null) {
				marker = new SyncMarker();
			}
			if (parentEventId == null) {
				parentEventId = eventId;
			}
			marker.setStartTime(new Date());
			dataBuffer = new LinkedBlockingQueue<List<MongoGridData>>(batchSize);
			eventDao = new SyncEventDao();

			logger.info("OracleToMongoBasicEvent Thread Started at " + System.currentTimeMillis());

			OracleToMongoGridFsMap map = (OracleToMongoGridFsMap) new SyncMapDao().getMapping(mapId);
			
			if (rangeFilter != null) {
				if (map.getFilters() == null) {
					map.setFilters(rangeFilter);
					map.getFilters().AND(Operations.isNotNull(map.getInputStreamColumn()));
				} else {
					map.getFilters().AND(Operations.isNotNull(map.getInputStreamColumn())).AND(rangeFilter);
				}
			}			
			getStats(map);
			if (marker.getTotalRows() != 0) {
				eventDao.updateMarker(eventId, marker);
				Thread reader;
				reader = new Thread(
						new OrclToMngGridFsReader(map, dataBuffer, marker, saveNulls, batchSize, eventId, latch));
				reader.setName(eventName + "-Reader");
				reader.start();

				Thread writer;
				writer = new Thread(new OrclToMngGridFsWriter(marker, map.getTargetUserName(), map.getTargetDbName(),
						collectionName, eventId, latch, dataBuffer));

				writer.setName(eventName + "-Writer");
				writer.start();
			} else {
				marker.setEndTime(new Date());
				eventDao.updateMarker(eventId, marker);
				NodeBalancer.INSTANCE.markEventAsCompleted(eventId);
			}
			latch.await();
		} catch (SyncError e) {
			e.printStackTrace();
			e.setThreadName(eventName);
			eventDao.pushError(eventId, e);
			NodeBalancer.INSTANCE.markEventAsFailed(eventId);
			Mailer.sendmail(this, null, e, Mailer.FAILURE);
		} catch (InterruptedException e) {
			e.printStackTrace();
			SyncError error = new SyncError(e);
			error.setThreadName(eventName);
			eventDao.pushError(eventId, error);
			NodeBalancer.INSTANCE.markEventAsFailed(eventId);
			Mailer.sendmail(this, null, e, Mailer.FAILURE);
		} finally {
			dataBuffer = null;
			marker = null;
			logger.info("OracleToMongoBasicEvent Thread Completed at " + System.currentTimeMillis());
		}
	}

	@SuppressWarnings("rawtypes")
	private void getStats(OracleToMongoGridFsMap map) throws SyncError {
		SelectQueryBuilder queryBuilder = new SelectQueryBuilder();
		List<MatchAble> bindvalues = new ArrayList<MatchAble>();
		PreparedStatement stmt = null;
		ResultSet rs = null;
	
		String countQuery = queryBuilder.select().from(map.getStreamTable()).where(map.getFilters())
				.getCountQuery(bindvalues);
		Connection connection = null;
		try {
			connection = DBCacheManager.INSTANCE.getCachedOracleConnection(map.getSourceDbName(),
					map.getSourceUserName());
			stmt = connection.prepareStatement(countQuery);
			if (bindvalues != null) {
				for (int index = 0; index < bindvalues.size(); index++) {
					stmt.setObject(index + 1, bindvalues.get(index).getSqlExpressionForMatchable());
				}
			}
			rs = stmt.executeQuery();
			logger.debug("Query Executed to get RowCount");
			rs.next();
			int totalRows = rs.getInt(1);
			logger.info("Rowcount Fecthed : " + totalRows);
			marker.setTotalRows(totalRows);
		} catch (SQLException e) {
			logger.error("Error while getting total count of rows to be processed", e);
			throw new SyncError(e);
		} finally {
			DbResourceUtils.closeResources(rs, stmt, connection);
		}
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		OracleToMongoGridFsEvent clonedEvent = (OracleToMongoGridFsEvent) super.clone();
		clonedEvent.setEventId(new ObjectId());
		clonedEvent.setParentEventId(this.getEventId());
		clonedEvent.setCreatedBy("SYSTEM");
		clonedEvent.setCreatedOn(new Date());
		clonedEvent.setStatus(SyncStatus.PENDING);
		clonedEvent.setDataBuffer(null);
		clonedEvent.setMarker(null);
		clonedEvent.setRangeFilter(null);
		return clonedEvent;
	}
}