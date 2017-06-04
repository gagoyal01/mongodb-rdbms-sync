package com.cisco.app.dbmigrator.migratorapp.core.thread;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.constants.SyncConstants;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncError;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncMarker;
import com.cisco.app.dbmigrator.migratorapp.core.job.NodeBalancer;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoAttribute;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoAttributeType;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoEntity;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoLiteralFactory;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoObject;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncAttrs;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncEventDao;
import com.cisco.app.dbmigrator.migratorapp.mail.Mailer;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.MatchAble;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.QueryConstants;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.queries.SelectQueryBuilder;
import com.cisco.app.dbmigrator.migratorapp.utilities.cache.DBCacheManager;
import com.cisco.app.dbmigrator.migratorapp.utilities.oracle.DbResourceUtils;

/**
 * @author pnilayam
 *
 */
public class OrclToMngReader implements Runnable {

	private static final Logger logger = Logger.getLogger(OrclToMngReader.class);

	private final boolean saveNulls;
	private final String sourceDbName;
	private final String sourceSchemaName;
	private final int batchSize;
	private final SyncEventDao eventDao;
	private final ObjectId eventId;
	private final CountDownLatch latch;
	private final MongoObject mongoObject;
	private Connection connection;
	private BlockingQueue<List<Document>> dataBuffer;
	private SyncMarker marker;
	private Map<String, SelectQueryHolder> queryMap;

	public OrclToMngReader(MongoObject mongoObject, int batchSize, BlockingQueue<List<Document>> dataBuffer,
			SyncMarker marker, boolean saveNulls, String sourceDbName, String sourceSchemaName, ObjectId eventId,
			CountDownLatch latch) {
		super();
		this.mongoObject = mongoObject;
		this.dataBuffer = dataBuffer;
		this.batchSize = 10;
		this.marker = marker;
		this.saveNulls = saveNulls;
		this.sourceDbName = sourceDbName;
		this.sourceSchemaName = sourceSchemaName;
		this.eventDao = new SyncEventDao();
		this.eventId = eventId;
		this.latch = latch;
	}

	@Override
	public void run() {
		logger.info("OrclToMngReader Thread Started at " + System.currentTimeMillis());
		try {
			connection = DBCacheManager.INSTANCE.getCachedOracleConnection(sourceDbName, sourceSchemaName);
			queryMap = new HashMap<String, SelectQueryHolder>();
			processMongoObject(mongoObject, true, null, mongoObject.getCollectionName());
			marker.setAllRowsFetchedFromDb(true);
		} catch (SQLException e) {
			logger.error("Error While Getting Connection", e);
			marker.setFailed(true);
			SyncError error = new SyncError(e);
			error.setThreadName(Thread.currentThread().getName());
			eventDao.pushError(eventId, error);
			Mailer.sendmail(eventId, null, e, Mailer.FAILURE);
			NodeBalancer.INSTANCE.markEventAsFailed(eventId);
		} catch (SyncError e) {
			logger.error("Error While processing read", e);
			marker.setFailed(true);
			SyncError error = new SyncError(e);
			error.setThreadName(Thread.currentThread().getName());
			eventDao.pushError(eventId, error);
			Mailer.sendmail(eventId, null, e, Mailer.FAILURE);
			NodeBalancer.INSTANCE.markEventAsFailed(eventId);
		} finally {
			for (Map.Entry<String, SelectQueryHolder> entry : queryMap.entrySet()) {
				DbResourceUtils.closeResources(null, entry.getValue().getPstmt(), null);
			}
			DbResourceUtils.closeResources(null, null, connection);
			connection = null;
			dataBuffer = null;
			queryMap = null;
			marker = null;
			latch.countDown();
		}
		logger.info("OrclToMngReader Thread Completed at " + System.currentTimeMillis());
	}

	@SuppressWarnings("rawtypes")
	private List<Document> processMongoObject(MongoObject mongoObject, boolean isParent, ResultSet parentRow,
			String parentObjectName) throws SyncError {
		Document document = new Document();
		int rowCount = 0;
		List<Document> documentBundle = new ArrayList<Document>(batchSize);

		boolean isIdentifierPresent = false;
		boolean isComplexIdentifier = false;
		if (mongoObject.getIdentifierList() != null && !mongoObject.getIdentifierList().isEmpty()) {
			isIdentifierPresent = true;
			if (mongoObject.getIdentifierList().size() > 1) {
				isComplexIdentifier = true;
			}
		}

		PreparedStatement stmt = null;
		Object value = null;
		String attributeName = null;
		ResultSet rows = null;
		SelectQueryHolder queryHolder = null;
		try {
			if (mongoObject.getSourceTables() != null && !mongoObject.getSourceTables().isEmpty()) {
				if (!isParent) {
					parentObjectName = parentObjectName + QueryConstants.DOT + mongoObject.getCollectionName();
					logger.debug("*** Parent Object Name **** "+parentObjectName);
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
							column.extractColumnValueFromParentRow(parentRow);
						}
						stmt.setObject(index + 1, bindvalues.get(index).getSqlExpressionForMatchable());
					}
				}
				stmt.setFetchSize(batchSize);
				rows = stmt.executeQuery();
				while (rows.next() && !marker.isFailed()) {
					rowCount++;
					document = new Document();
					for (MongoEntity attribute : mongoObject.getAttributes()) {
						if (attribute instanceof MongoAttribute) {
							MongoAttribute mongoAttribute = (MongoAttribute) attribute;
							if (mongoAttribute.getMappedOracleColumn() !=null && mongoAttribute.getMappedOracleColumn().isParentColumn()) {
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
									parentObjectName);
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
						if (isParent) {
							document.append(SyncConstants.SRC, SyncConstants.ORCL);
							document.append(SyncConstants.SYNC_TIME, System.currentTimeMillis());
						}
						documentBundle.add(document);
					}
					if (documentBundle.size() == batchSize && isParent) {
						logger.info("Offering documentBundle to buffer");
						dataBuffer.put(documentBundle);
						documentBundle = new ArrayList<Document>(batchSize);
						marker.setRowsRead(rowCount);
						eventDao.updateMarker(eventId, marker);
					}
				}
			} else {
				document = new Document();
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
						List<Document> nestedDocList = processMongoObject(mongoNestedObject, false, parentRow,
								parentObjectName);
						MongoAttributeType collectionType = MongoAttributeType
								.valueOf(mongoNestedObject.getCollectionType());
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
						document.append(attributeName, value);
					}
				}
				if (!document.isEmpty()) {
					if (isParent) {
						document.append(SyncConstants.SRC, SyncConstants.ORCL);
						document.append(SyncConstants.SYNC_TIME, System.currentTimeMillis());
					}
					documentBundle.add(document);
				}
			}
			if (documentBundle != null && documentBundle.size() > 0 && isParent) {
				dataBuffer.put(documentBundle);
				marker.setRowsRead(rowCount);
				eventDao.updateMarker(eventId, marker);
				logger.info("All rows migrated. Total rowCount" + rowCount);
			}
		} catch (SQLException e) {
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
				queryHolder.setPstmt(null);
				logger.warn("Error in closing ResultSet", e);
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
				//value = mongoAttribute.getDefaultValue();
				value = getMongoLiteralForDefaultVal(mongoAttribute.getDefaultValue(),mongoAttribute.getAttributeType());
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
				logger.info(object.getCollectionName() +" "+ selectQuery);
				pstmt = connection.prepareStatement(selectQuery, ResultSet.TYPE_FORWARD_ONLY,
						ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
			}
		}
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
}