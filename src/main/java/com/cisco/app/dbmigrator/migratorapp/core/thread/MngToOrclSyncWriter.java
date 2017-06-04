package com.cisco.app.dbmigrator.migratorapp.core.thread;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.constants.SyncConstants;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncError;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncMarker;
import com.cisco.app.dbmigrator.migratorapp.core.map.ColumnAttrMapper;
import com.cisco.app.dbmigrator.migratorapp.core.map.MongoToOracleMap;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoAttributeType;
import com.cisco.app.dbmigrator.migratorapp.core.meta.oracle.NodeGroup;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncAttrs;
import com.cisco.app.dbmigrator.migratorapp.mail.Mailer;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.Literal;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.MatchAble;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleTable;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.QueryConstants;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.SqlColumnType;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.SqlLiteralFactory;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.queries.DeleteQueryBuilder;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.queries.InsertQueryBuilder;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.queries.MergeQueryBuilder;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.queries.SelectQueryBuilder;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.Operations;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.OperationsFactory;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.SQLFilters;
import com.cisco.app.dbmigrator.migratorapp.utilities.cache.DBCacheManager;
import com.cisco.app.dbmigrator.migratorapp.utilities.oracle.DbResourceUtils;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;

import oracle.jdbc.OraclePreparedStatement;

@SuppressWarnings("unchecked")
public class MngToOrclSyncWriter implements Runnable {
	private BlockingQueue<Document> dataBuffer;
	private final MongoToOracleMap map;
	private Connection connection = null;
	private final Logger logger = Logger.getLogger(getClass());
	private TreeMap<OracleTable, QueryHolder> insertStatementMap;
	private TreeMap<OracleTable, QueryHolder> deleteStatementMap;
	private TreeMap<OracleTable, QueryHolder> mergeStatementMap;
	private Map<String, QueryHolder> keyColumnStatementMap;
	private static final String O = "o";
	private static final String O2 = "o2";
	private static final String OP = "op";
	private MongoCollection<Document> collection;
	private SyncMarker marker;
	private final CountDownLatch latch;
	private int retryCount = 0;
	private final boolean isRestrictedSyncEnabled;
	private Map<String, List<Object>> parentKeys;
	private Map<String, List<String>> keyColumnMetaMap;
	private ObjectId eventId;
	private static final long SYNC_DIFF = 2;
	private static final String TS = "ts";
	private boolean syncFlg = false;
	private Object syncTime = null;
	private long waitTime = 300000;
	private final FindOneAndUpdateOptions options;

	public MngToOrclSyncWriter(BlockingQueue<Document> dataBuffer, MongoToOracleMap map, SyncMarker marker,
			CountDownLatch latch, boolean isRestrictedSyncEnabled, ObjectId eventId) {
		super();
		this.dataBuffer = dataBuffer;
		this.map = map;
		this.marker = marker;
		this.latch = latch;
		this.isRestrictedSyncEnabled = isRestrictedSyncEnabled;
		this.eventId = eventId;
		this.options = new FindOneAndUpdateOptions();
		options.returnDocument(ReturnDocument.BEFORE);
	}

	private void refreshConnection() throws SQLException, SyncError, InterruptedException {
		retryCount++;
		// waitTime *= retryCount;
		closeResources();
		DbResourceUtils.closeResources(null, null, connection);
		insertStatementMap = new TreeMap<OracleTable, QueryHolder>(new TableRankComparator());
		deleteStatementMap = new TreeMap<OracleTable, QueryHolder>(new TableRankComparator());
		mergeStatementMap = new TreeMap<OracleTable, QueryHolder>(new TableRankComparator());
		keyColumnStatementMap = new HashMap<String, QueryHolder>();
		keyColumnMetaMap = new HashMap<String, List<String>>();
		boolean connected = false;
		while (retryCount <= 5 && !connected) {
			try {
				logger.info("Attempting to get Oracle connection");
				connection = DBCacheManager.INSTANCE.getCachedOracleConnection(map.getTargetDbName(),
						map.getTargetUserName());
				retryCount = 0;
				connected = true;
			} catch (Exception e) {
				logger.error(
						"Error while attempting to refresh connection. Will retry in  " + waitTime + " milliseconds.",
						e);
				collection = null;
				connected = false;
				Thread.sleep(waitTime);
			}
		}
		if (retryCount >= 6) {
			marker.setFailed(true);
		}
	}

