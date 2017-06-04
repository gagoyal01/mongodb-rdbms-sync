package com.cisco.app.dbmigrator.migratorapp.logging.codecs;

import java.math.BigDecimal;
import java.sql.RowId;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bson.Document;

import com.cisco.app.dbmigrator.migratorapp.constants.SyncConstants;
import com.cisco.app.dbmigrator.migratorapp.core.event.MongoToOracleEvent;
import com.cisco.app.dbmigrator.migratorapp.core.event.MongoToOracleSyncEvent;
import com.cisco.app.dbmigrator.migratorapp.core.event.OracleParallelReadInfo;
import com.cisco.app.dbmigrator.migratorapp.core.event.OracleToMongoEvent;
import com.cisco.app.dbmigrator.migratorapp.core.event.OracleToMongoGridFsEvent;
import com.cisco.app.dbmigrator.migratorapp.core.event.OracleToMongoSyncEvent;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncEvent;
import com.cisco.app.dbmigrator.migratorapp.core.job.SyncStatus;
import com.cisco.app.dbmigrator.migratorapp.core.map.ColumnAttrMapper;
import com.cisco.app.dbmigrator.migratorapp.core.map.MongoToOracleMap;
import com.cisco.app.dbmigrator.migratorapp.core.map.OracleToMongoGridFsMap;
import com.cisco.app.dbmigrator.migratorapp.core.map.OracleToMongoMap;
import com.cisco.app.dbmigrator.migratorapp.core.map.SyncMap;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoAttribute;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoEntity;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoObject;
import com.cisco.app.dbmigrator.migratorapp.core.meta.oracle.NodeGroup;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.JoinedTable;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.Literal;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.MatchAble;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleTable;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.SqlColumnType;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.LogicalOperation;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.MatchOperation;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.SQLFilters;

import oracle.sql.TIMESTAMP;

/**
 * Encoder to encode all the objects related to Mapping to equivalent Document
 * structure
 * 
 * @author pnilayam
 *
 */
@SuppressWarnings("rawtypes")
public class SyncMapAndEventEncoder {
	private final Logger logger = Logger.getLogger(SyncMapAndEventEncoder.class);

	private Document encodeLiteral(Literal literal) {
		Document doc = new Document();
		doc.append(SyncAttrs.EXPRESSION_TYPE, SyncAttrs.LITERAL);
		doc.append(SyncAttrs.LITERAL_TYPE, literal.getLiteralType());
		if(SqlColumnType.ROWID.equalsIgnoreCase(literal.getLiteralType())){
			doc.append(SyncAttrs.LITERAL_VALUE,new String(((RowId)literal.getLiteralValue()).getBytes()));
		}else if(SqlColumnType.NUMBER.equalsIgnoreCase(literal.getLiteralType())){
			doc.append(SyncAttrs.LITERAL_VALUE,((BigDecimal)literal.getLiteralValue()).doubleValue());
		}
		else if(SqlColumnType.TIMESTAMP.equalsIgnoreCase(literal.getLiteralType())){
			try {
				doc.append(SyncAttrs.LITERAL_VALUE,new Date(((TIMESTAMP)literal.getLiteralValue()).dateValue().getTime()));
			} catch (SQLException e) {
				logger.error("Error while encoding Literal colum : ",e);
			}
		}else{
			doc.append(SyncAttrs.LITERAL_VALUE, literal.getLiteralValue());	
		}		
		return doc;
	}

	private Document encodeMatchable(MatchAble matchAble) {
		Document doc = new Document();
		if (matchAble instanceof OracleColumn) {
			OracleColumn column = (OracleColumn) matchAble;
			Document columnDataDoc = encodeColumn(column);
			doc.append(SyncAttrs.EXPRESSION_TYPE, SyncAttrs.COLUMN);
			doc.append(SyncAttrs.COLUMN_DATA, columnDataDoc);
		} else {
			Literal literal = (Literal) matchAble;
			doc = encodeLiteral(literal);
		}
		return doc;
	}

