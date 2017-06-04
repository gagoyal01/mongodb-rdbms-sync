package com.cisco.app.dbmigrator.migratorapp.core.thread;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;
import org.bson.Document;

import com.cisco.app.dbmigrator.migratorapp.constants.SyncConstants;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncError;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncMarker;
import com.cisco.app.dbmigrator.migratorapp.core.map.ColumnAttrMapper;
import com.cisco.app.dbmigrator.migratorapp.core.map.MongoToOracleMap;
import com.cisco.app.dbmigrator.migratorapp.core.meta.oracle.NodeGroup;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncAttrs;
import com.cisco.app.dbmigrator.migratorapp.mail.Mailer;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.Literal;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleTable;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.QueryConstants;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.SqlColumnType;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.SqlLiteralFactory;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.queries.DeleteQueryBuilder;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.queries.InsertQueryBuilder;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.queries.MergeQueryBuilder;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.Operations;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.SQLFilters;
import com.cisco.app.dbmigrator.migratorapp.utilities.cache.DBCacheManager;
import com.cisco.app.dbmigrator.migratorapp.utilities.oracle.DbResourceUtils;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import oracle.jdbc.OraclePreparedStatement;

@SuppressWarnings("unchecked")
public class MngToOrclSync implements Runnable {
	private BlockingQueue<Document> dataBuffer;
	private final MongoToOracleMap map;
	private Connection connection = null;
	private final Logger logger = Logger.getLogger(getClass());
	private Map<String, QueryHolder> statementMap;
	private static final String UNDERSCORE = "_";
	private static final String O = "o";
	private static final String O2 = "o2";
	private static final String OP = "op";
	private MongoCollection<Document> collection;
	private SyncMarker marker;
	private final CountDownLatch latch;
	private int retryCount = 0;
	private final boolean isRestrictedSyncEnabled;
	private static enum SyncOperation {
		i, u, d;
	}

	public MngToOrclSync(BlockingQueue<Document> dataBuffer, MongoToOracleMap map, SyncMarker marker,
			CountDownLatch latch , boolean isRestrictedSyncEnabled) {
		super();
		this.dataBuffer = dataBuffer;
		this.map = map;
		this.marker = marker;
		this.latch = latch;
		this.isRestrictedSyncEnabled=isRestrictedSyncEnabled;
	}

	private void refreshConnection() throws SQLException, SyncError {
		retryCount++;
		for (QueryHolder holder : statementMap.values()) {
			DbResourceUtils.closeResources(null, holder.getPstmt(), null);
		}
		DbResourceUtils.closeResources(null, null, connection);
		statementMap = new LinkedHashMap<String, QueryHolder>();
		connection = DBCacheManager.INSTANCE
				.getCachedOracleConnection(map.getTargetDbName(), map.getTargetUserName());
	}

	private void refreshCollectionHandle() {
		retryCount++;
		collection = DBCacheManager.INSTANCE.getCachedMongoPool(map.getSourceDbName(), map.getSourceUserName())
				.getDatabase(map.getSourceDbName()).getCollection(map.getCollectionName());
	}

