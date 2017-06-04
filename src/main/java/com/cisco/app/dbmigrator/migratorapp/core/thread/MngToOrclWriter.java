package com.cisco.app.dbmigrator.migratorapp.core.thread;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;
import org.bson.Document;

import com.cisco.app.dbmigrator.migratorapp.core.map.ColumnAttrMapper;
import com.cisco.app.dbmigrator.migratorapp.core.map.MongoToOracleMap;
import com.cisco.app.dbmigrator.migratorapp.core.meta.oracle.NodeGroup;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.Literal;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.QueryConstants;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.SqlLiteralFactory;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.queries.InsertQueryBuilder;
import com.cisco.app.dbmigrator.migratorapp.utilities.cache.DBCacheManager;
import oracle.jdbc.OraclePreparedStatement;

@SuppressWarnings("unchecked")
public class MngToOrclWriter implements Runnable {
	private BlockingQueue<Document> dataBuffer;
	private MongoToOracleMap map;
	private Connection connection = null;
	private final Logger logger = Logger.getLogger(getClass());
	private Map<String,QueryHolder> statementMap;

	public MngToOrclWriter(BlockingQueue<Document> dataBuffer, MongoToOracleMap map) {
		super();
		this.dataBuffer = dataBuffer;
		this.map = map;
	}