	private Document encodeMatchOperation(MatchOperation operation) {
		Document doc = new Document();
		doc.append(SyncAttrs.SQL_OPERATION, String.valueOf(operation.getOperator()));
		doc.append(SyncAttrs.LEFT_HAND_EXPRESSION, encodeMatchable(operation.getLeftExpression()));
		if(operation.getRightExpression()!=null){
			doc.append(SyncAttrs.RIGHT_HAND_EXPRESSION, encodeMatchable(operation.getRightExpression()));
		}		
		return doc;
	}

	private List<Document> encodeFilters(SQLFilters filters) {
		List<Document> filterList = new ArrayList<Document>();
		filterList.add(encodeMatchOperation(filters.getMatchOperation()));
		if (filters.getLogicaloperations() != null) {
			for (LogicalOperation logicalOperation : filters.getLogicaloperations()) {
				Document logicalOpDoc = encodeMatchOperation(logicalOperation.getMatchOperation());
				logicalOpDoc.append(SyncAttrs.LOGICAL_OPERATOR, String.valueOf(logicalOperation.getOperator()));
				filterList.add(logicalOpDoc);
			}
		}
		return filterList;
	}

	private Document encodeColumn(OracleColumn column) {
		Document document = new Document();
		document.append(SyncAttrs.COLUMN_NAME, column.getColumnName());
		document.append(SyncAttrs.COLUMN_ALIAS, column.getColumnAlias());
		document.append(SyncAttrs.COLUMN_TYPE, column.getColumnType());
		document.append(SyncAttrs.TABLE_ALIAS, column.getTableAlias());
		if (column.isParentColumn()) {
			document.append(SyncAttrs.IS_PARENT_COLUMN, column.isParentColumn());
		}
		if (column.getPrecision() != 0) {
			document.append(SyncAttrs.PRECISION, column.getPrecision());
		}
		if (column.isNullable()) {
			document.append(SyncAttrs.IS_NULLABLE, column.isNullable());
		}
		return document;
	}

	private Document encodeTable(OracleTable table) {
		Document document = new Document();
		document.append(SyncAttrs.TABLE_NAME, table.getTableName());
		document.append(SyncAttrs.TABLE_ALIAS, table.getTableAlias());
		List<Document> keyColumnDocList = null;
		if (table.getKeyColumns() != null && !table.getKeyColumns().isEmpty()) {
			keyColumnDocList = new ArrayList<Document>(table.getKeyColumns().size());
			for (OracleColumn column : table.getKeyColumns()) {
				keyColumnDocList.add(encodeColumn(column));
			}
			document.append(SyncAttrs.KEY_COLUMNS, keyColumnDocList);
		}

		if (table.getJoinedTables() != null && !table.getJoinedTables().isEmpty()) {
			List<Document> joinedTableDocList = new ArrayList<Document>();
			for (JoinedTable joinedTable : table.getJoinedTables()) {
				joinedTableDocList.add(encodeJoinedTable(joinedTable));
			}
			document.append(SyncAttrs.JOINED_TABLES, joinedTableDocList);
		}
		return document;
	}

	private Document encodeJoinedTable(JoinedTable joinedTable) {
		Document document = new Document();
		document.append(SyncAttrs.JOIN_TYPE, String.valueOf(joinedTable.getJoinType()));
		document.append(SyncAttrs.FILTERS, encodeFilters(joinedTable.getFilters()));
		OracleTable innerJoinedTable = joinedTable.getTable();
		document.append(SyncAttrs.TABLE_NAME, innerJoinedTable.getTableName());
		document.append(SyncAttrs.TABLE_ALIAS, innerJoinedTable.getTableAlias());
		if (innerJoinedTable.getJoinedTables() != null) {
			List<Document> nestedJoinedTableList = new ArrayList<Document>();
			for (JoinedTable nestedJoinedTable : innerJoinedTable.getJoinedTables()) {
				nestedJoinedTableList.add(encodeJoinedTable(nestedJoinedTable));
			}
			document.append(SyncAttrs.JOINED_TABLES, nestedJoinedTableList);
		}
		return document;
	}

