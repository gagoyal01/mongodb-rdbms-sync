package com.cisco.app.dbmigrator.migratorapp.core.thread;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.constants.SyncConstants;
import com.cisco.app.dbmigrator.migratorapp.core.event.OracleToMongoSyncEvent.O2MSyncPollInfo;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncError;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncMarker;
import com.cisco.app.dbmigrator.migratorapp.core.map.OracleToMongoMap;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoAttribute;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoAttributeType;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoEntity;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoLiteralFactory;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoObject;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.O2MSyncEventLogCodec;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncAttrs;
import com.cisco.app.dbmigrator.migratorapp.logging.connection.ApplicationCollections;
import com.cisco.app.dbmigrator.migratorapp.logging.connection.MongoConnection;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncEventDao;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.O2MSyncEventLog;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.O2MSyncEventLog.O2MSyncEventInfo;
import com.cisco.app.dbmigrator.migratorapp.mail.Mailer;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.Literal;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.MatchAble;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.QueryConstants;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.SqlColumnType;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.SqlLiteralFactory;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.queries.SelectQueryBuilder;
import com.cisco.app.dbmigrator.migratorapp.utilities.cache.DBCacheManager;
import com.cisco.app.dbmigrator.migratorapp.utilities.oracle.DbResourceUtils;
import com.mongodb.Block;
import com.mongodb.CursorType;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

public class OrclToMngSyncReader implements Runnable {
	private final Logger logger = Logger.getLogger(getClass());
	private final ObjectId eventId;
	private final CountDownLatch latch;
	private final OracleToMongoMap map;
	private final int batchSize = SyncConstants.DEFAULT_BATCH_SIZE;
	private final boolean saveNulls;
	private final boolean pollBased;
	private final SyncEventDao eventDao;
	private final ScheduledThreadPoolExecutor executor;
	private ScheduledFuture<?> task;
	private O2MSyncPollInfo pollInfo;
	private int retryCount = 0;
	private Connection connection;
	private BlockingQueue<List<Document>> dataBuffer;
	private Map<String, SelectQueryHolder> queryMap;
	private SyncMarker marker;
	private long waitTime = 1;
	private CountDownLatch pollLatch;

	private MongoCollection<O2MSyncEventLog> logCollection;

	public OrclToMngSyncReader(OracleToMongoMap orclToMongoMap, boolean saveNulls,
			BlockingQueue<List<Document>> dataBuffer, SyncMarker marker, ObjectId eventId, CountDownLatch latch,
			boolean pollBased, O2MSyncPollInfo pollInfo) {
		super();
		this.dataBuffer = dataBuffer;
		this.marker = marker;
		this.eventId = eventId;
		this.latch = latch;
		this.map = orclToMongoMap;
		this.saveNulls = saveNulls;
		this.pollInfo = pollInfo;
		this.pollBased = pollBased;
		this.eventDao = new SyncEventDao();
		executor = new ScheduledThreadPoolExecutor(1);
	}

	private void releaseReources() {
		for (SelectQueryHolder holder : queryMap.values()) {
			DbResourceUtils.closeResources(null, holder.getPstmt(), null);
		}
		DbResourceUtils.closeResources(null, null, connection);
		queryMap = null;
		connection = null;
		dataBuffer = null;
		logCollection = null;
		marker = null;
		latch.countDown();
	}

	private FindIterable<O2MSyncEventLog> getCursor() throws InterruptedException {
		Thread.sleep(waitTime);
		waitTime *= retryCount;
		logCollection = MongoConnection.INSTANCE.getMongoDataBase()
				.getCollection(String.valueOf(ApplicationCollections.O2MSyncEventLog), O2MSyncEventLog.class);
		FindIterable<O2MSyncEventLog> it = logCollection
				.find(Filters.and(Filters.eq(O2MSyncEventLogCodec.EVENT_ID, String.valueOf(eventId)),
						Filters.eq(O2MSyncEventLogCodec.STATUS, O2MSyncEventLogCodec.PENDING)))
				.cursorType(CursorType.TailableAwait).noCursorTimeout(true);
		return it;
	}

