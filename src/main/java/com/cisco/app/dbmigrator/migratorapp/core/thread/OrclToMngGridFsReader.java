package com.cisco.app.dbmigrator.migratorapp.core.thread;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.core.event.SyncError;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncMarker;
import com.cisco.app.dbmigrator.migratorapp.core.job.NodeBalancer;
import com.cisco.app.dbmigrator.migratorapp.core.map.ColumnAttrMapper;
import com.cisco.app.dbmigrator.migratorapp.core.map.OracleToMongoGridFsMap;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoAttribute;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoAttributeType;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoGridData;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoLiteralFactory;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncEventDao;
import com.cisco.app.dbmigrator.migratorapp.mail.Mailer;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.MatchAble;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.queries.SelectQueryBuilder;
import com.cisco.app.dbmigrator.migratorapp.utilities.cache.DBCacheManager;
import com.cisco.app.dbmigrator.migratorapp.utilities.oracle.DbResourceUtils;

public class OrclToMngGridFsReader implements Runnable {
	private final Logger logger = Logger.getLogger(getClass());
	private final boolean saveNulls;
	private final int batchSize;
	private final SyncEventDao eventDao;
	private final ObjectId eventId;
	private final CountDownLatch latch;
	private final OracleToMongoGridFsMap map;
	private Connection connection;
	private BlockingQueue<List<MongoGridData>> dataBuffer;
	private SyncMarker marker;

	public OrclToMngGridFsReader(OracleToMongoGridFsMap map, BlockingQueue<List<MongoGridData>> dataBuffer,
			SyncMarker marker, boolean saveNulls, int batchSize, ObjectId eventId, CountDownLatch latch) {
		super();
		this.saveNulls = saveNulls;
		this.batchSize = batchSize;
		this.eventId = eventId;
		this.latch = latch;
		this.map = map;
		this.dataBuffer = dataBuffer;
		this.marker = marker;
		this.eventDao = new SyncEventDao();
	}

	@Override
	public void run() {
		logger.info("OrclToMngReader Thread Started at " + System.currentTimeMillis());
		try {
			connection = DBCacheManager.INSTANCE.getCachedOracleConnection(map.getSourceDbName(),
					map.getSourceUserName());
			processGridFsObject();
			marker.setAllRowsFetchedFromDb(true);
		} catch (Exception e) {
			logger.error("Error While processing read", e);
			marker.setFailed(true);
			SyncError error = new SyncError(e);
			error.setThreadName(Thread.currentThread().getName());
			eventDao.pushError(eventId, error);
			Mailer.sendmail(eventId, null, e, Mailer.FAILURE);
			NodeBalancer.INSTANCE.markEventAsFailed(eventId);
		} finally {
			DbResourceUtils.closeResources(null, null, connection);
			connection = null;
			dataBuffer = null;
			marker = null;
			latch.countDown();
		}
		logger.info("OrclToMngReader Thread Completed at " + System.currentTimeMillis());
	}

	private byte[] convertIsToByteArray(InputStream is) {
		if(is==null){
			return null;
		}
		byte[] bArray = null;
		try {
			bArray= IOUtils.toByteArray(is);
		} catch (Exception e) {
			logger.error("Error while converting BLOB to BYTE array", e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				logger.error("Error while closing input Stream", e);
			}
		}
		return bArray;
	}