	private Document encodeMongoEntity(MongoEntity entity) {
		Document document = new Document();
		if (entity instanceof MongoAttribute) {
			document = encodeMongoAttribute((MongoAttribute) entity);
		} else if (entity instanceof MongoObject) {
			document = encodeMongoObject((MongoObject) entity);
		}
		return document;
	}

	private Document encodeMongoAttribute(MongoAttribute mongoAttribute) {
		logger.debug("Encode called for MongoAttribute");
		Document document = new Document();
		document.append(SyncAttrs.ATTRIBUTE_NAME, mongoAttribute.getAttributeName());
		document.append(SyncAttrs.ATTRIBUTE_TYPE, String.valueOf(mongoAttribute.getAttributeType()));
		document.append(SyncAttrs.IS_IDENTIFIER, mongoAttribute.isIdentifier());
		if (mongoAttribute.getMappedOracleColumn() != null) {
			document.append(SyncAttrs.COLUMN_DATA, encodeColumn(mongoAttribute.getMappedOracleColumn()));
		}
		document.append(SyncAttrs.DEFAULT_VALUE, mongoAttribute.getDefaultValue());
		logger.debug("Encoded Document : " + document);
		return document;
	}

	private Document encodeMongoObject(MongoObject mongoObject) {
		logger.debug("Encode called for MongoObject");
		Document document = new Document();
		if (null != mongoObject.getCollectionName() && !mongoObject.getCollectionName().isEmpty()) {
			document.append(SyncAttrs.ATTRIBUTE_NAME, mongoObject.getCollectionName());
		}
		document.append(SyncAttrs.ATTRIBUTE_TYPE, mongoObject.getCollectionType());
		List<Document> identifierDocList = new ArrayList<Document>();
		if (null != mongoObject.getIdentifierList() && !mongoObject.getIdentifierList().isEmpty()) {
			for (MongoEntity identifierEntity : mongoObject.getIdentifierList()) {
				Document identifierDoc = encodeMongoEntity(identifierEntity);
				identifierDocList.add(identifierDoc);
			}
			document.append(SyncAttrs.IDENTIFIERS, identifierDocList);
		}
		List<Document> entityDocList = new ArrayList<Document>();
		for (MongoEntity entity : mongoObject.getAttributes()) {
			Document entityDoc = encodeMongoEntity(entity);
			entityDocList.add(entityDoc);
		}
		document.append(SyncAttrs.ATTRIBUTES, entityDocList);
		List<Document> sourceTablesDocList = new ArrayList<Document>();
		for (OracleTable table : mongoObject.getSourceTables()) {
			sourceTablesDocList.add(encodeTable(table));
		}
		document.append(SyncAttrs.SOURCE_TABLES, sourceTablesDocList);
		if (mongoObject.getFilters() != null) {
			document.append(SyncAttrs.FILTERS, encodeFilters(mongoObject.getFilters()));
		}
		List<Document> refColumnsList = null;
		if (null != mongoObject.getReferencedColumns() && !mongoObject.getReferencedColumns().isEmpty()) {
			refColumnsList = new ArrayList<Document>();
			for (OracleColumn refCol : mongoObject.getReferencedColumns()) {
				Document refColDoc = encodeColumn(refCol);
				refColumnsList.add(refColDoc);
			}
			document.append(SyncAttrs.REFERENCED_COLUMNS, refColumnsList);
		}
		logger.debug("Encoded Document : " + document);
		return document;
	}

	private Document encodeOracleParallelReadInfo(OracleParallelReadInfo parallelReadInfo) {
		Document document = null;
		if (parallelReadInfo != null) {
			document = new Document();
			document.append(SyncAttrs.PROCESS_PARALLEL, parallelReadInfo.isProcessParallel());
			document.append(SyncAttrs.NUM_OF_BUCKETS, parallelReadInfo.getNumOfBuckets());
			if (parallelReadInfo.getRangeColumn() != null) {
				document.append(SyncAttrs.COLUMN_DATA, encodeColumn(parallelReadInfo.getRangeColumn()));
			}
		}
		return document;
	}