	private void refreshConnection() throws SQLException, InterruptedException, SyncError {
		Thread.sleep(waitTime);
		waitTime *= retryCount;
		for (SelectQueryHolder holder : queryMap.values()) {
			DbResourceUtils.closeResources(null, holder.getPstmt(), null);
		}
		DbResourceUtils.closeResources(null, null, connection);
		queryMap = new LinkedHashMap<String, SelectQueryHolder>();
		connection = DBCacheManager.INSTANCE.getCachedOracleConnection(map.getSourceDbName(), map.getSourceUserName());
	}

	@Override
	public void run() {
		queryMap = new HashMap<String, SelectQueryHolder>();
		while (retryCount < 5) {
			try {
				if (marker.isFailed()) {
					releaseReources();
					break;
				}
				refreshConnection();
				retryCount = 0;
				waitTime = 30000;
				if (pollBased) {
					pollLatch = new CountDownLatch(1);
					processPollBasedSync();
					pollLatch.await();
				} else {
					processEventLogFlow();
				}
			} catch (Exception e) {
				logger.error("Event Failed with error", e);
				Mailer.sendmail(eventId, null, e, Mailer.FAILURE);
				retryCount++;
			}
		}
	}

	private void processPollBasedSync() {
		task=executor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					if (marker.isFailed()) {
						releaseReources();
						pollLatch.countDown();
						return;
					}
					logger.info("Polling for updated documents");
					processMongoObject(map.getMapObject(), true, null, map.getMapObject().getCollectionName(), null);
					pollInfo.getLastReadTime().setTime(System.currentTimeMillis());
					eventDao.updateLastReadTime(eventId, pollInfo.getLastReadTime());
					logger.info("Updated lastReadTime as "+pollInfo.getLastReadTime());
				} catch (SyncError e) {
					pollLatch.countDown();
					Mailer.sendmail(eventId, null, e, Mailer.FAILURE);	
					task.cancel(true);
					return;
				}
			}
		}, 1, pollInfo.getInterval(), TimeUnit.valueOf(pollInfo.getTimeUnit()));
	}

	private void processEventLogFlow() throws InterruptedException {
		FindIterable<O2MSyncEventLog> it = getCursor();
		retryCount = 0;
		it.forEach(new Block<O2MSyncEventLog>() {
			@Override
			public void apply(O2MSyncEventLog eventLog) {
				try {
					if (marker.isFailed()) {
						releaseReources();
						return;
					}
					logCollection.findOneAndUpdate(Filters.eq(SyncAttrs.ID, eventLog.getLogId()),
							Updates.set(O2MSyncEventLogCodec.STATUS, O2MSyncEventLogCodec.RUNNING));
					logger.info("Processing filter : "+eventLog.getEventFilters());
					if (eventLog.getOperation().equals(SyncConstants.DELETE)) {
						processDeletedDoc(eventLog);
					}else{
						processMongoObject(map.getMapObject(), true, null, map.getMapObject().getCollectionName(),
								eventLog);	
					}					
					logCollection.findOneAndUpdate(Filters.eq(SyncAttrs.ID, eventLog.getLogId()),
							Updates.set(O2MSyncEventLogCodec.STATUS, O2MSyncEventLogCodec.COMPLETE));
					logger.info("Processed filter : "+eventLog.getEventFilters());
				} catch (SyncError e) {
					logger.error("Error in O2M replication", e);
					Mailer.sendmail(eventId, null, e, Mailer.FAILURE);
				}
			}
		});
	}

	private void setEventLogSyncFilterLiteral(O2MSyncEventLog eventLog, OracleColumn filterColumn) {
		for (O2MSyncEventInfo eventInfo : eventLog.getEventFilters()) {
			if (eventInfo.getTableName().equalsIgnoreCase(filterColumn.getTableAlias())
					&& eventInfo.getColumnName().equalsIgnoreCase(filterColumn.getColumnName())) {
				@SuppressWarnings("rawtypes")
				Literal columnLiteralValue = SqlLiteralFactory.getLiteral(eventInfo.getColumnValue(),
						filterColumn.getColumnType());
				filterColumn.setLiteralValueForColumn(columnLiteralValue);
				break;
			}
		}
	}

	private void setPollingSyncFilterLiteral(OracleColumn filterColumn) {
		System.out.println("Setting date in filter "+pollInfo.getLastReadTime());
		filterColumn
				.setLiteralValueForColumn(SqlLiteralFactory.getLiteral(pollInfo.getLastReadTime(), SqlColumnType.DATE));
	}

	private void processDeletedDoc(O2MSyncEventLog eventLog) throws SyncError {
		Document deletedData = null;
		try {
			if (eventLog.getEventFilters() != null) {
				deletedData = new Document(SyncConstants.OPERATION,SyncConstants.DELETE);
				MongoObject rootObj = map.getMapObject();
				for (O2MSyncEventInfo info : eventLog.getEventFilters()) {
					if (rootObj.getIdentifierList() != null) {
						for (MongoEntity entity : rootObj.getIdentifierList()) {
							MongoAttribute attribute = (MongoAttribute) entity;
							if (attribute.getMappedOracleColumn().getColumnName().equals(info.getColumnName())) {
								deletedData.append(SyncAttrs.ID, info.getColumnValue());
								break;
							}
						}
						for (MongoEntity entity : rootObj.getAttributes()) {
							if (entity instanceof MongoAttribute) {
								MongoAttribute attribute = (MongoAttribute) entity;
								if (attribute.getMappedOracleColumn().getColumnName().equals(info.getColumnName())) {
									deletedData.append(attribute.getAttributeName(), info.getColumnValue());
								}
							}
						}
					}
				}
				if (deletedData != null) {
					dataBuffer.put(Arrays.asList(deletedData));
				}
			}
		} catch (Exception e) {
			Mailer.sendmail(eventId, null, e, Mailer.FAILURE);
			throw new SyncError("Delete operation failed" ,e);
		}
	}

	@SuppressWarnings("rawtypes")
	private List<Document> processMongoObject(MongoObject mongoObject, boolean isParent, ResultSet parentRow,
			String parentObjectName, O2MSyncEventLog eventLog) throws SyncError {
		int rowCount = 0;
		List<Document> documentBundle = null;// new
												// ArrayList<Document>(batchSize);

		PreparedStatement stmt = null;
		ResultSet rows = null;
		SelectQueryHolder queryHolder = null;
		try {
			if (mongoObject.getSourceTables() != null && !mongoObject.getSourceTables().isEmpty()) {
				if (!isParent) {
					parentObjectName = parentObjectName + QueryConstants.DOT + mongoObject.getCollectionName();
				}
				queryHolder = queryMap.get(parentObjectName);
				if (queryHolder == null || queryHolder.getPstmt() == null) {
					queryHolder = new SelectQueryHolder();
					queryHolder.buildHolder(mongoObject, isParent);
					queryMap.put(parentObjectName, queryHolder);
				}

				List<MatchAble> bindvalues = queryHolder.getBindValues();
				stmt = queryHolder.getPstmt();
				if (bindvalues != null) {
					for (int index = 0; index < bindvalues.size(); index++) {
						if (bindvalues.get(index) instanceof OracleColumn) {
							OracleColumn column = (OracleColumn) bindvalues.get(index);
							if (isParent) {
								if (pollBased) {
									setPollingSyncFilterLiteral(column);
								} else {
									setEventLogSyncFilterLiteral(eventLog, column);
								}
							} else {
								column.extractColumnValueFromParentRow(parentRow);
							}
						}
						stmt.setObject(index + 1, bindvalues.get(index).getSqlExpressionForMatchable());
					}
				}
				stmt.setFetchSize(batchSize);
				rows = stmt.executeQuery();
				documentBundle = processResultSet(rows, mongoObject, parentRow, isParent,parentObjectName);

			} else {
				documentBundle = processChildNodes(mongoObject, parentRow, parentObjectName);
			}

			if (documentBundle != null && documentBundle.size() > 0 && isParent) {
				dataBuffer.put(documentBundle);
				marker.setRowsRead(rowCount);
				logger.info("Offering documentBundle to buffer");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SyncError("SQL Error While Processing Document ", e);
		} catch (InterruptedException e) {
			throw new SyncError("Error while putting document", e);
		} catch (Exception e) {
			throw new SyncError("Error while processing document", e);
		} finally {
			try {
				if (queryHolder != null) {
					queryHolder.getPstmt().clearParameters();
				}
				DbResourceUtils.closeResources(rows, null, null);
			} catch (SQLException e) {
				DbResourceUtils.closeResources(null, queryHolder.getPstmt(), connection);
				queryHolder.setPstmt(null);
				logger.warn("Error in closing ResultSet", e);
			}
		}
		return documentBundle;
	}

	private List<Document> processChildNodes(MongoObject mongoObject, ResultSet parentRow, String parentObjectName)
			throws SQLException, SyncError {
		List<Document> documentBundle = null;
		Object value = null;
		Document document = new Document();
		String attributeName = null;
		for (MongoEntity attribute : mongoObject.getAttributes()) {
			value = null;
			if (attribute instanceof MongoAttribute) {
				MongoAttribute mongoAttribute = (MongoAttribute) attribute;
				attributeName = mongoAttribute.getAttributeName();
				if (mongoAttribute.getMappedOracleColumn().isParentColumn()) {
					value = processMongoAttribute(mongoAttribute, parentRow);
				}
			} else if (attribute instanceof MongoObject) {
				MongoObject mongoNestedObject = (MongoObject) attribute;
				attributeName = mongoNestedObject.getCollectionName();
				List<Document> nestedDocList = processMongoObject(mongoNestedObject, false, parentRow, parentObjectName,
						null);
				MongoAttributeType collectionType = MongoAttributeType.valueOf(mongoNestedObject.getCollectionType());
				if (MongoAttributeType.AUTO.equals(collectionType)) {
					if (nestedDocList != null && nestedDocList.size() > 1) {
						value = nestedDocList;
					} else if (nestedDocList != null && nestedDocList.size() == 1) {
						value = nestedDocList.get(0);
					}
				} else if (MongoAttributeType.COLLECTION.equals(collectionType)) {
					if (nestedDocList != null && nestedDocList.size() >= 1) {
						value = nestedDocList.get(0);
					}
				} else {
					if (nestedDocList != null && nestedDocList.size() >= 1) {
						value = nestedDocList;
					}
				}
			}
			if (value != null || saveNulls) {
				/*document.append(SyncConstants.SRC, SyncConstants.ORCL);
				document.append(SyncConstants.SYNC_TIME, System.currentTimeMillis());*/
				document.append(attributeName, value);
			}
		}
		if (!document.isEmpty()) {
			documentBundle = new ArrayList<Document>(1);
			documentBundle.add(document);
		}
		return documentBundle;
	}

	private List<Document> processResultSet(ResultSet rows, MongoObject mongoObject, ResultSet parentRow,
			boolean isParent,String parentObjectName) throws InterruptedException, SQLException, SyncError {
		Document document = null;
		Object value = null;
		String attributeName = null;
		List<Document> documentBundle = new ArrayList<Document>(batchSize);
		boolean isIdentifierPresent = false;
		boolean isComplexIdentifier = false;
		if (mongoObject.getIdentifierList() != null && !mongoObject.getIdentifierList().isEmpty()) {
			isIdentifierPresent = true;
			if (mongoObject.getIdentifierList().size() > 1) {
				isComplexIdentifier = true;
			}
		}
		while (rows.next() && !marker.isFailed()) {
			// rowCount++;
			document = new Document();
			for (MongoEntity attribute : mongoObject.getAttributes()) {
				if (attribute instanceof MongoAttribute) {
					MongoAttribute mongoAttribute = (MongoAttribute) attribute;
					if (null!= mongoAttribute.getMappedOracleColumn() && mongoAttribute.getMappedOracleColumn().isParentColumn()) {
						value = processMongoAttribute(mongoAttribute, parentRow);
					} else {
						value = processMongoAttribute(mongoAttribute, rows);
					}
					attributeName = mongoAttribute.getAttributeName();
				} else if (attribute instanceof MongoObject) {
					MongoObject mongoNestedObject = (MongoObject) attribute;
					MongoAttributeType collectionType = MongoAttributeType
							.valueOf(mongoNestedObject.getCollectionType());
					List<Document> nestedDocList = processMongoObject(mongoNestedObject, false, rows,
							/*mongoObject.getCollectionName()*/parentObjectName, null);
					value = null;
					if (MongoAttributeType.AUTO.equals(collectionType)) {
						if (nestedDocList != null && nestedDocList.size() > 1) {
							value = nestedDocList;
						} else if (nestedDocList != null && nestedDocList.size() == 1) {
							value = nestedDocList.get(0);
						}
					} else if (MongoAttributeType.COLLECTION.equals(collectionType)) {
						if (nestedDocList != null && nestedDocList.size() >= 1) {
							value = nestedDocList.get(0);
						}
					} else {
						if (nestedDocList != null && nestedDocList.size() >= 1) {
							value = nestedDocList;
						}
					}
					attributeName = mongoNestedObject.getCollectionName();
				}

				if (value != null || saveNulls) {
					document.append(attributeName, value);
				}
			}
			// Can be modularized
			if (isIdentifierPresent) {
				Object _id = null;
				if (isComplexIdentifier) {
					Object nestedValue = null;
					Document identifierDocument = new Document();
					for (MongoEntity identifier : mongoObject.getIdentifierList()) {
						MongoAttribute mongoAttribute = (MongoAttribute) identifier;
						nestedValue = processMongoAttribute(mongoAttribute, rows);
						attributeName = mongoAttribute.getAttributeName();
						identifierDocument.append(attributeName, nestedValue);
					}
					_id = identifierDocument;
				} else {
					MongoAttribute mongoAttribute = (MongoAttribute) mongoObject.getIdentifierList().get(0);
					_id = processMongoAttribute(mongoAttribute, rows);
				}
				document.append(SyncAttrs.ID, _id);
			}
			if (!document.isEmpty()) {
				if(isParent){
					document.append(SyncConstants.SRC, SyncConstants.ORCL);
					document.append(SyncConstants.SYNC_TIME, System.currentTimeMillis());	
				}				
				documentBundle.add(document);
			}
			if (documentBundle.size() == batchSize && isParent) {
				logger.info("Offering documentBundle to buffer");
				dataBuffer.put(documentBundle);
				documentBundle = new ArrayList<Document>(batchSize);
			}
		}
		return documentBundle;
	}

	private Object processMongoAttribute(MongoAttribute mongoAttribute, ResultSet row) throws SQLException {
		OracleColumn oracleColumn = mongoAttribute.getMappedOracleColumn();
		Object value = null;
		try {
			if (mongoAttribute.getMappedOracleColumn() != null) {
				value = MongoLiteralFactory.getMongoLiteral(row, oracleColumn.getColumnAlias(),
						mongoAttribute.getAttributeType());
			} else if (mongoAttribute.getDefaultValue() != null) {
				// TODO : do proper transformation of value according to type
				// using variant of mongoliteral
				value = mongoAttribute.getDefaultValue();
			}
		} catch (SQLException e) {
			logger.error("Error occured while getting mapped value for mongoattribute : "
					+ mongoAttribute.getAttributeName(), e);
			throw new SQLException();
		}
		return value;
	}

	@SuppressWarnings("rawtypes")
	private final class SelectQueryHolder {
		private PreparedStatement pstmt;

		private List<MatchAble> bindValues;

		public PreparedStatement getPstmt() {
			return pstmt;
		}

		/**
		 * @param pstmt
		 *            the pstmt to set
		 */
		public void setPstmt(PreparedStatement pstmt) {
			this.pstmt = pstmt;
		}

		public List<MatchAble> getBindValues() {
			return bindValues;
		}

		public void buildHolder(MongoObject object, boolean isParent) throws SQLException {
			if (object.getSourceTables() != null && !object.getSourceTables().isEmpty()) {
				SelectQueryBuilder queryBuilder = new SelectQueryBuilder();
				bindValues = new ArrayList<MatchAble>();
				String selectQuery = queryBuilder.select().from(object.getSourceTables().get(0))
						.where(object.getFilters()).getPreparedStatement(bindValues, false);// Temp
				logger.info(object.getCollectionName() + selectQuery);
				pstmt = connection.prepareStatement(selectQuery, ResultSet.TYPE_FORWARD_ONLY,
						ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
			}
		}
	}
}