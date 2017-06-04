package com.cisco.app.dbmigrator.migratorapp.logging.codecs;

public enum SyncAttrs {
	MapAndEventConst;
	public static final String ID = "_id";
	public static final String MAP_OBJECT = "mapObject";
	public static final String CREATED_BY = "createdBy";
	public static final String APPROVED_BY = "approvedBy";
	public static final String CREATED_ON = "createdOn";
	public static final String APPROVED_ON = "approvedOn";
	public static final String COMMENTS = "comments";
	public static final String SOURCE_USER_NAME = "sourceUserName";
	public static final String SOURCE_DB_NAME = "sourceDbName";
	public static final String TARGET_USER_NAME = "targetUserName";
	public static final String TARGET_DB_NAME = "targetDbName";
	public static final String MAP_TYPE = "mapType";
	public static final String COLLECTION_NAME = "collectionName";
	public static final String COLUMN_ALIAS_TO_ATTR_NAME_MAP = "columnAliasToAttrNameMap";
	public static final String COLUMN_ATTR_MAP = "columnAttrMap";
	public static final String COLUMN_DATA = "columnData";
	public static final String ATTRIBUTE = "attribute";
	public static final String IS_PARENT_COLUMN = "isParentColumn";
	public static final String IS_SEQ_GENERATED = "isSeqGenerated";
	public static final String SEQ_NAME = "seqName";
	public static final String IGNORE_LIST = "ignoreList";
	public static final String LITERAL_VALUE_FOR_COLUMN = "literalValueForColumn";
	public static final String IS_PARENT_ATTRIBUTE = "isParentAttribute";
	public static final String PARENT_ATTRIBUTE_NODE = "parentNode";
	public static final String IS_CHILD_ATTRIBUTE = "isChildAttribute";
	public static final String CHILD_ATTRIBUTE_NODE = "ChildAttributeNode";
	public static final String NODE_TABLE_GROUP = "nodeGroup";
	public static final String PARENT_EVENT_ID = "parentEventId";
	public static final String EVENT_NAME = "eventName";
	public static final String EVENT_TYPE = "eventType";
	public static final String STATUS = "status";
	public static final String MAP_ID = "mapId";
	public static final String MAP_NAME = "mapName";
	public static final String BATCH_SIZE = "batchSize";
	public static final String RANGE_FILTER = "rangeFilter";
	public static final String PARALLEL_PROCESSING_INFO = "parallelReadInfo";
	public static final String SAVE_NULLS = "saveNulls";
	public static final String IS_IDENTIFIER = "isIdentifier";
	public static final String IDENTIFIERS = "identifiers";
	public static final String EXPRESSION_TYPE = "expressionType";
	public static final String LITERAL_VALUE = "literalValue";
	public static final String LITERAL_TYPE = "literalType";
	public static final String SQL_OPERATION = "sqlOperation";
	public static final String LOGICAL_OPERATOR = "logicalOperator";
	public static final String FILTERS = "filters";
	public static final String ATTRIBUTE_NAME = "attributeName";
	public static final String SOURCE_TABLES = "sourceTables";
	public static final String LEFT_HAND_EXPRESSION = "leftHandExpression";
	public static final String RIGHT_HAND_EXPRESSION = "rightHandExpression";
	public static final String ATTRIBUTE_TYPE = "attributeType";
	public static final String ATTRIBUTES = "attributes";
	public static final String COLUMN_NAME = "columnName";
	public static final String COLUMN_ALIAS = "columnAlias";
	public static final String COLUMN_TYPE = "columnType";
	public static final String PRECISION = "precision";
	public static final String IS_NULLABLE = "isNullable";
	public static final String TABLE_NAME = "tableName";
	public static final String TABLE_ALIAS = "tableAlias";
	public static final String TABLE_LIST = "tableList";
	public static final String COLUMNS = "columns";
	public static final String JOINED_TABLES = "joinedTables";
	public static final String JOIN_TYPE = "joinType";
	public static final String PROCESS_PARALLEL = "processParallel";
	public static final String NUM_OF_BUCKETS = "numOfBuckets";
	public static final String NODE_NAME = "nodeName";
	public static final String REF_ATTRS = "referenceAttributes";
	public static final String KEY_COLUMNS = "keyColumns";
	public static final String LAST_READ_TIME = "lastReadTime";
	public static final String MARKER = "marker";
	public static final String ERRORS = "errors";
	public static final String IS_RETRY = "isRetry";
	public static final String KEY_ATTRIBUTES = "keyAttrs";
	public static final String CHILD_NODES = "childNodes";
	public static final String RESTRCTITED_SYNC_ENABLED = "rse";
	public static final String POLL_BASED = "pollBased";
	public static final String INTERVAL = "interval";
	public static final String POLLING_COLUMN = "pollingColumn";
	public static final String TIME_UNIT = "timeUnit";
	public static final String POLL_INFO = "pollInfo";
	public static final String DURATION = "duration";
	public static final String NOTIF_ALIAS = "notificationAlias";
	public static final String APPLICATION_NAME = "appName";

	public static final String COLUMN = "column";
	public static final String LITERAL = "literal";

	// Marker Attributes
	public static final String EVENT_ID = "eventId";
	public static final String ROWS_READ = "rowsRead";
	public static final String ROWS_DUMPED = "rowsDumped";
	public static final String TOTAL_ROWS = "totalRows";
	public static final String START_TIME = "startTime";
	public static final String END_TIME = "endTime";
	public static final String ALL_ROWS_FETCHED = "allRowsFetched";

	// Event Error Attributes
	public static final String ERROR_ID = "errorId";
	public static final String ERROR_MESSAGE = "errorMessage";
	public static final String THREAD_NAME = "threadName";
	public static final String TRACE = "trace";

	public static final String REPLACEMENT_MAP = "replacementMap";
	public static final String DEFAULT_VALUE = "defaultValue";
	public static final String REFERENCED_COLUMNS = "referencedColumns";

	public static final String LITERAL_VALUES = "literalValues";
	public static final String ARRAY = "array";

	// SyncNode Attributes
	public static final String HOST = "host";
	public static final String NODE = "node";
	public static final String JVM = "jvm";
	public static final String LIFE_CYCLE = "lifeCycle";
	public static final String STATE = "state";
	public static final String CON_LEVEL = "concurrencyLevel";
	public static final String TOTAL_HEAP_SIZE = "totalHeapSize";
	public static final String USED_HEAP_SIZE = "usedHeapSize";
	public static final String ACTIVE_EVENTS = "activeEvents";
	public static final String EVENT_TYPES = "eventTypes";
	public static final String SYSTEM_EVENTS = "systemEvents";
	public static final String HOST_NAME = "hostName";
	public static final String LAST_PING_TIME = "lastPingTime";
	public static final String FAILURE_TIME = "failureTime";
	public static final String UUID = "appId";
	
	public static final String STREAM_TABLE = "streamTable";
	public static final String META_ATTRIBUTES = "metaAttributes";
	public static final String FILE_NAME_COLUMN="fileNameColumn";
	public static final String INPUT_STREAM_COLUMN = "inputStreamColumn";

}