	private Document encodeColumnAttrMapper(ColumnAttrMapper mapper) {
		Document document = new Document();
		if (mapper.getColumn() != null) {
			document.append(SyncAttrs.COLUMN_DATA, encodeColumn(mapper.getColumn()));
		}
		if (mapper.isParentColumn()) {
			document.append(SyncAttrs.IS_PARENT_COLUMN, mapper.isParentColumn());
		}
		if (mapper.isSeqGenerated()) {
			document.append(SyncAttrs.IS_SEQ_GENERATED, mapper.isSeqGenerated());
			document.append(SyncAttrs.SEQ_NAME, mapper.getSeqName());
		}
		if (mapper.getIgnoreList() != null && !mapper.getIgnoreList().isEmpty()) {
			document.append(SyncAttrs.IGNORE_LIST, mapper.getIgnoreList());
		}
		if (mapper.getLiteralValueForColumn() != null) {
			document.append(SyncAttrs.LITERAL_VALUE_FOR_COLUMN, encodeLiteral(mapper.getLiteralValueForColumn()));
		}
		if (mapper.getAttribute() != null) {
			document.append(SyncAttrs.ATTRIBUTE, encodeMongoAttribute(mapper.getAttribute()));
		}
		if (mapper.isParentAttribute()) {
			document.append(SyncAttrs.IS_PARENT_ATTRIBUTE, mapper.isParentAttribute());
			document.append(SyncAttrs.PARENT_ATTRIBUTE_NODE, mapper.getParentAttributeNode());
		}
		if (mapper.isChildAttribute()) {
			document.append(SyncAttrs.IS_CHILD_ATTRIBUTE, mapper.isChildAttribute());
			document.append(SyncAttrs.CHILD_ATTRIBUTE_NODE, mapper.getChildAttributeNode());
		}
		if (mapper.getReplacementMap() != null && !mapper.getReplacementMap().isEmpty()) {
			document.append(SyncAttrs.REPLACEMENT_MAP, mapper.getReplacementMap());
		}
		return document;
	}

	public Document encodeSyncMap(SyncMap map) {
		Document document = new Document();
		if (map != null) {
			if (map.getMapId() != null) {
				document.append(SyncAttrs.ID, map.getMapId());
			}
			if (map.getMapName() != null && !map.getMapName().isEmpty()) {
				document.append(SyncAttrs.MAP_NAME, map.getMapName());
			}
			if (map.getComments() != null && !map.getComments().isEmpty()) {
				document.append(SyncAttrs.COMMENTS, map.getComments());
			}
			if (map.getCreatedBy() != null && !map.getCreatedBy().isEmpty()) {
				document.append(SyncAttrs.CREATED_BY, map.getCreatedBy());
			}
			if (map.getCreatedOn() != null) {
				document.append(SyncAttrs.CREATED_ON, map.getCreatedOn());
			}
			if (map.getApprovedBy() != null && !map.getApprovedBy().isEmpty()) {
				document.append(SyncAttrs.APPROVED_BY, map.getApprovedBy());
			}
			if (map.getApprovedOn() != null) {
				document.append(SyncAttrs.APPROVED_ON, map.getApprovedOn());
			}
			if (map.getSourceDbName() != null && !map.getSourceDbName().isEmpty()) {
				document.append(SyncAttrs.SOURCE_DB_NAME, map.getSourceDbName());
			}
			if (map.getSourceUserName() != null && !map.getSourceUserName().isEmpty()) {
				document.append(SyncAttrs.SOURCE_USER_NAME, map.getSourceUserName());
			}
			if (map.getTargetDbName() != null && !map.getTargetDbName().isEmpty()) {
				document.append(SyncAttrs.TARGET_DB_NAME, map.getTargetDbName());
			}
			if (map.getTargetUserName() != null && !map.getTargetUserName().isEmpty()) {
				document.append(SyncAttrs.TARGET_USER_NAME, map.getTargetUserName());
			}
			if (map.getMapType() != null) {
				document.append(SyncAttrs.MAP_TYPE, String.valueOf(map.getMapType()));
				switch (map.getMapType()) {
				case OrclToMongo:
					encodeOracleToMongoMap((OracleToMongoMap) map, document);
					break;
				case MongoToOrcl:
					encodeMongoToOracleMap((MongoToOracleMap) map, document);
					break;
				case OrclToMongoGridFs:
					encodeOracleToMongoGridFsMap((OracleToMongoGridFsMap) map, document);
					break;
				default:
					break;
				}
			}
		}
		return document;
	}

