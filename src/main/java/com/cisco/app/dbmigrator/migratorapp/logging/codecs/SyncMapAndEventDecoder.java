package com.cisco.app.dbmigrator.migratorapp.logging.codecs;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.constants.SyncConstants;
import com.cisco.app.dbmigrator.migratorapp.core.event.EventType;
import com.cisco.app.dbmigrator.migratorapp.core.event.MongoToOracleEvent;
import com.cisco.app.dbmigrator.migratorapp.core.event.MongoToOracleSyncEvent;
import com.cisco.app.dbmigrator.migratorapp.core.event.OracleParallelReadInfo;
import com.cisco.app.dbmigrator.migratorapp.core.event.OracleToMongoEvent;
import com.cisco.app.dbmigrator.migratorapp.core.event.OracleToMongoGridFsEvent;
import com.cisco.app.dbmigrator.migratorapp.core.event.OracleToMongoSyncEvent;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncEvent;
import com.cisco.app.dbmigrator.migratorapp.core.event.OracleToMongoSyncEvent.O2MSyncPollInfo;
import com.cisco.app.dbmigrator.migratorapp.core.map.ColumnAttrMapper;
import com.cisco.app.dbmigrator.migratorapp.core.map.MapType;
import com.cisco.app.dbmigrator.migratorapp.core.map.MongoToOracleMap;
import com.cisco.app.dbmigrator.migratorapp.core.map.OracleToMongoGridFsMap;
import com.cisco.app.dbmigrator.migratorapp.core.map.OracleToMongoMap;
import com.cisco.app.dbmigrator.migratorapp.core.map.SyncMap;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoAttribute;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoAttributeType;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoEntity;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoObject;
import com.cisco.app.dbmigrator.migratorapp.core.meta.oracle.NodeGroup;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.JoinType;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.JoinedTable;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.Literal;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.MatchAble;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleTable;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.SqlLiteralFactory;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.MatchOperation;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.OperationsFactory;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.SQLFilters;