	@Override
	public void run() {
		logger.info("Sync Writer started");
		Document rootDoc = null;
		statementMap = new LinkedHashMap<String, QueryHolder>();
		Map<String, Object> parentValueMap = null;
		boolean syncFlg = false;
		try {
			retryCount = 0;
			// map.setNodeGroupMap(new TreeMap<String,
			// NodeGroup>(map.getNodeGroupMap()));
			while (retryCount < 5) {
				try {
					if (marker.isFailed()) {
						break;
					}
					if (connection == null || connection.isClosed()) {
						Thread.sleep(30000);
						refreshConnection();
					}
					if (collection == null) {
						Thread.sleep(30000);
						refreshCollectionHandle();
					}
					retryCount = 0;
					parentValueMap = new HashMap<String, Object>();
					rootDoc = dataBuffer.take();
					connection.setAutoCommit(false);
					SyncOperation syncOperation = SyncOperation.valueOf(rootDoc.getString(OP));
					Document doc = null;
					if (SyncOperation.i.equals(syncOperation)) {
						doc = (Document) rootDoc.get(O);
					} else if (SyncOperation.u.equals(syncOperation)) {
						Document idDoc = (Document) rootDoc.get(O2);
						Object obj = idDoc.get(SyncAttrs.ID);
						doc = collection.find(Filters.eq(SyncAttrs.ID, obj)).first();
					}
					//String src = doc.getString("src");
					syncFlg = doc.getBoolean(SyncConstants.SYNC_FLAG, false);
					if (!isRestrictedSyncEnabled || syncFlg) {
						logger.info("Document picked with ID " + doc.get(SyncAttrs.ID));
						parentValueMap.put(SyncAttrs.ID, doc.get(SyncAttrs.ID));
						List<Document> dataMapList = Arrays.asList(doc);
						// Processing only one root as of now. Change to add capability to process all
						processSyncDocument(map.getRootNode().get(0), syncOperation, dataMapList, parentValueMap,
								map.getCollectionName());
						for (Map.Entry<String, QueryHolder> statementEntry : statementMap.entrySet()) {
							OraclePreparedStatement pstmt = statementEntry.getValue().getPstmt();
							pstmt.executeBatch();
							logger.debug("Statement executed");
							pstmt.clearBatch();
						}
						connection.commit();
						if(syncFlg){
							collection.updateOne(Filters.eq(SyncAttrs.ID, doc.get(SyncAttrs.ID)), Updates.set(SyncConstants.SYNC_FLAG, false));
						}
						logger.info("Statement committed for id " + doc.get(SyncAttrs.ID));
					}

				} catch (SQLException e) {
					logger.error("Error while processing Replication", e);
					Mailer.sendmail("sync_dev", "pnilayam", "Notif", e.getMessage() + rootDoc, null);
					rootDoc = null;
					try {
						if (connection != null && !connection.isClosed()) {
							connection.rollback();
							for (QueryHolder holder : statementMap.values()) {
								// DbResourceUtils.closeResources(null,
								// holder.getPstmt(), null);
								holder.getPstmt().clearBatch();
							}
						} else {
							refreshConnection();
						}
					} catch (Exception e1) {
						connection = null;
						logger.error("Error in rolling back changes", e1);
					}
				} catch (InterruptedException e) {
					Mailer.sendmail("sync_dev", "pnilayam", "Notif", e.getMessage() + rootDoc, null);
					logger.error("Error while processing Replication", e);
					collection = null;
					try {
						connection.rollback();
					} catch (SQLException e1) {
						logger.error("Error in rolling back changes", e1);
					}
				} catch (Exception e) {
					Mailer.sendmail("sync_dev", "pnilayam", "Notif", e.getMessage() + rootDoc, null);
					logger.error("Error while processing Replication", e);
					collection = null;
					try {
						connection.rollback();
					} catch (SQLException e1) {
						logger.error("Error in rolling back changes", e1);
					}
				}
			}
		} finally {
			for (QueryHolder holder : statementMap.values()) {
				DbResourceUtils.closeResources(null, holder.getPstmt(), null);
			}
			DbResourceUtils.closeResources(null, null, connection);
			retryCount++;
			marker = null;
			collection = null;
			connection = null;
		}
		logger.info(Thread.currentThread().getName() + "terminated");
		dataBuffer = null;
		latch.countDown();
	}