	public void encodeOracleToMongoMap(OracleToMongoMap map, Document document) {
		if (map.getMapObject() != null) {
			document.append(SyncAttrs.MAP_OBJECT, encodeMongoObject(map.getMapObject()));
		}
	}

	private Document encodeNodeGroup(NodeGroup nodeGroup) {
		Document nodeGrpDoc = null;
		if (nodeGroup != null) {
			nodeGrpDoc = new Document();
			nodeGrpDoc.append(SyncAttrs.NODE_NAME, nodeGroup.getNodeName());

			nodeGrpDoc.append(SyncAttrs.REF_ATTRS, nodeGroup.getReferenceAttributes());

			List<OracleTable> tableList = nodeGroup.getTableList();
			List<Document> tableListDoc = null;
			if (tableList != null) {
				tableListDoc = new ArrayList<Document>();
				for (OracleTable table : tableList) {
					tableListDoc.add(encodeTable(table));
				}
				nodeGrpDoc.append(SyncAttrs.TABLE_LIST, tableListDoc);
			}

			Map<String, ColumnAttrMapper> mapperListMap = nodeGroup.getColumnAttrMappers();
			if (mapperListMap != null && !mapperListMap.isEmpty()) {
				List<Document> mapperListDoc = new ArrayList<Document>();
				;
				for (Map.Entry<String, ColumnAttrMapper> mapperEntry : mapperListMap.entrySet()) {
					mapperListDoc.add(encodeColumnAttrMapper(mapperEntry.getValue()));
				}
				nodeGrpDoc.append(SyncAttrs.COLUMN_ATTR_MAP, mapperListDoc);
			}

			if (nodeGroup.getChildGroups() != null && !nodeGroup.getChildGroups().isEmpty()) {
				List<Document> childNodeDocList = new ArrayList<Document>();
				for (NodeGroup childNode : nodeGroup.getChildGroups()) {
					childNodeDocList.add(encodeNodeGroup(childNode));
				}
				nodeGrpDoc.append(SyncAttrs.CHILD_NODES, childNodeDocList);
			}
		}
		return nodeGrpDoc;
	}

	public void encodeMongoToOracleMap(MongoToOracleMap map, Document mapDocument) {
		if (map.getCollectionName() != null && !map.getCollectionName().isEmpty()) {
			mapDocument.append(SyncAttrs.COLLECTION_NAME, map.getCollectionName());
		}
		
		if (map.getRootNode() != null && !map.getRootNode().isEmpty()) {
			List<Document> nodeTableGrpListDoc= new ArrayList<Document>();
			for (NodeGroup rootNode : map.getRootNode()) {
					nodeTableGrpListDoc.add(encodeNodeGroup(rootNode));
			}
			mapDocument.append(SyncAttrs.NODE_TABLE_GROUP, nodeTableGrpListDoc);
		}	
	}