	@Override
	public void run() {
		// TODO : Check whether PreparedStatement Objects can be resued.
		logger.info("Writer started");
		statementMap=new HashMap<String,QueryHolder>();
		Document rootDoc;
		List<OraclePreparedStatement> executableStatements;
		Map<String, Object> parentValueMap = null;
		try {
			connection = DBCacheManager.INSTANCE
					.getCachedOracleConnection(map.getTargetDbName(), map.getTargetUserName());
			//map.setNodeGroupMap(new TreeMap<String, NodeGroup>(map.getNodeGroupMap()));
			while (true) {
				try {
					executableStatements = new ArrayList<OraclePreparedStatement>();
					parentValueMap = new HashMap<String, Object>();
					rootDoc = dataBuffer.take();
					processDocument(rootDoc, null, map.getRootNode().get(0), parentValueMap, executableStatements);
					logger.info("Statement prepared");
					connection.setAutoCommit(false);
					for (OraclePreparedStatement pstmt : executableStatements) {
						pstmt.executeBatch();
						logger.info("Statement executed");
						pstmt.clearBatch();
					}
					connection.commit();
					logger.info("Statement committed");
				} catch (InterruptedException e) {
					try {
						connection.rollback();
					} catch (SQLException e1) {
						logger.error("ROlling back",e1);
						//TODO : use error logging for these events						
					}
				} catch (SQLException e) {
					try {
						connection.rollback();
					} catch (SQLException e1) {
						logger.error("ROlling back",e1);
						//TODO : use error logging for these events						
					}
				} finally{
					
				}
			}
		} catch (Exception e2) {
			//TODO : use error logging for these events
			logger.error("Unexpected Excpetion", e2);
		} finally {
			if(connection!=null){
				try {
					connection.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	private void processDocument(Document dataMap, Document parentDoc, NodeGroup node,
			Map<String, Object> parentValueMap, List<OraclePreparedStatement> executableStatements)
					throws SQLException {
		logger.info("processing node " + node.getNodeName());
		//NodeGroup node = map.getNodeGroupMap().get(nodeName);
		if (node.getReferenceAttributes() != null) {
			for (String attributeName : node.getReferenceAttributes()) {
				parentValueMap.put(node.getNodeName() + QueryConstants.DOT + attributeName, dataMap.get(attributeName));
			}
		}
		if (node.getColumnAttrMappers() != null && !node.getColumnAttrMappers().isEmpty()) {
			QueryHolder queryHolder =statementMap.get(node.getNodeName());
			if(queryHolder==null){
				queryHolder =getQueryHolder(node);
				statementMap.put(node.getNodeName(), queryHolder);
			}
/*			String insertQuery = node.getInsertQuery();
			for (Map.Entry<String, ColumnAttrMapper> mapper : node.getColumnAttrMappers().entrySet()) {
				if (mapper.getValue().isSeqGenerated()) {
					insertQuery = insertQuery.replace(QueryConstants.COLON + mapper.getKey(),
							mapper.getValue().getSeqName() + QueryConstants.NEXTVAL);
					node.getColumnAliasSet().remove(mapper.getKey());
				}
			}*/

			OraclePreparedStatement pstmt = queryHolder.getPstmt();
			executableStatements.add(pstmt);
			for (String columnAlias : queryHolder.getColumnAliasSet()) {
				ColumnAttrMapper mapper = node.getColumnAttrMappers().get(columnAlias);
				Object mongoValue = null;
				if (mapper.getAttribute() != null) {
					mongoValue = dataMap.get(mapper.getAttribute().getAttributeName());
				}
				Object columnValue = getColumnValue(mongoValue, mapper, parentValueMap);
				pstmt.setObjectAtName(columnAlias, columnValue);
			}
			pstmt.addBatch();
		}

		//Set<String> childNodes = getChildNodes(nodeName);
		List<NodeGroup> childNodes = node.getChildGroups();
		if (childNodes != null && !childNodes.isEmpty()) {
			for (NodeGroup childNode : childNodes) {
				Object childDataObj = dataMap.get(childNode.getNodeName());
				if(childDataObj!=null){
					if (childDataObj instanceof List) {
						List<Document> childDataMapList = (List<Document>) childDataObj;
						processDocument(childDataMapList, parentDoc, childNode, parentValueMap, executableStatements);
					} else {
						Document childDataMap = (Document) childDataObj;
						processDocument(childDataMap, parentDoc, childNode, parentValueMap, executableStatements);
					}	
				}				
			}
		}
	}

	private void processDocument(List<Document> dataMapList, Document parentDoc, NodeGroup node,
			Map<String, Object> parentValueMap, List<OraclePreparedStatement> executableStatements)
					throws SQLException {
		//NodeGroup node = map.getNodeGroupMap().get(nodeName);

		if (node.getColumnAttrMappers() != null && !node.getColumnAttrMappers().isEmpty()) {
			QueryHolder queryHolder =statementMap.get(node.getNodeName());
			if(queryHolder==null){
				queryHolder =getQueryHolder(node);
				statementMap.put(node.getNodeName(), queryHolder);
			}

			//Set<String> childNodes = getChildNodes(node.getNodeName());
			OraclePreparedStatement pstmt = queryHolder.getPstmt();
			executableStatements.add(pstmt);
			for (Document dataMap : dataMapList) {

				if (node.getReferenceAttributes() != null) {
					for (String attributeName : node.getReferenceAttributes()) {
						parentValueMap.put(node.getNodeName() + QueryConstants.DOT + attributeName, dataMap.get(attributeName));
					}
				}

				for (String columnAlias : queryHolder.getColumnAliasSet()) {
					ColumnAttrMapper mapper = node.getColumnAttrMappers().get(columnAlias);
					// List
					Object mongoValue = null;
					if (mapper.getAttribute() != null) {
						mongoValue = dataMap.get(mapper.getAttribute().getAttributeName());
					}
					Object columnValue = getColumnValue(mongoValue, mapper, parentValueMap);
					pstmt.setObjectAtName(columnAlias, columnValue);
				}
				pstmt.addBatch();
				List<NodeGroup> childNodes = node.getChildGroups();
				if (childNodes != null) {
					for (NodeGroup childNode : childNodes) {
						Object childDataObj = dataMap.get(childNode.getNodeName());
						if (childDataObj instanceof List) {
							List<Document> childDataMapList = (List<Document>) childDataObj;
							processDocument(childDataMapList, parentDoc, childNode, parentValueMap,
									executableStatements);
						} else {
							Document childDataMap = (Document) childDataObj;
							processDocument(childDataMap, parentDoc, childNode, parentValueMap, executableStatements);
						}
					}
				}
			}
		} else {
			//Set<String> childNodes = getChildNodes(nodeName);
			List<NodeGroup> childNodes = node.getChildGroups();
			if (childNodes != null) {
				for (NodeGroup childNode : childNodes) {
					for (Document dataMap : dataMapList) {
						Object childDataObj = dataMap.get(childNode.getNodeName());
						if (childDataObj instanceof List) {
							List<Document> childDataMapList = (List<Document>) childDataObj;
							processDocument(childDataMapList, parentDoc, childNode, parentValueMap,
									executableStatements);
						} else {
							Document childDataMap = (Document) childDataObj;
							processDocument(childDataMap, parentDoc, childNode, parentValueMap, executableStatements);
						}
					}
				}
			}
		}
	}

	/*private String getChildElementName(String parentNode, String childNode) {
		return childNode.substring(parentNode.length() + 1, childNode.length());
	}*/

	/*private Set<String> getChildNodes(String parentNode) {
		Set<String> childNodes = new HashSet<String>();
		Set<String> allNodes = map.getNodeGroupMap().keySet();
		for (String node : allNodes) {
			if (node.contains(parentNode + QueryConstants.DOT)) {
				String subNode = node.substring(0, node.indexOf(QueryConstants.DOT, parentNode.length() + 1) == -1
						? node.length() : node.indexOf(QueryConstants.DOT, parentNode.length() + 1));
				if (subNode != null && !subNode.isEmpty()) {
					childNodes.add(subNode);
				}
			}
		}
		return childNodes;
	}*/
	
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

	@SuppressWarnings("rawtypes")
	private Object getColumnValue(Object mongoValue, ColumnAttrMapper mapper, Map<String, Object> parentValueMap) {
		Object value = null;
		if (mapper.isParentAttribute()) {
			mongoValue = parentValueMap.get(
					mapper.getParentAttributeNode() + QueryConstants.DOT + mapper.getAttribute().getAttributeName());
		}
		if (mongoValue != null) {
			if (mapper.getReplacementMap() != null && mapper.getReplacementMap().containsKey(mongoValue)) {
				mongoValue = mapper.getReplacementMap().get(mongoValue);
			}
			value = SqlLiteralFactory.getLiteral(mongoValue, mapper.getColumn().getColumnType()).getLiteralValue();
		} else {
			Literal defaultValue = mapper.getLiteralValueForColumn();
			if (defaultValue != null) {
				value = defaultValue.getLiteralValue();
			}
		}
		return value;
	}
	
	private QueryHolder getQueryHolder(NodeGroup node) throws SQLException{
		QueryHolder queryHolder = new QueryHolder();
		Set<String> columnAliasSet = new HashSet<String>();
		String query=null;
		query = getInsertQuery(node, columnAliasSet);
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
