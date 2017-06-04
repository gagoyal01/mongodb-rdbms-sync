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
import com.cisco.app.dbmigrator.migratorapp.core.event.OracleToMongoGridFsEvent;
import com.cisco.app.dbmigrator.migratorapp.core.job.SyncStatus;
import com.cisco.app.dbmigrator.migratorapp.core.map.ColumnAttrMapper;
import com.cisco.app.dbmigrator.migratorapp.core.map.OracleToMongoGridFsMap;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoAttribute;
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

public class SyncMapAndEventGridFsEncoder {
	
	private final Logger logger = Logger.getLogger(SyncMapAndEventGridFsEncoder.class);

	@SuppressWarnings("rawtypes")
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

	@SuppressWarnings("rawtypes")
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

	public Document encodeSyncMap(OracleToMongoGridFsMap map) {
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
			encodeOracleToMongoGridFsMap((OracleToMongoGridFsMap) map, document);
		}
		return document;
	}
	
	public void encodeOracleToMongoGridFsMap(OracleToMongoGridFsMap map, Document document) {
			logger.debug("Encode called for MongoObject");
			if (null != map.getCollectionName() && !map.getCollectionName().isEmpty()) {
				document.append(SyncAttrs.ATTRIBUTE_NAME, map.getCollectionName());
			}
			
			Map<String, ColumnAttrMapper> mapperListMap = map.getMetaAttributes();
			if (mapperListMap != null && !mapperListMap.isEmpty()) {
				List<Document> mapperListDoc = new ArrayList<Document>();
				;
				for (Map.Entry<String, ColumnAttrMapper> mapperEntry : mapperListMap.entrySet()) {
					mapperListDoc.add(encodeColumnAttrMapper(mapperEntry.getValue()));
				}
				document.append(SyncAttrs.META_ATTRIBUTES, mapperListDoc);
			}
			document.append(SyncAttrs.FILE_NAME_COLUMN, encodeColumn(map.getFileNameColumn()));
			document.append(SyncAttrs.INPUT_STREAM_COLUMN, encodeColumn(map.getInputStreamColumn()));
			document.append(SyncAttrs.STREAM_TABLE, encodeTable(map.getStreamTable()));
			if (map.getFilters() != null) {
				document.append(SyncAttrs.FILTERS, encodeFilters(map.getFilters()));
			}
			logger.debug("Encoded Document : " + document);
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
	
	public Document encodeSyncEvent(OracleToMongoGridFsEvent event) {
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
			encodeOracleToMongoEvent((OracleToMongoGridFsEvent) event, document);
		}
		return document;
	}
		
		private void encodeOracleToMongoEvent(OracleToMongoGridFsEvent event, Document document) {
			if (event.getCollectionName() != null && !event.getCollectionName().isEmpty()) {
				document.append(SyncAttrs.COLLECTION_NAME, event.getCollectionName());
			}
			if (event.isSaveNulls()) {
				document.append(SyncAttrs.SAVE_NULLS, event.isSaveNulls());
			}
		}	
	
	public static String getEventJson(OracleToMongoGridFsEvent event) {
		return new SyncMapAndEventGridFsEncoder().encodeSyncEvent(event).toJson();
	}

	public static String getMapJson(OracleToMongoGridFsMap map) {
		return new SyncMapAndEventGridFsEncoder().encodeSyncMap(map).toJson();
	}
}