	public Document encodeSyncEvent(SyncEvent event) {
		Document document = null;
		if (event != null) {
			document = new Document();
			if (event.getEventId() != null) {
				document.append(SyncAttrs.ID, event.getEventId());
			}
			if (event.getEventName() != null && !event.getEventName().isEmpty()) {
				document.append(SyncAttrs.EVENT_NAME, event.getEventName());
			}
			if (event.getEventType() != null) {
				document.append(SyncAttrs.EVENT_TYPE, String.valueOf(event.getEventType()));
			}
			if (event.getParentEventId() != null) {
				document.append(SyncAttrs.PARENT_EVENT_ID, event.getParentEventId());
			} else {
				document.append(SyncAttrs.PARENT_EVENT_ID, event.getEventId());
			}
			if (event.getMapId() != null) {
				document.append(SyncAttrs.MAP_ID, event.getMapId());
				document.append(SyncAttrs.MAP_NAME, event.getMapName());
			}
			if (event.getComments() != null && !event.getComments().isEmpty()) {
				document.append(SyncAttrs.COMMENTS, event.getComments());
			}
			if (event.getCreatedBy() != null && !event.getCreatedBy().isEmpty()) {
				document.append(SyncAttrs.CREATED_BY, event.getCreatedBy());
			}
			if (event.getCreatedOn() != null) {
				document.append(SyncAttrs.CREATED_ON, event.getCreatedOn());
			} else {
				document.append(SyncAttrs.CREATED_ON, new Date());
			}
			if (event.getApprovedBy() != null && !event.getApprovedBy().isEmpty()) {
				document.append(SyncAttrs.APPROVED_BY, event.getApprovedBy());
			}
			if (event.getApprovedOn() != null) {
				document.append(SyncAttrs.APPROVED_ON, event.getApprovedOn());
			}
			if (event.getNotifIds() != null) {
				document.append(SyncAttrs.NOTIF_ALIAS, event.getNotifIds());
			}
			if (event.isRetry()) {
				document.append(SyncAttrs.IS_RETRY, event.isRetry());
			}
			if (event.getStatus() != null && !event.getStatus().isEmpty()) {
				document.append(SyncAttrs.STATUS, event.getStatus());
			} else {
				document.append(SyncAttrs.STATUS, SyncStatus.PENDING);
			}
			if (event.getBatchSize() == 0) {
				document.append(SyncAttrs.BATCH_SIZE, SyncConstants.DEFAULT_BATCH_SIZE);
			} else {
				document.append(SyncAttrs.BATCH_SIZE, event.getBatchSize());
			}
			switch (event.getEventType()) {
			case OrclToMongo: {
				encodeOracleToMongoEvent((OracleToMongoEvent) event, document);
				break;
			}
			case MongoToOrcl: {
				encodeMongoToOracleEvent((MongoToOracleEvent) event, document);
				break;
			}
			case MongoToOrclSync: {
				encodeMongoToOracleSyncEvent((MongoToOracleSyncEvent) event, document);
				break;
			}
			case OrclToMongoSync: {
				encodeOracleToMongoSyncEvent((OracleToMongoSyncEvent) event, document);
				break;
			}
			case OrclToMongoGridFs: {
				encodeOracleToMongoGridFsEvent((OracleToMongoGridFsEvent) event,document);
				break;
			}
			default: {
			}
			}
		}
		return document;
	}

	private void encodeOracleToMongoSyncEvent(OracleToMongoSyncEvent event, Document document) {
		if (event.getCollectionName() != null && !event.getCollectionName().isEmpty()) {
			document.append(SyncAttrs.COLLECTION_NAME, event.getCollectionName());
		}
		if (event.isSaveNulls()) {
			document.append(SyncAttrs.SAVE_NULLS, event.isSaveNulls());
		}
		if (event.getKeyAttributes() != null && !event.getKeyAttributes().isEmpty()) {
			List<Document> keyAttributeDocList = new ArrayList<Document>();
			for (MongoAttribute keyAttr : event.getKeyAttributes()) {
				keyAttributeDocList.add(encodeMongoAttribute(keyAttr));
			}
			document.append(SyncAttrs.KEY_ATTRIBUTES, keyAttributeDocList);
		}
		if(event.isPollBased()){
			document.append(SyncAttrs.POLL_BASED, true);
			Document pollInfo = new Document();
			pollInfo.append(SyncAttrs.INTERVAL, event.getPollInfo().getInterval());
			pollInfo.append(SyncAttrs.POLLING_COLUMN, encodeColumn(event.getPollInfo().getPollingColumn()));
			pollInfo.append(SyncAttrs.LAST_READ_TIME, event.getPollInfo().getLastReadTime());
			pollInfo.append(SyncAttrs.TIME_UNIT, event.getPollInfo().getTimeUnit());
			document.append(SyncAttrs.POLL_INFO, pollInfo);
		}
	}