	private void refreshCollectionHandle() throws InterruptedException {
		retryCount++;
		boolean connected = false;
		// waitTime *= retryCount;
		while (retryCount <= 5 && !connected) {
			try {
				logger.info("Attempting to get Mongo connection");
				collection = DBCacheManager.INSTANCE.getCachedMongoPool(map.getSourceDbName(), map.getSourceUserName())
						.getDatabase(map.getSourceDbName()).getCollection(map.getCollectionName());
				retryCount = 0;
				connected = true;
			} catch (Exception e) {
				logger.error("Error while attempting to refresh collection handle. Will retry in  " + waitTime
						+ " milliseconds.", e);
				collection = null;
				connected = false;
				Thread.sleep(waitTime);
			}
		}
		if (retryCount >= 6) {
			marker.setFailed(true);
		}
	}

	private boolean isSyncNeeded(Document doc, BsonTimestamp oplogTs) {
		syncFlg = doc.getBoolean(SyncConstants.SYNC_FLAG, false);
		syncTime = doc.get(SyncConstants.SYNC_TIME);
		boolean syncNeeded = false;
		if (syncFlg) {
			syncNeeded = true;
		} else {
			if (!isRestrictedSyncEnabled) {
				if (syncTime != null) {
					if (oplogTs.getTime() - (Long.valueOf(String.valueOf(syncTime)) / 1000) > SYNC_DIFF) {
						syncNeeded = true;
					}
				} else {
					syncNeeded = true;
				}
			}
		}
		return syncNeeded;
	}