/**
 * Class with methods to create Java object event equivalent to jsonString or
 * Document structure
 * 
 * @author pnilayam
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class SyncMapAndEventDecoder {
	private Map<String, OracleTable> referredTables = new HashMap<String, OracleTable>();
	private static final Logger logger = Logger.getLogger(SyncMapAndEventDecoder.class);
	private int tableRank=0;
	private Literal decodeLiteral(Document document) {
		String expressionType = document.getString(SyncAttrs.EXPRESSION_TYPE);
		Literal literal = null;
		if (expressionType != null && !expressionType.isEmpty()) {
			Object literalValue = document.get(SyncAttrs.LITERAL_VALUE);
			String literalType = document.getString(SyncAttrs.LITERAL_TYPE);
			literal = SqlLiteralFactory.getLiteral(literalValue, literalType);
		}
		return literal;
	}

	private MatchAble decodeExpression(Document document) {
		MatchAble matchAble = null;
		String expressionType = document.getString(SyncAttrs.EXPRESSION_TYPE);
		if (expressionType != null && !expressionType.isEmpty()) {
			if (SyncAttrs.COLUMN.equalsIgnoreCase(expressionType)) {
				Document columDataDoc = (Document) document.get(SyncAttrs.COLUMN_DATA);
				matchAble = decodeColumn(columDataDoc);
			} else {
				matchAble = decodeLiteral(document);
			}
		}
		return matchAble;
	}

	private MatchOperation decodeMatchOperation(Document document) {
		String sqlOperation = document.getString(SyncAttrs.SQL_OPERATION);
		MatchAble leftHandExpr = decodeExpression((Document) document.get(SyncAttrs.LEFT_HAND_EXPRESSION));
		MatchAble rightHandExpr =null;
		if(document.get(SyncAttrs.RIGHT_HAND_EXPRESSION)!=null){
			rightHandExpr = decodeExpression((Document) document.get(SyncAttrs.RIGHT_HAND_EXPRESSION));
		}		
		return OperationsFactory.getMatchExpression(leftHandExpr, rightHandExpr, sqlOperation);
	}

	private SQLFilters decodeFilter(List<Document> filterList) {
		logger.debug("decodeFilter called " + filterList);
		SQLFilters filters = new SQLFilters();
		for (Document doc : filterList) {
			String logicalOperator = doc.getString(SyncAttrs.LOGICAL_OPERATOR);
			MatchOperation operation = decodeMatchOperation(doc);
			filters.addOperation(operation, logicalOperator);
		}
		return filters;
	}

	private OracleColumn decodeColumn(Document document) {
		logger.debug("Decode called for decodeColumn" + document);
		OracleColumn column = new OracleColumn();
		column.setColumnName(document.getString(SyncAttrs.COLUMN_NAME));
		column.setColumnAlias(document.getString(SyncAttrs.COLUMN_ALIAS));
		column.setColumnType(document.getString(SyncAttrs.COLUMN_TYPE));
		column.setParentColumn(document.getBoolean(SyncAttrs.IS_PARENT_COLUMN, false));
		column.setTableAlias(document.getString(SyncAttrs.TABLE_ALIAS));
		column.setNullable(document.getBoolean(SyncAttrs.IS_NULLABLE, false));
		OracleTable table = referredTables.get(column.getTableAlias());
		if (table != null) {
			table.addColumn(column);
		}
		return column;
	}

	private OracleTable decodeTable(Document document) {
		logger.debug("Decode called for decodeTable" + document);
		OracleTable table = new OracleTable();
		table.setRank(++tableRank);
		table.setTableName(document.getString(SyncAttrs.TABLE_NAME));
		table.setTableAlias(document.getString(SyncAttrs.TABLE_ALIAS));
		// TODO : use decodeColumn once UI changes are done
		List<Object> keyColumns = (List<Object>) document.get(SyncAttrs.KEY_COLUMNS);
		if (keyColumns != null && !keyColumns.isEmpty()) {
			for (Object keyColumn : keyColumns) {
				if (keyColumn instanceof String) {
					table.addKeyColumn((String) keyColumn, "VARCHAR2");
				} else {
					OracleColumn column = decodeColumn((Document) keyColumn);
					table.addKeyColumn(column);
				}

			}
		}
		// table.setKeyColumns(keyColumns);
		referredTables.put(table.getTableAlias(), table);
		List<Document> joinedTableDocList = (List<Document>) document.get(SyncAttrs.JOINED_TABLES);
		if (joinedTableDocList != null) {
			for (Document joinedTableDoc : joinedTableDocList) {
				table.addJoinedTable(decodeJoinedTable(joinedTableDoc));
			}
		}
		return table;
	}

	private JoinedTable decodeJoinedTable(Document document) {
		JoinedTable joinedTable = new JoinedTable();
		OracleTable oracleTable = new OracleTable();
		oracleTable.setTableName(document.getString(SyncAttrs.TABLE_NAME));
		oracleTable.setTableAlias(document.getString(SyncAttrs.TABLE_ALIAS));
		referredTables.put(document.getString(SyncAttrs.TABLE_ALIAS), oracleTable);
		joinedTable.setTable(oracleTable);
		joinedTable.setJoinType(JoinType.valueOf(document.getString(SyncAttrs.JOIN_TYPE)));
		List<Document> nestedJoinedTableDocList = (List<Document>) document.get(SyncAttrs.JOINED_TABLES);
		if (nestedJoinedTableDocList != null) {
			for (Document nestedJoinedTableDoc : nestedJoinedTableDocList) {
				oracleTable.addJoinedTable(decodeJoinedTable(nestedJoinedTableDoc));
			}
		}
		List<Document> filterList = (List<Document>) document.get(SyncAttrs.FILTERS);
		joinedTable.setFilters(decodeFilter(filterList));
		return joinedTable;
	}

	private MongoAttribute decodeMongoAttribute(Document doc) {
		logger.debug("Decode called for MongoAttribute" + doc);
		MongoAttribute attribute = new MongoAttribute();
		attribute.setAttributeName(doc.getString(SyncAttrs.ATTRIBUTE_NAME));
		attribute.setAttributeType(MongoAttributeType.valueOf(doc.getString(SyncAttrs.ATTRIBUTE_TYPE)));
		attribute.setIdentifier(doc.getBoolean(SyncAttrs.IS_IDENTIFIER, false));
		Document mappedOracleEntityDoc = (Document) doc.get(SyncAttrs.COLUMN_DATA);
		if (mappedOracleEntityDoc != null && !mappedOracleEntityDoc.isEmpty())
			attribute.setMappedOracleColumn(decodeColumn(mappedOracleEntityDoc));
		attribute.setDefaultValue(doc.getString(SyncAttrs.DEFAULT_VALUE));
		logger.debug("Decode Completed. Decode Object : " + attribute);
		return attribute;
	}

	private MongoObject decodeMongoObject(Document document) {
		logger.debug("Decode called for MongoObject" + document);
		MongoObject mongoObject = new MongoObject();
		mongoObject.setCollectionName(document.getString(SyncAttrs.ATTRIBUTE_NAME));
		String collectionType = document.getString(SyncAttrs.ATTRIBUTE_TYPE);
		mongoObject.setCollectionType(
				collectionType != null ? collectionType : String.valueOf(MongoAttributeType.COLLECTION));
		List<Document> sourceTablesDocList = (List<Document>) document.get(SyncAttrs.SOURCE_TABLES);
		List<OracleTable> sourceTables = new ArrayList<OracleTable>();
		for (Document sourceTableDoc : sourceTablesDocList) {
			sourceTables.add(decodeTable(sourceTableDoc));
		}
		mongoObject.setSourceTables(sourceTables);
		List<Document> attributeDocList = (List<Document>) document.get(SyncAttrs.ATTRIBUTES);
		MongoEntity entity = null;
		for (Document attributeDoc : attributeDocList) {
			MongoAttributeType attributeTypeEnum = MongoAttributeType
					.valueOf(attributeDoc.getString(SyncAttrs.ATTRIBUTE_TYPE));
			if (attributeTypeEnum.equals(MongoAttributeType.AUTO) || attributeTypeEnum.equals(MongoAttributeType.ARRAY)
					|| attributeTypeEnum.equals(MongoAttributeType.COLLECTION)) {
				entity = decodeMongoObject(attributeDoc);
			} else {
				entity = decodeMongoAttribute(attributeDoc);
			}
			mongoObject.addEntity(entity);
		}
		List<Document> filterDocList = (List<Document>) document.get(SyncAttrs.FILTERS);
		if (filterDocList != null && !filterDocList.isEmpty()) {
			mongoObject.setFilters(decodeFilter(filterDocList));
		}
		List<Document> identifiersList = (List<Document>) document.get(SyncAttrs.IDENTIFIERS);
		if (identifiersList != null && !identifiersList.isEmpty()) {
			for (Document identifier : identifiersList) {
				MongoAttribute attr = decodeMongoAttribute(identifier);
				attr.setIdentifier(true);
				mongoObject.addIdentifierEntity(attr);
			}
		}
		/*
		 * if (mongoObject.getSourceTables() != null &&
		 * !mongoObject.getSourceTables().isEmpty()) { SelectQueryBuilder
		 * queryBuilder = new SelectQueryBuilder(); List<MatchAble> bindvalues =
		 * new ArrayList<MatchAble>(); String selectQuery =
		 * queryBuilder.select().from(mongoObject.getSourceTables().get(0))
		 * .where(mongoObject.getFilters()).getPreparedStatement(bindvalues);
		 * mongoObject.setSelectQuery(selectQuery);
		 * mongoObject.setSelectQueryBindValues(bindvalues); }
		 */
		List<Document> referencedColumns = (List<Document>) document.get(SyncAttrs.REFERENCED_COLUMNS);
		if (referencedColumns != null && !referencedColumns.isEmpty()) {
			for (Document doc : referencedColumns) {
				mongoObject.addReferencedColumns(decodeColumn(doc));
			}
		}
		logger.debug("Decode Completed. Decode Object : " + mongoObject);
		return mongoObject;
	}

	private OracleParallelReadInfo decodeOracleParallelReadInfo(Document document) {
		OracleParallelReadInfo parallelReadInfo = new OracleParallelReadInfo();
		parallelReadInfo.setNumOfBuckets(document.getInteger(SyncAttrs.NUM_OF_BUCKETS, 0));
		parallelReadInfo.setProcessParallel(document.getBoolean(SyncAttrs.PROCESS_PARALLEL, false));
		Document columnDataDoc = (Document) document.get(SyncAttrs.COLUMN_DATA);
		if (columnDataDoc != null && !columnDataDoc.isEmpty()) {
			parallelReadInfo.setRangeColumn(decodeColumn(columnDataDoc));
		}
		return parallelReadInfo;
	}

	private OracleToMongoMap decodeOracleToMongoMap(Document document) {
		logger.debug("Start of decode method");
		OracleToMongoMap map = new OracleToMongoMap();
		Document mapObjectDoc = (Document) document.get(SyncAttrs.MAP_OBJECT);
		if (mapObjectDoc != null && !mapObjectDoc.isEmpty()) {
			map.setMapObject(decodeMongoObject(mapObjectDoc));
		}
		return map;
	}

	private ColumnAttrMapper decodeColumnAttrMapper(Document document) {
		ColumnAttrMapper mapper = new ColumnAttrMapper();
		Document columnData = (Document) document.get(SyncAttrs.COLUMN_DATA);
		if (columnData != null) {
			mapper.setColumn(decodeColumn(columnData));
		}
		mapper.setParentColumn(document.getBoolean(SyncAttrs.IS_PARENT_COLUMN, false));
		mapper.setSeqGenerated(document.getBoolean(SyncAttrs.IS_SEQ_GENERATED, false));
		mapper.setSeqName(document.getString(SyncAttrs.SEQ_NAME));
		mapper.setIgnoreList((List<String>) document.get(SyncAttrs.IGNORE_LIST));
		Object literalValDoc = document.get(SyncAttrs.LITERAL_VALUE_FOR_COLUMN);
		if (literalValDoc != null && !"".equals(literalValDoc)) {
			if (literalValDoc instanceof Document) {
				mapper.setLiteralValueForColumn(decodeLiteral((Document) literalValDoc));
			} else {
				Literal literal = SqlLiteralFactory.getLiteral(literalValDoc, mapper.getColumn().getColumnType());
				mapper.setLiteralValueForColumn(literal);
			}
		}

		Document mongoAttribute = (Document) document.get(SyncAttrs.ATTRIBUTE);
		if (mongoAttribute != null && !mongoAttribute.isEmpty()) {
			mapper.setAttribute(decodeMongoAttribute(mongoAttribute));
		}
		mapper.setParentAttribute(document.getBoolean(SyncAttrs.IS_PARENT_ATTRIBUTE, false));
		mapper.setParentAttributeNode(document.getString(SyncAttrs.PARENT_ATTRIBUTE_NODE));
		mapper.setChildAttribute(document.getBoolean(SyncAttrs.IS_CHILD_ATTRIBUTE, false));
		mapper.setChildAttributeNode(document.getString(SyncAttrs.CHILD_ATTRIBUTE_NODE));
		mapper.setReplacementMap((Map<String, String>) document.get(SyncAttrs.REPLACEMENT_MAP));
		return mapper;
	}

	private NodeGroup decodeNodeGroup(Document nodeDoc) {
		NodeGroup nodeGroup = new NodeGroup();
		nodeGroup.setNodeName(nodeDoc.getString(SyncAttrs.NODE_NAME));

		nodeGroup.setReferenceAttributes((List<String>) nodeDoc.get(SyncAttrs.REF_ATTRS));

		List<Document> tableListDoc = (List<Document>) nodeDoc.get(SyncAttrs.TABLE_LIST);
		if(tableListDoc!=null){
			List<OracleTable> tableList = new LinkedList<OracleTable>();
			for (Document tableDoc : tableListDoc) {
				tableList.add(decodeTable(tableDoc));
			}
			nodeGroup.setTableList(tableList);
		}		
		

		List<Document> columnAttrMapperDocList = (List<Document>) nodeDoc.get(SyncAttrs.COLUMN_ATTR_MAP);
		if (columnAttrMapperDocList != null && !columnAttrMapperDocList.isEmpty()) {
			Map<String, ColumnAttrMapper> mapperList = new HashMap<String, ColumnAttrMapper>();
			for (Document columnAttrMapperDoc : columnAttrMapperDocList) {
				ColumnAttrMapper mapper = decodeColumnAttrMapper(columnAttrMapperDoc);
				mapperList.put(mapper.getColumn().getColumnAlias(), mapper);
			}
			nodeGroup.setColumnAttrMappers(mapperList);
		}
		
		List<Document> childNodeDocList = (List<Document>) nodeDoc.get(SyncAttrs.CHILD_NODES);
		if(childNodeDocList!=null && !childNodeDocList.isEmpty()){
			List<NodeGroup> childNodeList = new ArrayList<NodeGroup>();
			for(Document childNodeDoc : childNodeDocList){
				NodeGroup childNode = decodeNodeGroup(childNodeDoc);
				childNodeList.add(childNode);
			}
			nodeGroup.setChildGroups(childNodeList);
		}
		return nodeGroup;
	}

	private MongoToOracleMap decodeMongoToOracleMap(Document mapDocument) {
		MongoToOracleMap map = new MongoToOracleMap();
		map.setCollectionName(mapDocument.getString(SyncAttrs.COLLECTION_NAME));

		List<Document> nodeGrpListDoc = (List<Document>) mapDocument.get(SyncAttrs.NODE_TABLE_GROUP);
		if (nodeGrpListDoc != null && !nodeGrpListDoc.isEmpty()) {
			//Map<String, NodeGroup> tableNodeGrp = new HashMap<String, NodeGroup>();
			List<NodeGroup> rootNodeList = new ArrayList<NodeGroup>();
			for (Document nodeGrpDoc : nodeGrpListDoc) {
				if (nodeGrpDoc != null && !nodeGrpDoc.isEmpty()) {
					NodeGroup node = decodeNodeGroup(nodeGrpDoc);
					rootNodeList.add(node);
					//tableNodeGrp.put(node.getNodeName(), node);
				}
			}
			map.setRootNode(rootNodeList);
		}
		return map;
	}

	public SyncMap decodeSyncMap(Document document) {
		SyncMap map = null;
		String mapTypeStr = document.getString(SyncAttrs.MAP_TYPE);
		MapType mapTypeEnum = MapType.valueOf(mapTypeStr);
		switch (mapTypeEnum) {
		case OrclToMongo:
			map = decodeOracleToMongoMap(document);
			break;
		case MongoToOrcl:
			map = decodeMongoToOracleMap(document);
			break;
		case OrclToMongoGridFs:
			map = decodeOracleToMongoGridFsMap(document);
			break;
		default:
			return null;
		}

		Object _id = document.get(SyncAttrs.ID);
		if (_id instanceof String) {// Coming from UI
			map.setMapId(new ObjectId((String) _id));
		} else if (_id instanceof ObjectId) { // Coming from Db
			map.setMapId((ObjectId) _id);
		}
		map.setMapName(document.getString(SyncAttrs.MAP_NAME));
		map.setMapType(MapType.valueOf(mapTypeStr));
		map.setCreatedBy(document.getString(SyncAttrs.CREATED_BY));
		if (document.getDate(SyncAttrs.CREATED_ON) != null) {
			map.setCreatedOn(document.getDate(SyncAttrs.CREATED_ON));
		} else {
			map.setCreatedOn(new Date());// Map creation
		}
		map.setApprovedBy(document.getString(SyncAttrs.APPROVED_BY));
		map.setApprovedOn(document.getDate(SyncAttrs.APPROVED_ON));
		map.setComments(document.getString(SyncAttrs.COMMENTS));
		map.setSourceDbName(document.getString(SyncAttrs.SOURCE_DB_NAME));
		map.setSourceUserName(document.getString(SyncAttrs.SOURCE_USER_NAME));
		map.setTargetDbName(document.getString(SyncAttrs.TARGET_DB_NAME));
		map.setTargetUserName(document.getString(SyncAttrs.TARGET_USER_NAME));
		return map;
	}

	// hook to process extra attributes in future
	private MongoToOracleEvent decodeMongoToOracleEvent(Document document) {
		MongoToOracleEvent event = new MongoToOracleEvent();
		event.setCollectionName(document.getString(SyncAttrs.COLLECTION_NAME));
		return event;
	}

	// hook to process extra attributes in future
	private MongoToOracleSyncEvent decodeMongoToOracleSyncEvent(Document document) {
		MongoToOracleSyncEvent event = new MongoToOracleSyncEvent();
		BsonTimestamp lastReadTime = (BsonTimestamp) document.get(SyncAttrs.LAST_READ_TIME);
		event.setRestrictedSyncEnabled(document.getBoolean(SyncAttrs.RESTRCTITED_SYNC_ENABLED, false));
		if (lastReadTime == null) {
			lastReadTime = new BsonTimestamp();
		}
		event.setLastReadTime(lastReadTime);
		return event;
	}

	private OracleToMongoEvent decodeOracleToMongoEvent(Document document) {
		logger.debug("Start of decode method");
		OracleToMongoEvent event = new OracleToMongoEvent();
		event.setCollectionName(document.getString(SyncAttrs.COLLECTION_NAME));
		event.setSaveNulls(document.getBoolean(SyncAttrs.SAVE_NULLS, false));
		Document parallelReadInfo = (Document) document.get(SyncAttrs.PARALLEL_PROCESSING_INFO);
		if (parallelReadInfo != null) {
			event.setParallelReadInfo(decodeOracleParallelReadInfo(parallelReadInfo));
		}
		List<Document> rangeDoc = (List<Document>) document.get(SyncAttrs.RANGE_FILTER);
		if (rangeDoc != null) {
			SQLFilters rangeFilter = decodeFilter(rangeDoc);
			event.setRangeFilter(rangeFilter);
		}
		logger.debug("Decode method completed. Decoded document : " + event);
		return event;
	}

	private OracleToMongoSyncEvent decodeOracleToMngSyncEvent(Document document) {
		OracleToMongoSyncEvent event = new OracleToMongoSyncEvent();
		event.setCollectionName(document.getString(SyncAttrs.COLLECTION_NAME));
		event.setSaveNulls(document.getBoolean(SyncAttrs.SAVE_NULLS, false));
		List<Document> keyAttributeDocList = (List<Document>) document.get(SyncAttrs.KEY_ATTRIBUTES);
		if(keyAttributeDocList!=null){
			List<MongoAttribute> keyAttrList = new ArrayList<MongoAttribute>(keyAttributeDocList.size());
			for(Document keyAttrDoc : keyAttributeDocList){
				keyAttrList.add(decodeMongoAttribute(keyAttrDoc));
			}
			event.setKeyAttributes(keyAttrList);
		}
		event.setPollBased(document.getBoolean(SyncAttrs.POLL_BASED, false));
		if(event.isPollBased()){
			O2MSyncPollInfo pollInfo = new O2MSyncPollInfo();
			Document pollInfoDoc = (Document) document.get(SyncAttrs.POLL_INFO);
			pollInfo.setInterval(pollInfoDoc.getInteger(SyncAttrs.INTERVAL,1));
			Date lastReadTime = pollInfoDoc.getDate(SyncAttrs.LAST_READ_TIME);
			if(lastReadTime!=null){
				pollInfo.setLastReadTime(lastReadTime);				
			}else{
				pollInfo.setLastReadTime(new Date());
			}
			pollInfo.setPollingColumn(decodeColumn((Document) pollInfoDoc.get(SyncAttrs.POLLING_COLUMN)));
			pollInfo.setTimeUnit(pollInfoDoc.getString(SyncAttrs.TIME_UNIT));
			event.setPollInfo(pollInfo);
		}
		return event;
	}

	public SyncEvent decodeSyncEvent(Document document) {
		SyncEvent event = null;
		String eventTypeStr = document.getString(SyncAttrs.EVENT_TYPE);
		EventType eventTypeEnum = EventType.valueOf(eventTypeStr);
		switch (eventTypeEnum) {
		case OrclToMongo:
			event = decodeOracleToMongoEvent(document);
			break;
		case MongoToOrcl:
			event = decodeMongoToOracleEvent(document);
			break;
		case MongoToOrclSync:
			event = decodeMongoToOracleSyncEvent(document);
			break;
		case OrclToMongoSync:
			event = decodeOracleToMngSyncEvent(document);
			break;
		case OrclToMongoGridFs:
			event = decodeOracleToMongoGridFsEvent(document);
			break;
		default:
			return null;
		}
		Object _id = document.get(SyncAttrs.ID);
		if (_id instanceof String) {// Coming from UI
			event.setEventId(new ObjectId((String) _id));
		} else if (_id instanceof ObjectId) { // Coming from Db
			event.setEventId((ObjectId) _id);
		}
		Object mappingId = document.get(SyncAttrs.MAP_ID);
		if (mappingId instanceof String) {
			event.setMapId(new ObjectId((String) mappingId));
		} else if (mappingId instanceof ObjectId) {
			event.setMapId((ObjectId) mappingId);
		}
		event.setEventName(document.getString(SyncAttrs.EVENT_NAME));
		event.setComments(document.getString(SyncAttrs.COMMENTS));
		event.setBatchSize(document.getInteger(SyncAttrs.BATCH_SIZE, SyncConstants.DEFAULT_BATCH_SIZE));
		event.setCreatedBy(document.getString(SyncAttrs.CREATED_BY));
		event.setCreatedOn(document.getDate(SyncAttrs.CREATED_ON));
		event.setApprovedBy(document.getString(SyncAttrs.APPROVED_BY));
		event.setApprovedOn(document.getDate(SyncAttrs.APPROVED_ON));
		event.setStatus(document.getString(SyncAttrs.STATUS));
		event.setMapName(document.getString(SyncAttrs.MAP_NAME));
		event.setRetry(document.getBoolean(SyncAttrs.IS_RETRY, false));
		event.setNotifIds(document.getString(SyncAttrs.NOTIF_ALIAS));
		event.setEventType(EventType.valueOf(eventTypeStr));
		event.setParentEventId(document.getObjectId(SyncAttrs.PARENT_EVENT_ID));
		if (event.getParentEventId() == null) {
			event.setParentEventId(event.getEventId());
		}
		return event;
	}

	public static Document parsefromJson(String jsonString) {
		return Document.parse(jsonString);
	}

	public SyncEvent decodeSyncEvent(String jsonString) {
		return decodeSyncEvent(parsefromJson(jsonString));
	}

	public SyncMap decodeSyncMap(String jsonString) {
		return decodeSyncMap(parsefromJson(jsonString));
	}
	
	private OracleToMongoGridFsMap decodeOracleToMongoGridFsMap(Document document){
		OracleToMongoGridFsMap fsMap = new OracleToMongoGridFsMap();
		fsMap.setCollectionName(document.getString(SyncAttrs.ATTRIBUTE_NAME));
		if(document.get(SyncAttrs.STREAM_TABLE) !=null){
			Document streamTable = (Document) document.get(SyncAttrs.STREAM_TABLE);
			fsMap.setStreamTable(decodeTable(streamTable));
		}
		fsMap.setFileNameColumn(decodeColumn((Document) document.get(SyncAttrs.FILE_NAME_COLUMN)));
		fsMap.setInputStreamColumn(decodeColumn((Document) document.get(SyncAttrs.INPUT_STREAM_COLUMN)));
		if(document.get(SyncAttrs.META_ATTRIBUTES) !=null){
			Document metaDoc = (Document)document.get(SyncAttrs.META_ATTRIBUTES);
			List<Document> attributeDocList = (List<Document>) metaDoc.get(SyncAttrs.ATTRIBUTES);
			if (attributeDocList != null && !attributeDocList.isEmpty()) {
				Map<String, ColumnAttrMapper> mapperList = new HashMap<String, ColumnAttrMapper>();
				for (Document columnAttrMapperDoc : attributeDocList) {
					ColumnAttrMapper mapper = decodeColumnAttrMapper(columnAttrMapperDoc);
					mapperList.put(mapper.getColumn().getColumnAlias(), mapper);
				}
				fsMap.setMetaAttributes(mapperList);
			}
		}
		List<Document> filterDocList = (List<Document>) document.get(SyncAttrs.FILTERS);
		if (filterDocList != null && !filterDocList.isEmpty()) {
			fsMap.setFilters(decodeFilter(filterDocList));
		}
		return fsMap; 
	}
	
	private OracleToMongoGridFsEvent decodeOracleToMongoGridFsEvent(Document document) {
		logger.debug("Start of decode method");
		OracleToMongoGridFsEvent event = new OracleToMongoGridFsEvent();
		event.setCollectionName(document.getString(SyncAttrs.COLLECTION_NAME));
		event.setSaveNulls(document.getBoolean(SyncAttrs.SAVE_NULLS, false));
		List<Document> rangeDoc = (List<Document>) document.get(SyncAttrs.RANGE_FILTER);
		if (rangeDoc != null) {
			SQLFilters rangeFilter = decodeFilter(rangeDoc);
			event.setRangeFilter(rangeFilter);
		}
		logger.debug("Decode method completed. Decoded document : " + event);
		return event;
	}
}