	@SuppressWarnings("rawtypes")
	private void processGridFsObject() throws SQLException, InterruptedException {
		PreparedStatement stmt = null;
		ResultSet rows = null;
		List<MatchAble> bindValues = null;
		List<MongoGridData> dataList = null;
		try {
			int rowCount = 0;
			dataList = new ArrayList<MongoGridData>(10);
			MongoGridData data = null;
			Document document = null;
			bindValues = new ArrayList<MatchAble>();
			String query = getSelectQuery(bindValues);
			logger.info(query);
			stmt = connection.prepareStatement(query);
			if (bindValues != null) {
				for (int index = 0; index < bindValues.size(); index++) {
					stmt.setObject(index + 1, bindValues.get(index).getSqlExpressionForMatchable());
				}
			}
			stmt.setFetchSize(batchSize);
			rows = stmt.executeQuery();
			while (rows.next() && !marker.isFailed()) {
				rowCount++;
				data = new MongoGridData();
				document = new Document();
				InputStream is = rows.getBinaryStream(map.getInputStreamColumn().getColumnAlias());
				byte [] bArray = convertIsToByteArray(is);
				
				if (bArray != null) {
					data.setBinData(bArray);
				} else {
					data = null;
					document = null;
					continue;
				}
				if (map.getMetaAttributes() != null && !map.getMetaAttributes().isEmpty()) {
					for (Map.Entry<String, ColumnAttrMapper> columnAttrMap : map.getMetaAttributes().entrySet()) {
						Object mongoValue = processMongoAttribute(columnAttrMap.getValue(), rows);
						if (mongoValue != null || saveNulls) {
							document.append(columnAttrMap.getValue().getAttribute().getAttributeName(), mongoValue);
						}
					}
					if (document != null && !document.isEmpty()) {
						data.setGridMetaData(document);
					}
				}
				
				String fileName = rows.getString(map.getFileNameColumn().getColumnAlias());
				data.setFileName(fileName);
				dataList.add(data);
				if (dataList.size() >= 10) {
					logger.info("Offering documentBundle to buffer");
					dataBuffer.put(dataList);
					dataList = new ArrayList<MongoGridData>();
					marker.setRowsRead(rowCount);
					eventDao.updateMarker(eventId, marker);
				}
			}
			if (dataList.size() >= 0) {
				logger.info("Offering documentBundle to buffer");
				dataBuffer.put(dataList);
				dataList = new ArrayList<MongoGridData>();
				marker.setRowsRead(rowCount);
				eventDao.updateMarker(eventId, marker);
			}
		} finally {
			DbResourceUtils.closeResources(rows, stmt, null);
			bindValues = null;
			dataList = null;
		}
	}

	private Object processMongoAttribute(ColumnAttrMapper attrMapper, ResultSet row) throws SQLException {
		OracleColumn oracleColumn = attrMapper.getColumn();
		MongoAttribute mongoAttribute = attrMapper.getAttribute();
		Object value = null;
		try {
			if (oracleColumn != null) {
				value = MongoLiteralFactory.getMongoLiteral(row, oracleColumn.getColumnAlias(),
						mongoAttribute.getAttributeType());
			} else if (mongoAttribute.getDefaultValue() != null) {
				value = getMongoLiteralForDefaultVal(mongoAttribute.getDefaultValue(),
						mongoAttribute.getAttributeType());
			}
		} catch (SQLException e) {
			logger.error("Error occured while getting mapped value for mongoattribute : "
					+ mongoAttribute.getAttributeName(), e);
			throw new SQLException();
		}
		return value;
	}

	public static Object getMongoLiteralForDefaultVal(Object value, String attributeType) throws SQLException {
		if (value == null) {
			return null;
		}
		if (String.valueOf(MongoAttributeType.STRING).equalsIgnoreCase(attributeType)) {
			value = String.valueOf(value);
		} else if (String.valueOf(MongoAttributeType.DOUBLE).equalsIgnoreCase(attributeType)
				|| String.valueOf(MongoAttributeType.NUMBER).equalsIgnoreCase(attributeType)) {
			Double numericValue = new Double(String.valueOf(value));
			if (numericValue != 0) {
				value = numericValue.doubleValue();
			}
		} else if (String.valueOf(MongoAttributeType.INTEGER).equalsIgnoreCase(attributeType)) {
			Long numericValue = Long.valueOf(String.valueOf(value));
			if (numericValue != 0) {
				value = numericValue.longValue();
			}
		} else {
			value = String.valueOf(value);
		}
		return value;
	}

	@SuppressWarnings("rawtypes")
	private String getSelectQuery(List<MatchAble> bindValues) {
		SelectQueryBuilder queryBuilder = new SelectQueryBuilder();
		return queryBuilder.select().from(map.getStreamTable()).where(map.getFilters()).getPreparedStatement(bindValues,
				false);
	}
}