	@SuppressWarnings("unused")
	private String loadJsonFromFile(String path) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		br.close();
		return sb.toString();
	}

	@Override
	public void run() {
		logger.info("Sync Writer started");
		Document rootDoc = null;
		Document doc = null;
		Map<String, Object> parentValueMap = null;
		BsonTimestamp oplogTs = null;
		try {
			retryCount = 0;
			while (!marker.isFailed()) {
				try {
					if (connection == null || connection.isClosed()) {
						refreshConnection();
					}
					if (collection == null) {
						refreshCollectionHandle();// Mongo
					}
					// waitTime = 30000;
					// retryCount = 0;
					parentValueMap = new HashMap<String, Object>();
					rootDoc = dataBuffer.take();
					connection.setAutoCommit(false);
					SyncOperation syncOperation = SyncOperation.valueOf(rootDoc.getString(OP));
					oplogTs = (BsonTimestamp) rootDoc.get(TS);
					doc = null;

					if (SyncOperation.i.equals(syncOperation) || SyncOperation.u.equals(syncOperation)) {
						if (SyncOperation.i.equals(syncOperation)) {
							doc = (Document) rootDoc.get(O);
						} else if (SyncOperation.u.equals(syncOperation)) {
							Document idDoc = (Document) rootDoc.get(O2);
							Object obj = idDoc.get(SyncAttrs.ID);
							// doc = collection.find(Filters.eq(SyncAttrs.ID,
							// obj)).first();
							if (isRestrictedSyncEnabled) {
								doc = collection.findOneAndUpdate(
										Filters.and(Filters.eq(SyncAttrs.ID, obj),
												Filters.eq(SyncConstants.SYNC_FLAG, true)),
										Updates.set(SyncConstants.SYNC_FLAG, false), options);
							} else {
								doc = collection.find(Filters.eq(SyncAttrs.ID, obj)).first();
							}
						}
						// Temp for debugging only , Comment before commiting
						// doc = collection.find(Filters.eq("estId",
						// 90378715)).first();
						if (doc != null) {
							if (isSyncNeeded(doc, oplogTs)) {
								// if (true) {
								logger.info("Document picked with ID " + doc.get(SyncAttrs.ID));
								parentValueMap.put(SyncAttrs.ID, doc.get(SyncAttrs.ID));
								parentKeys = new HashMap<String, List<Object>>();
								List<Document> dataMapList = Arrays.asList(doc);
								Set<String> deletedTableSet = new HashSet<String>();
								deleteOldData(map.getRootNode().get(0), true, doc, deletedTableSet,
										String.valueOf(MongoAttributeType.COLLECTION), syncOperation,
										map.getRootNode().get(0).getNodeName());
								deletedTableSet.clear();
								processSyncDocument(map.getRootNode().get(0), dataMapList, parentValueMap,
										map.getCollectionName(), true);
								for (Map.Entry<OracleTable, QueryHolder> statementEntry : mergeStatementMap
										.entrySet()) {
									OraclePreparedStatement pstmt = statementEntry.getValue().getPstmt();
									logger.debug("Merging into table " + statementEntry.getKey().getTableName());
									pstmt.executeBatch();
									logger.debug("Record Merged");
									pstmt.clearBatch();
								}
								for (Map.Entry<OracleTable, QueryHolder> statementEntry : deleteStatementMap
										.descendingMap().entrySet()) {
									OraclePreparedStatement pstmt = statementEntry.getValue().getPstmt();
									logger.debug("Deleting from table " + statementEntry.getKey().getTableName());
									pstmt.executeBatch();
									logger.debug("Old records deleted");
									pstmt.clearBatch();
								}
								for (Map.Entry<OracleTable, QueryHolder> statementEntry : insertStatementMap
										.entrySet()) {
									OraclePreparedStatement pstmt = statementEntry.getValue().getPstmt();
									logger.debug("Inserting into table " + statementEntry.getKey().getTableName());
									pstmt.executeBatch();
									logger.debug("New Snapshot inserted");
									pstmt.clearBatch();
								}
								connection.commit();
								logger.info("Statement committed for id " + doc.get(SyncAttrs.ID));
							}
						}
					} else if (SyncOperation.d.equals(syncOperation)) {
						Document idDoc = (Document) rootDoc.get(O);
						Object deletedId = idDoc.get(SyncAttrs.ID);
						if (!(deletedId instanceof ObjectId || deletedId instanceof Document)) {
							logger.info("Document picked for deletion with ID " + deletedId);
							Set<String> deletedTableSet = new HashSet<String>();
							parentKeys = new HashMap<String, List<Object>>();
							deleteOldData(map.getRootNode().get(0), true, new Document(SyncAttrs.ID, deletedId),
									deletedTableSet, String.valueOf(MongoAttributeType.COLLECTION), syncOperation,
									map.getRootNode().get(0).getNodeName());
							deletedTableSet.clear();
							for (Map.Entry<OracleTable, QueryHolder> statementEntry : deleteStatementMap.descendingMap()
									.entrySet()) {
								OraclePreparedStatement pstmt = statementEntry.getValue().getPstmt();
								logger.debug("Deleting from table " + statementEntry.getKey().getTableName());
								pstmt.executeBatch();
								logger.debug("Old records deleted");
								pstmt.clearBatch();
							}
							connection.commit();
							logger.info("Document Deleted with ID " + deletedId);
						}
					}

				} catch (SQLException e) {
					logger.error("Error while processing Replication", e);
					try {
						Mailer.sendmail(eventId, String.valueOf(doc), e, Mailer.FAILURE);
						rootDoc = null;
						if (DbResourceUtils.isValidConnection(connection)) {
							for (QueryHolder holder : insertStatementMap.values()) {
								holder.getPstmt().clearBatch();
							}
							for (QueryHolder holder : deleteStatementMap.values()) {
								holder.getPstmt().clearBatch();
							}
							for (QueryHolder holder : mergeStatementMap.values()) {
								holder.getPstmt().clearBatch();
							}
							connection.rollback();
						} else {
							refreshConnection();
						}
					} catch (Exception e1) {
						closeResources();
						connection = null;
						logger.error("Error in rolling back changes", e1);
					}
				} catch (InterruptedException e) {
					Mailer.sendmail(eventId, String.valueOf(doc), e, Mailer.FAILURE);
					logger.error("Error while processing Replication", e);
					collection = null;
					try {
						connection.rollback();
					} catch (SQLException e1) {
						logger.error("Error in rolling back changes", e1);
					}
				} catch (Exception e) {
					Mailer.sendmail(eventId, String.valueOf(doc), e, Mailer.FAILURE);
					logger.error("Error while processing Replication", e);
					collection = null;
					try {
						connection.rollback();
					} catch (SQLException e1) {
						closeResources();
						logger.error("Error in rolling back changes", e1);
					}
				}
			}
		} finally {
			closeResources();
			keyColumnMetaMap = null;
			retryCount++;
			marker = null;
			collection = null;
			connection = null;
		}
		logger.info(Thread.currentThread().getName() + "terminated");
		dataBuffer = null;
		latch.countDown();
	}

	private void closeResources() {
		if (insertStatementMap != null) {
			for (QueryHolder holder : insertStatementMap.values()) {
				DbResourceUtils.closeResources(null, holder.getPstmt(), null);
			}
		}
		if (deleteStatementMap != null) {
			for (QueryHolder holder : deleteStatementMap.values()) {
				DbResourceUtils.closeResources(null, holder.getPstmt(), null);
			}
		}
		if (mergeStatementMap != null) {
			for (QueryHolder holder : mergeStatementMap.values()) {
				DbResourceUtils.closeResources(null, holder.getPstmt(), null);
			}
		}
		if (keyColumnStatementMap != null) {
			for (QueryHolder holder : keyColumnStatementMap.values()) {
				if (holder != null) {
					DbResourceUtils.closeResources(null, holder.getPstmt(), null);
				}
			}
		}
		DbResourceUtils.closeResources(null, null, connection);
	}

	private void processSyncDocument(NodeGroup node, List<Document> dataMapList, Map<String, Object> parentValueMap,
			String parentNodeName, boolean isParent) throws SQLException {
		logger.debug("processSyncDocument for node " + node.getNodeName());
		if (node != null && node.getColumnAttrMappers() != null && !node.getColumnAttrMappers().isEmpty()) {
			QueryHolder queryHolder = null;
			for (Document dataMap : dataMapList) {
				for (OracleTable table : node.getTableList()) {
					if (!isParent) {
						if (insertStatementMap.get(table) != null) {
							queryHolder = insertStatementMap.get(table);
						} else {
							queryHolder = getQueryHolder(node, table, SyncOperation.i);
							insertStatementMap.put(table, queryHolder);
						}
					} else {
						if (mergeStatementMap.get(table) != null) {
							queryHolder = mergeStatementMap.get(table);
						} else {
							queryHolder = getQueryHolder(node, table, SyncOperation.u);
							mergeStatementMap.put(table, queryHolder);
						}
					}
					putValuesInStatement(queryHolder.getPstmt(), queryHolder.getColumnAliasSet(), node, dataMap,
							parentValueMap);
				}
				if (node.getReferenceAttributes() != null) {
					for (String attributeName : node.getReferenceAttributes()) {
						parentValueMap.put(parentNodeName + QueryConstants.DOT + attributeName,
								dataMap.get(attributeName));
					}
				}
				processChildNodes(node, dataMap, parentValueMap, parentNodeName);
			}
		} else {
			for (Document dataMap : dataMapList) {
				if (node != null && node.getReferenceAttributes() != null) {
					for (String attributeName : node.getReferenceAttributes()) {
						parentValueMap.put(parentNodeName + QueryConstants.DOT + attributeName,
								dataMap.get(attributeName));
					}
				}
				processChildNodes(node, dataMap, parentValueMap, parentNodeName);
			}
		}
	}

	private void processChildNodes(NodeGroup rootNode, Document dataMap, Map<String, Object> parentValueMap,
			String parentNodeName) throws SQLException {
		logger.debug("Processing child nodes for root node " + rootNode.getNodeName());
		List<NodeGroup> childNodes = rootNode.getChildGroups();
		List<Document> childDataMapList = null;
		if (childNodes != null && !childNodes.isEmpty()) {
			for (NodeGroup childNode : childNodes) {
				Object childDataObj = dataMap.get(childNode.getNodeName());
				if (childDataObj != null) {
					if (childDataObj instanceof List) {
						childDataMapList = (List<Document>) childDataObj;
					} else {
						Document childDataMap = (Document) childDataObj;
						if (!childDataMap.isEmpty()) {
							childDataMapList = Arrays.asList(childDataMap);
						}
					}
					if (childDataMapList != null && !childDataMapList.isEmpty()) {
						processSyncDocument(childNode, childDataMapList, parentValueMap,
								parentNodeName + QueryConstants.DOT + childNode.getNodeName(), false);
					}
				}
			}
		}
	}

	private void putValuesInStatement(OraclePreparedStatement pstmt, Set<String> columnAliasSet, NodeGroup node,
			Document dataMap, Map<String, Object> parentValueMap) throws SQLException {
		logger.debug("Putting values in PreparedStatement");
		for (String columnAlias : columnAliasSet) {
			ColumnAttrMapper mapper = node.getColumnAttrMappers().get(columnAlias);
			Object mongoValue = null;
			if (mapper.getAttribute() != null) {
				if (mapper.isParentAttribute()) {
					if (!mapper.getAttribute().isIdentifier()) {
						String parentAttrNode = mapper.getParentAttributeNode();
						mongoValue = parentValueMap
								.get(parentAttrNode + QueryConstants.DOT + mapper.getAttribute().getAttributeName());
					} else {
						mongoValue = parentValueMap.get(SyncAttrs.ID);
					}
				} else if (mapper.isChildAttribute()) {
					mongoValue = getChildValue(mapper.getChildAttributeNode(), dataMap,
							mapper.getAttribute().getAttributeName());
				} else {
					if (dataMap != null) {
						mongoValue = dataMap.get(mapper.getAttribute().getAttributeName());
					}
				}
			}
			Object columnValue = getColumnValue(mongoValue, mapper, parentValueMap);
			if (SqlColumnType.BLOB.equals(mapper.getColumn().getColumnType())) {
				pstmt.setBlobAtName(columnAlias, (Blob) columnValue);
			} else if (SqlColumnType.CLOB.equals(mapper.getColumn().getColumnType())) {
				pstmt.setClobAtName(columnAlias, (Clob) columnValue);
			} else {
				pstmt.setObjectAtName(columnAlias, columnValue);
			}
		}
		pstmt.addBatch();
		logger.debug("Statement added to batch");
	}

	private String getMergeQuery(OracleTable table, Set<String> columnAliasSet, NodeGroup node) {
		MergeQueryBuilder builder = MergeQueryBuilder.getBuilder();
		SQLFilters mergeFilter = null;
		for (OracleColumn keyColumn : table.getKeyColumns()) {
			if (mergeFilter == null) {
				mergeFilter = SQLFilters.getFilter(Operations.eq(keyColumn, null));
			} else {
				mergeFilter.AND(Operations.eq(keyColumn, null));
			}
		}
		String mergeQuery = builder.merge().into(table).usingDual().on(mergeFilter).getMergeQuery(columnAliasSet);
		for (Map.Entry<String, ColumnAttrMapper> mapper : node.getColumnAttrMappers().entrySet()) {
			if (mapper.getValue().isSeqGenerated()) {
				mergeQuery = mergeQuery.replace(QueryConstants.COLON + mapper.getKey(),
						mapper.getValue().getSeqName() + QueryConstants.NEXTVAL);
				columnAliasSet.remove(mapper.getKey());
			}
		}
		logger.info(mergeQuery);
		return mergeQuery;
	}

	private String getDeleteQuery(OracleTable table, Set<String> columnAliasSet) {
		DeleteQueryBuilder builder = new DeleteQueryBuilder();
		SQLFilters deleteFilter = null;
		for (OracleColumn keyColumn : table.getKeyColumns()) {
			if (deleteFilter == null) {
				deleteFilter = SQLFilters.getFilter(Operations.eq(keyColumn, null));
			} else {
				deleteFilter.AND(Operations.eq(keyColumn, null));
			}
		}
		String deleteQuery = builder.delete().from(table.getTableName(), table.getTableAlias()).where(deleteFilter)
				.getQuery(columnAliasSet);
		logger.info(deleteQuery);
		return deleteQuery;
	}

	private String getInsertQuery(NodeGroup node, OracleTable table, Set<String> columnAliasSet) {
		InsertQueryBuilder builder = new InsertQueryBuilder();
		String insertQuery = builder.insert().into(table).getQuery(columnAliasSet);
		for (Map.Entry<String, ColumnAttrMapper> mapper : node.getColumnAttrMappers().entrySet()) {
			if (mapper.getValue().isSeqGenerated()) {
				insertQuery = insertQuery.replace(QueryConstants.COLON + mapper.getKey(),
						mapper.getValue().getSeqName() + QueryConstants.NEXTVAL);
				columnAliasSet.remove(mapper.getKey());
			}
		}
		logger.info(insertQuery);
		return insertQuery;
	}

	private Object getChildValue(String childNode, Map<String, Object> dataMap, String attributeName) {
		Object value = null;
		if (dataMap != null) {
			// Map<String, Object> childDocument = dataMap;
			StringTokenizer tokenizer = new StringTokenizer(childNode, QueryConstants.DOT);
			tokenizer.nextElement();// Take out parentNodeName
			while (tokenizer.hasMoreTokens()) {
				dataMap = (Document) dataMap.get(tokenizer.nextToken());
				if (dataMap == null) {
					break;
				}
			}
			if (dataMap != null) {
				value = dataMap.get(attributeName);
			}
		}
		return value;
	}

	@SuppressWarnings("rawtypes")
	private Object getColumnValue(Object mongoValue, ColumnAttrMapper mapper, Map<String, Object> parentValueMap)
			throws SQLException {
		Object value = null;
		if (mongoValue != null) {
			logger.debug(mongoValue + "  " + mapper.getColumn().getColumnName());
			if (SqlColumnType.CLOB.equalsIgnoreCase(mapper.getColumn().getColumnType())
					|| SqlColumnType.BLOB.equalsIgnoreCase(mapper.getColumn().getColumnType())) {
				value = SqlLiteralFactory.getLobLiteral(mongoValue, mapper.getColumn().getColumnType(), connection)
						.getLiteralValue();
			} else {
				if (mapper.getReplacementMap() != null && mapper.getReplacementMap().containsKey(mongoValue)) {
					mongoValue = mapper.getReplacementMap().get(mongoValue);
				}
				value = SqlLiteralFactory.getLiteral(mongoValue, mapper.getColumn().getColumnType()).getLiteralValue();
			}
		} else {
			Literal defaultValue = mapper.getLiteralValueForColumn();
			if (defaultValue != null) {
				value = defaultValue.getLiteralValue();
			}
		}
		return value;
	}

	private QueryHolder getQueryHolder(NodeGroup node, OracleTable table, SyncOperation operation) throws SQLException {
		QueryHolder queryHolder = new QueryHolder();
		Set<String> columnAliasSet = new HashSet<String>();
		String query = null;
		switch (operation) {
		case i:
			query = getInsertQuery(node, table, columnAliasSet);
			break;
		case u:
			query = getMergeQuery(table, columnAliasSet, node);
			break;
		case d:
			query = getDeleteQuery(table, columnAliasSet);
			break;
		default:
			break;
		}
		OraclePreparedStatement pstmt = (OraclePreparedStatement) connection.prepareStatement(query);
		queryHolder = new QueryHolder();
		queryHolder.setColumnAliasSet(columnAliasSet);
		queryHolder.setPstmt(pstmt);
		return queryHolder;
	}

	private static final class QueryHolder {
		Set<String> columnAliasSet;
		OraclePreparedStatement pstmt;

		public Set<String> getColumnAliasSet() {
			return columnAliasSet;
		}

		public void setColumnAliasSet(Set<String> columnAliasSet) {
			this.columnAliasSet = columnAliasSet;
		}

		public OraclePreparedStatement getPstmt() {
			return pstmt;
		}

		public void setPstmt(OraclePreparedStatement pstmt) {
			this.pstmt = pstmt;
		}
	}

	private static class TableRankComparator implements Comparator<OracleTable> {

		@Override
		public int compare(OracleTable o1, OracleTable o2) {
			return o1.getRank() - o2.getRank();
		}

	}

	private OracleColumn getDummyParentColumn(OracleColumn column) {
		OracleColumn parentColumn = new OracleColumn();
		parentColumn.setParentColumn(true);
		parentColumn.setColumnAlias(column.getColumnAlias());
		parentColumn.setColumnName(column.getColumnName());
		parentColumn.setColumnType(column.getColumnType());
		parentColumn.setTableAlias(column.getTableAlias());
		return parentColumn;
	}

	private void getParentKeyQuery(NodeGroup node, String parentNodeName) throws SQLException {
		logger.debug("In getParentKeyQuery for node : " + node.getNodeName());
		OracleTable[] dummyTableArr = new OracleTable[node.getTableList().size()];

		int tableIndex = 0;
		Set<String> derivedReferences = new HashSet<String>();
		List<String> referenceAttrs = node.getReferenceAttributes();
		for (OracleTable nodeTable : node.getTableList()) {
			if (!keyColumnStatementMap
					.containsKey(node.getNodeName() + QueryConstants.DOT + nodeTable.getTableName())) {
				OracleTable dummyTable = new OracleTable();
				dummyTable.setTableName(nodeTable.getTableName());
				dummyTable.setTableAlias(nodeTable.getTableAlias());
				dummyTable.setKeyColumns(nodeTable.getKeyColumns());
				List<String> fetchedColumns = new ArrayList<String>();
				Iterator<String> it = referenceAttrs.iterator();
				while (it.hasNext()) {
					ColumnAttrMapper mapper = null;
					String refAttr = it.next();
					if (!derivedReferences.contains(refAttr)) {
						for (Map.Entry<String, ColumnAttrMapper> mapperEntry : node.getColumnAttrMappers().entrySet()) {
							if (mapperEntry.getValue().getAttribute().getAttributeName().equalsIgnoreCase(refAttr)) {
								mapper = mapperEntry.getValue();
								if (dummyTable.getTableAlias()
										.equalsIgnoreCase(mapper.getColumnData().getTableAlias())) {
									break;
								} else {
									mapper = null;
								}
							}
						}
					}
					if (mapper != null && dummyTable.getTableAlias().equals(mapper.getColumn().getTableAlias())) {
						derivedReferences.add(refAttr);
						dummyTable.addColumn(mapper.getColumn());
						fetchedColumns.add(mapper.getColumn().getColumnAlias());
						parentKeys.put(getKeyName(mapper, parentNodeName), new ArrayList<Object>());
					}
				}
				if (!fetchedColumns.isEmpty()) {
					keyColumnMetaMap.put(node.getNodeName() + QueryConstants.DOT + dummyTable.getTableName(),
							fetchedColumns);
					dummyTableArr[tableIndex++] = dummyTable;
				} else {
					keyColumnStatementMap.put(node.getNodeName() + QueryConstants.DOT + dummyTable.getTableName(),
							null);
				}
			}
		}
		QueryHolder queryHolder = null;
		OraclePreparedStatement pstmt = null;
		String query = null;
		for (OracleTable dummyTable : dummyTableArr) {
			Set<String> columnAliasSet = null;
			if (dummyTable != null) {
				SQLFilters filter = null;
				List<OracleColumn> keyColumns = dummyTable.getKeyColumns();
				columnAliasSet = new HashSet<String>(keyColumns.size());
				for (OracleColumn keyColumn : keyColumns) {
					columnAliasSet.add(keyColumn.getColumnAlias());
					if (filter == null) {
						filter = SQLFilters.getFilter(
								OperationsFactory.getMatchExpression(keyColumn, getDummyParentColumn(keyColumn), "eq"));
					} else {
						filter.AND(
								OperationsFactory.getMatchExpression(keyColumn, getDummyParentColumn(keyColumn), "eq"));
					}
				}
				SelectQueryBuilder queryBuilder = new SelectQueryBuilder();
				@SuppressWarnings("rawtypes")
				List<MatchAble> bindValues = new ArrayList<MatchAble>();
				query = queryBuilder.select().from(dummyTable).where(filter).getPreparedStatement(bindValues, false);
				logger.debug("Key Query :" + query);
				pstmt = (OraclePreparedStatement) connection.prepareStatement(query);
				queryHolder = new QueryHolder();
				queryHolder.setPstmt(pstmt);
				queryHolder.setColumnAliasSet(columnAliasSet);
				keyColumnStatementMap.put(node.getNodeName() + QueryConstants.DOT + dummyTable.getTableName(),
						queryHolder);
			}
		}
	}

	private void getExistingChildKeys(NodeGroup node, String parentNodeName) {
		logger.debug("Get Existing Keys called for node" + node.getNodeName());
		ResultSet rset = null;
		try {
			getParentKeyQuery(node, parentNodeName);
			for (OracleTable table : node.getTableList()) {
				QueryHolder holder = keyColumnStatementMap
						.get(node.getNodeName() + QueryConstants.DOT + table.getTableName());
				if (holder != null) {
					OraclePreparedStatement pstmt = holder.getPstmt();
					pstmt.clearParameters();
					logger.info("Executing Key Column Query for table : " + table.getTableName());
					for (String columnAlias : holder.getColumnAliasSet()) {
						ColumnAttrMapper mapper = node.getColumnAttrMappers().get(columnAlias);
						if (mapper.getAttribute().isIdentifier()) {
							pstmt.setObject(1, parentKeys.get(SyncAttrs.ID).get(0));
						} else {
							pstmt.setObject(1, parentKeys.get(getKeyName(mapper, parentNodeName)).get(0));
						}
					}
					rset = pstmt.executeQuery();
					List<String> fetchedColumns = keyColumnMetaMap
							.get(node.getNodeName() + QueryConstants.DOT + table.getTableName());
					while (rset.next()) {
						for (String columnAlias : fetchedColumns) {
							List<Object> keys = parentKeys
									.get(getKeyName(node.getColumnAttrMappers().get(columnAlias), parentNodeName));
							if (keys == null) {
								keys = new ArrayList<Object>();
							}
							keys.add(rset.getObject(columnAlias));
							parentKeys.put(getKeyName(node.getColumnAttrMappers().get(columnAlias), parentNodeName),
									keys);
						}
					}
					logger.debug("parentKeys" + parentKeys);
					DbResourceUtils.closeResources(rset, null, null);
				}
			}
		} catch (SQLException e) {
			logger.error("Error while getting parent Keys", e);
			Mailer.sendmail(eventId, null, e, Mailer.FAILURE);
			DbResourceUtils.closeResources(rset, null, null);
			for (QueryHolder queryHolder : keyColumnStatementMap.values()) {
				if (queryHolder != null) {
					DbResourceUtils.closeResources(null, queryHolder.getPstmt(), null);
				}
			}
			keyColumnStatementMap.clear();
			keyColumnMetaMap.clear();
		} finally {
			DbResourceUtils.closeResources(rset, null, null);
		}
	}

	@SuppressWarnings("rawtypes")
	private void deleteOldData(NodeGroup rootNode, boolean isParent, Document rootDoc, Set<String> deletedTableSet,
			String parentNodeType, SyncOperation op, String parentNodeName) throws SQLException {
		if (isParent) { // Put it for all cases
			parentKeys.put(SyncAttrs.ID, Arrays.asList(rootDoc.get(SyncAttrs.ID)));
		}
		if (rootNode.getReferenceAttributes() != null && !rootNode.getReferenceAttributes().isEmpty()) {
			if (isParent) {
				if (rootNode.getReferenceAttributes() != null) {
					for (String attributeName : rootNode.getReferenceAttributes()) {
						parentKeys.put(rootNode.getNodeName() + QueryConstants.DOT + attributeName,
								Arrays.asList(rootDoc.get(attributeName)));
					}
				}
			} else {
				getExistingChildKeys(rootNode, parentNodeName);
			}
		}
		if (rootNode.getChildGroups() != null) {
			for (NodeGroup childNode : rootNode.getChildGroups()) {
				deleteOldData(childNode, false, null, deletedTableSet, rootNode.getNodeType(), op,
						parentNodeName + QueryConstants.DOT + childNode.getNodeName());
			}
		}
		if (!isParent || SyncOperation.d.equals(op)) {
			QueryHolder deleteQueryHolder = null;
			if (rootNode.getTableList() != null) {
				for (OracleTable table : rootNode.getTableList()) {
					if (!deletedTableSet.contains(table.getTableName())) {
						// deletedTableSet.add(table.getTableName());
						if (deleteStatementMap.get(table) != null) {
							deleteQueryHolder = deleteStatementMap.get(table);
						} else {
							deleteQueryHolder = getQueryHolder(rootNode, table, SyncOperation.d);
							deleteStatementMap.put(table, deleteQueryHolder);
						}
						ColumnAttrMapper mapper = null;
						OraclePreparedStatement pstmt = deleteQueryHolder.getPstmt();
						for (String columnAlias : deleteQueryHolder.getColumnAliasSet()) {
							mapper = rootNode.getColumnAttrMappers().get(columnAlias);

							if (mapper.getAttribute().isIdentifier()) {
								List<Object> parentKeyList = parentKeys.get(SyncAttrs.ID);
								Literal literal = SqlLiteralFactory.getLiteral(parentKeyList.get(0),
										mapper.getColumn().getColumnType());
								pstmt.setObjectAtName(columnAlias, literal.getLiteralValue());
								pstmt.addBatch();
							} else {

								List<Object> parentKeyList = parentKeys.get(getKeyName(mapper, rootNode.getNodeName()));
								if (parentKeyList != null) {
									for (Object parentKey : parentKeyList) {
										logger.debug("Attribute : " + mapper.getAttribute());
										Literal literal = SqlLiteralFactory.getLiteral(parentKey,
												mapper.getColumn().getColumnType());
										pstmt.setObjectAtName(columnAlias, literal.getLiteralValue());
										pstmt.addBatch();
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private String getKeyName(ColumnAttrMapper mapper, String nodeName) {
		String keyName = null;
		if (mapper.isParentAttribute()) {
			keyName = mapper.getParentAttributeNode() + QueryConstants.DOT + mapper.getAttribute().getAttributeName();
		} else {
			keyName = nodeName + QueryConstants.DOT + mapper.getAttribute().getAttributeName();
		}
		return keyName;
	}
}