	private void encodeMongoToOracleEvent(MongoToOracleEvent event, Document document) {
		if (event.getCollectionName() != null && !event.getCollectionName().isEmpty()) {
			document.append(SyncAttrs.COLLECTION_NAME, event.getCollectionName());
		}
	}

	private void encodeMongoToOracleSyncEvent(MongoToOracleSyncEvent event, Document document) {
		if (event.getLastReadTime() != null) {
			document.append(SyncAttrs.LAST_READ_TIME, event.getLastReadTime());
		}
		if (event.getCollectionName() != null && !event.getCollectionName().isEmpty()) {
			document.append(SyncAttrs.COLLECTION_NAME, event.getCollectionName());
		}
		if (event.isRestrictedSyncEnabled()) {
			document.append(SyncAttrs.RESTRCTITED_SYNC_ENABLED, event.isRestrictedSyncEnabled());
		}
	}

	private void encodeOracleToMongoEvent(OracleToMongoEvent event, Document document) {
		if (event.getCollectionName() != null && !event.getCollectionName().isEmpty()) {
			document.append(SyncAttrs.COLLECTION_NAME, event.getCollectionName());
		}
		if (event.getRangeFilter() != null) {
			document.append(SyncAttrs.RANGE_FILTER, encodeFilters(event.getRangeFilter()));
		}
		if (event.isSaveNulls()) {
			document.append(SyncAttrs.SAVE_NULLS, event.isSaveNulls());
		}
		if (event.getParallelReadInfo() != null) {
			document.append(SyncAttrs.PARALLEL_PROCESSING_INFO,
					encodeOracleParallelReadInfo(event.getParallelReadInfo()));
		}
	}

	public static String getEventJson(SyncEvent event) {
		return new SyncMapAndEventEncoder().encodeSyncEvent(event).toJson();
	}

	public static String getMapJson(SyncMap map) {
		return new SyncMapAndEventEncoder().encodeSyncMap(map).toJson();
	}
	
	public void encodeOracleToMongoGridFsMap(OracleToMongoGridFsMap map, Document document) {
		logger.debug("Encode called for MongoObject");
		if (null != map.getCollectionName() && !map.getCollectionName().isEmpty()) {
			document.append(SyncAttrs.ATTRIBUTE_NAME, map.getCollectionName());
		}
		
		Map<String, ColumnAttrMapper> mapperListMap = map.getMetaAttributes();
		if (mapperListMap != null && !mapperListMap.isEmpty()) {
			List<Document> mapperListDoc = new ArrayList<Document>();
			for (Map.Entry<String, ColumnAttrMapper> mapperEntry : mapperListMap.entrySet()) {
				mapperListDoc.add(encodeColumnAttrMapper(mapperEntry.getValue()));
			}
			document.append(SyncAttrs.META_ATTRIBUTES, new Document(SyncAttrs.ATTRIBUTES, mapperListDoc));
		}
		
		document.append(SyncAttrs.FILE_NAME_COLUMN, encodeColumn(map.getFileNameColumn()));
		document.append(SyncAttrs.INPUT_STREAM_COLUMN, encodeColumn(map.getInputStreamColumn()));
		if(map.getStreamTable() != null)
			document.append(SyncAttrs.STREAM_TABLE, encodeTable(map.getStreamTable()));
		if (map.getFilters() != null) {
			document.append(SyncAttrs.FILTERS, encodeFilters(map.getFilters()));
		}
		logger.debug("Encoded Document : " + document);
	}
	
	private void encodeOracleToMongoGridFsEvent(OracleToMongoGridFsEvent event, Document document) {
		if (event.getCollectionName() != null && !event.getCollectionName().isEmpty()) {
			document.append(SyncAttrs.COLLECTION_NAME, event.getCollectionName());
		}
		if (event.getRangeFilter() != null) {
			document.append(SyncAttrs.RANGE_FILTER, encodeFilters(event.getRangeFilter()));
		}
		if (event.isSaveNulls()) {
			document.append(SyncAttrs.SAVE_NULLS, event.isSaveNulls());
		}
	}
}