	private void processSyncDocument(NodeGroup node, SyncOperation syncOperation, List<Document> dataMapList,
			Map<String, Object> parentValueMap, String parentNodeName) throws SQLException {
		logger.debug("processSyncDocument for node " + node.getNodeName());
		// NodeGroup node = map.getRootNode().get(0);// Taking only the first as
		// of now. Change to accept multiple root nodes
		if (node != null && node.getColumnAttrMappers() != null && !node.getColumnAttrMappers().isEmpty()) {
			QueryHolder queryHolder = null;
			syncOperation = SyncOperation.u;// TODO : Added to check Merge Only
											// flow
			if (SyncOperation.i.equals(syncOperation)) {
				if (statementMap.get(node.getNodeName()) != null) {
					queryHolder = statementMap.get(node.getNodeName());
				} else {
					queryHolder = getQueryHolder(node, null, syncOperation);
					statementMap.put(node.getNodeName(), queryHolder);
				}
				for (Document dataMap : dataMapList) {
					putValuesInStatement(queryHolder.getPstmt(), queryHolder.getColumnAliasSet(), node, dataMap,
							parentValueMap, parentNodeName);

					if (node.getReferenceAttributes() != null) {
						for (String attributeName : node.getReferenceAttributes()) {
							parentValueMap.put(node.getNodeName() + QueryConstants.DOT + attributeName,
									dataMap.get(attributeName));
						}
					}
					processChildNodes(node, dataMap, parentValueMap, syncOperation);
				}
			} else if (SyncOperation.u.equals(syncOperation) || SyncOperation.d.equals(syncOperation)) {
				for (Document dataMap : dataMapList) {
					for (OracleTable table : node.getTableList()) {
						String key = table.getTableAlias() + UNDERSCORE + syncOperation.name();
						if (statementMap.get(key) != null) {
							queryHolder = statementMap.get(key);
						} else {
							queryHolder = getQueryHolder(node, table, syncOperation);
							statementMap.put(key, queryHolder);
						}
						putValuesInStatement(queryHolder.getPstmt(), queryHolder.getColumnAliasSet(), node, dataMap,
								parentValueMap, parentNodeName);
					}
					if (node.getReferenceAttributes() != null) {
						for (String attributeName : node.getReferenceAttributes()) {
							parentValueMap.put(node.getNodeName() + QueryConstants.DOT + attributeName,
									dataMap.get(attributeName));
						}
					}
					processChildNodes(node, dataMap, parentValueMap, syncOperation);
				}
			}
		} else {
			for (Document dataMap : dataMapList) {
				if (node != null && node.getReferenceAttributes() != null) {
					for (String attributeName : node.getReferenceAttributes()) {
						parentValueMap.put(node.getNodeName() + QueryConstants.DOT + attributeName,
								dataMap.get(attributeName));
					}
				}
				processChildNodes(node, dataMap, parentValueMap, syncOperation);
			}
		}
	}

	private void processChildNodes(NodeGroup rootNode, Document dataMap, Map<String, Object> parentValueMap,
			SyncOperation syncOperation) throws SQLException {
		logger.debug("Processing child nodes for root node " + rootNode.getNodeName());
		// Set<String> childNodes = getChildNodes(nodeName);
		List<NodeGroup> childNodes = rootNode.getChildGroups();
		List<Document> childDataMapList = null;
		if (childNodes != null && !childNodes.isEmpty()) {
			for (NodeGroup childNode : childNodes) {
				// String childElement = getChildElementName(nodeName,
				// childNode);
				Object childDataObj = dataMap.get(childNode.getNodeName());
				if (childDataObj != null) {
					if (childDataObj instanceof List) {
						childDataMapList = (List<Document>) childDataObj;
					} else {
						Document childDataMap = (Document) childDataObj;
						childDataMapList = Arrays.asList(childDataMap);
					}
					processSyncDocument(childNode, syncOperation, childDataMapList, parentValueMap,
							rootNode.getNodeName());
				}
			}
		}
	}

	private void putValuesInStatement(OraclePreparedStatement pstmt, Set<String> columnAliasSet, NodeGroup node,
			Document dataMap, Map<String, Object> parentValueMap, String parentNodeName) throws SQLException {
		logger.debug("Putting values in PreparedStatement");
		for (String columnAlias : columnAliasSet) {
			ColumnAttrMapper mapper = node.getColumnAttrMappers().get(columnAlias);
			Object mongoValue = null;
			if (mapper.getAttribute() != null) {
				if (mapper.isParentAttribute()) {
					if (!mapper.getAttribute().isIdentifier()) {
						mongoValue = parentValueMap
								.get(parentNodeName + QueryConstants.DOT + mapper.getAttribute().getAttributeName());
					} else {
						mongoValue = parentValueMap.get(SyncAttrs.ID);
					}
				} else if (mapper.isChildAttribute()) {
					mongoValue = getChildValue(mapper.getChildAttributeNode(), dataMap,
							mapper.getAttribute().getAttributeName());
				} else {
					mongoValue = dataMap.get(mapper.getAttribute().getAttributeName());
				}
				if (mongoValue == null && mapper.getAttribute().getDefaultValue() != null) {
					mongoValue = mapper.getAttribute().getDefaultValue();
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

	private String getDeleteQuery(OracleTable table, Set<String>columnAliasSet) {
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

	private String getInsertQuery(NodeGroup node, Set<String> columnAliasSet) {
		InsertQueryBuilder builder = new InsertQueryBuilder();
		String insertQuery = builder.insertAll().intoTables(node.getTableList()).getQuery(columnAliasSet);
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

	/*
	 * private String getChildElementName(String parentNode, String childNode) {
	 * return childNode.substring(parentNode.length() + 1, childNode.length());
	 * }
	 */

	/*
	 * private String replaceLast(String query, String regex, String
	 * replacement) { return query.replaceFirst("(?s)(.*)" + regex, "$1" +
	 * replacement); }
	 */

	/*
	 * private Set<String> getChildNodes(String parentNode) { Set<String>
	 * childNodes = new HashSet<String>(); Set<String> allNodes =
	 * map.getNodeGroupMap().keySet(); for (String node : allNodes) { if
	 * (node.contains(parentNode + QueryConstants.DOT)) { String subNode =
	 * node.substring(0, node.indexOf(QueryConstants.DOT, parentNode.length() +
	 * 1) == -1 ? node.length() : node.indexOf(QueryConstants.DOT,
	 * parentNode.length() + 1)); if (subNode != null && !subNode.isEmpty()) {
	 * childNodes.add(subNode); } } } logger.info("Child Nodes : " +
	 * childNodes); return childNodes; }
	 */
	private Object getChildValue(String childNode, Map<String, Object> dataMap, String attributeName) {
		Object value = null;
		Map<String, Object> childDocument = dataMap;
		StringTokenizer tokenizer = new StringTokenizer(childNode, QueryConstants.DOT);
		tokenizer.nextElement();// Take out parentNodeName
		while (tokenizer.hasMoreTokens()) {
			childDocument = (Document) childDocument.get(tokenizer.nextToken());
			if (childDocument == null) {
				break;
			}
		}
		if (childDocument != null) {
			value = childDocument.get(attributeName);
		}
		return value;
	}

	@SuppressWarnings("rawtypes")
	private Object getColumnValue(Object mongoValue, ColumnAttrMapper mapper, Map<String, Object> parentValueMap)
			throws SQLException {
		Object value = null;
		/*
		 * if (mapper.isParentAttribute()) {
		 * if(!mapper.getAttribute().isIdentifier()){ mongoValue =
		 * parentValueMap.get( mapper.getParentAttributeNode() +
		 * QueryConstants.DOT + mapper.getAttribute().getAttributeName());
		 * }else{ mongoValue = parentValueMap.get(SyncAttrs.ID); } }
		 */
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
			query = getInsertQuery(node, columnAliasSet);
			break;
		case u:
			query = getMergeQuery(table, columnAliasSet, node);
			break;
		case d:
			// TODO : Temp set columnAliasSet
			query = getDeleteQuery(table, columnAliasSet);
			//columnAliasSet.add(table.getKeyColumns().get(0).getColumnAlias());
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
}
