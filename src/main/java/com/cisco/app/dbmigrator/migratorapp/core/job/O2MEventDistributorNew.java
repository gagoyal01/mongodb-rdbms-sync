package com.cisco.app.dbmigrator.migratorapp.core.job;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.cisco.app.dbmigrator.migratorapp.core.event.EventType;
import com.cisco.app.dbmigrator.migratorapp.core.event.OracleToMongoEvent;
import com.cisco.app.dbmigrator.migratorapp.core.event.OracleToMongoGridFsEvent;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncError;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncEvent;
import com.cisco.app.dbmigrator.migratorapp.core.map.OracleToMongoGridFsMap;
import com.cisco.app.dbmigrator.migratorapp.core.map.OracleToMongoMap;
import com.cisco.app.dbmigrator.migratorapp.core.map.SyncMap;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoEntity;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoObject;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncEventDao;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncMapDao;
import com.cisco.app.dbmigrator.migratorapp.mail.Mailer;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.JoinedTable;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.MatchAble;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.MatchOperator;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleTable;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.SqlColumnType;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.SqlLiteralFactory;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.queries.SelectQueryBuilder;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.Operations;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.OperationsFactory;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.SQLFilters;
import com.cisco.app.dbmigrator.migratorapp.utilities.cache.DBCacheManager;
import com.cisco.app.dbmigrator.migratorapp.utilities.oracle.DbResourceUtils;

@SuppressWarnings("rawtypes")
public class O2MEventDistributorNew implements Runnable {
	private final Logger logger = Logger.getLogger(getClass());
	private SyncEvent rootEvent;
	private final SyncMapDao mappingDao;
	private final SyncEventDao eventDao;
	private List<SyncEvent> subEventList;
	private Connection connection;
	private static final String HASH = "#";
	private int eventCount = 0;
	private final OracleColumn rangeColumn;
	private float idealRange;
	private int optimalRange;
	private int degree;
	private SyncMap map;
	private int tCount = 0;
	private static final String ROW_ID_QUERY = "SELECT ROWID AS ROW_ID FROM TNAME ORDER BY ROWID";
	private static final String GRID_ROW_ID_QUERY = "SELECT ROWID AS ROW_ID FROM TNAME WHERE CNAME IS NOT NULL ORDER BY ROWID";
	private static final String ROW_ID_QUERY_GREATER_THAN_ROWID = "SELECT ROWID AS ROW_ID FROM TNAME WHERE ROWID >= ? ORDER BY ROWID";
	private static final String GRID_ROW_ID_QUERY_GREATER_THAN_ROWID = "SELECT ROWID AS ROW_ID FROM TNAME WHERE CNAME IS NOT NULL AND ROWID >= ? ORDER BY ROWID";
	private int subEventCount = 0;
	private long fetchCount = 0;

	public O2MEventDistributorNew(SyncEvent rootEvent) {
		super();
		this.rootEvent = rootEvent;
		this.mappingDao = new SyncMapDao();
		this.eventDao = new SyncEventDao();
		this.rangeColumn = new OracleColumn();
		this.rangeColumn.setColumnName("ROWID");
		this.rangeColumn.setColumnType(SqlColumnType.ROWID);
	}

	public void process() {
		logger.info("O2MEventDistributor thread started at : " + new Date());
		subEventList = new ArrayList<SyncEvent>();
		try {
			map = mappingDao.getMapping(rootEvent.getMapId());
			connection = DBCacheManager.INSTANCE.getCachedOracleConnection(map.getSourceDbName(),
					map.getSourceUserName());
			if (rootEvent.isRetry()) {
				eventDao.cancelEvent(rootEvent.getEventId());
				String collectionName = null;
				if (rootEvent.getEventType() == EventType.OrclToMongo) {
					collectionName = ((OracleToMongoEvent) rootEvent).getCollectionName();
				} else {
					collectionName = ((OracleToMongoGridFsEvent) rootEvent).getCollectionName();
				}
				DBCacheManager.INSTANCE.getCachedMongoPool(map.getTargetDbName(), map.getTargetUserName())
						.getDatabase(map.getTargetDbName()).getCollection(collectionName).drop();
			}
			if (rootEvent.getEventType() == EventType.OrclToMongo) {
				processOracleToMongoEvent();
			} else {
				processGridEvent();
			}
			if (!subEventList.isEmpty()) {
				logger.info("Number of subEvents created : " + eventCount);
				eventDao.bulkInsert(subEventList);
				eventDao.updateEventStatus(rootEvent.getEventId(), SyncStatus.IN_PROGRESS);
				logger.debug("SubEvents inserted into DB");
			}
			Mailer.sendmail(rootEvent, null, null, Mailer.STARTED);
		} catch (SyncError e) {
			logger.error("Error in O2MEventDistributor for event : " + rootEvent.getEventName(), e);
			e.setThreadName(Thread.currentThread().getName());
			eventDao.pushError(rootEvent.getEventId(), e);
			Mailer.sendmail(rootEvent, null, e, Mailer.FAILURE);
		} catch (SQLException exp) {
			SyncError e = new SyncError(exp);
			e.setThreadName(Thread.currentThread().getName());
			eventDao.pushError(rootEvent.getEventId(), e);
			Mailer.sendmail(rootEvent, null, e, Mailer.FAILURE);
		} finally {
			DbResourceUtils.closeResources(null, null, connection);
		}
		logger.info("O2MEventDistributor thread Completed at : " + System.currentTimeMillis());
	}

	@Override
	public void run() {
		process();
	}

	private void getTableCount(OracleTable rootTable) {
		tCount += 1;
		if (rootTable.getJoinedTables() != null) {
			for (JoinedTable joinedTable : rootTable.getJoinedTables()) {
				getTableCount(joinedTable.getTable());
			}
		}
	}

	private void getTableCount(MongoObject mongoObject) {
		if (mongoObject.getSourceTables() != null && mongoObject.getSourceTables().size() > 0) {
			tCount += 1;
			OracleTable rootTable = mongoObject.getSourceTables().get(0);
			if (rootTable.getJoinedTables() != null) {
				for (JoinedTable joinedTable : rootTable.getJoinedTables()) {
					getTableCount(joinedTable.getTable());
				}
			}
		}
		if (mongoObject.getAttributes() != null) {
			for (MongoEntity entity : mongoObject.getAttributes()) {
				if (entity instanceof MongoObject) {
					getTableCount((MongoObject) entity);
				}
			}
		}
	}

	private void deduceIdealRange() {
		if (tCount >= 5) {
			idealRange = 5000;
		} else if (tCount == 4) {
			idealRange = 10000;
		} else if (tCount == 3) {
			idealRange = 15000;
		} else {
			idealRange = 25000;
		}
		/*
		 * else if (tCount == 2) { idealRange = 25000; } else { idealRange =
		 * 50000; }
		 */
		logger.info("Ideal range :" + idealRange);
	}

	private void generateOptimalRange(float rowCount) {
		optimalRange = (int) idealRange;
		float div = rowCount / idealRange;
		degree = (int) div;
		float fr = div - degree;
		if (fr > .2) {
			degree += 1;
		}
		if (degree > 1) {
			optimalRange = (int) (rowCount / degree);
		}
		logger.info("Optimal range :" + optimalRange);
	}

	private void getSubEvent(RowId minRid, RowId maxRid, boolean isLast) throws CloneNotSupportedException {
		SyncEvent childEvent = null;
		if (rootEvent.getEventType() == EventType.OrclToMongo) {
			OracleToMongoEvent oracleToMongoEvent = (OracleToMongoEvent) rootEvent;
			OracleToMongoEvent subEvent = (OracleToMongoEvent) oracleToMongoEvent.clone();
			subEvent.setRangeFilter(getRowIdFilter(minRid, maxRid, isLast));
			childEvent = subEvent;
		} else {
			OracleToMongoGridFsEvent gridEvent = (OracleToMongoGridFsEvent) rootEvent;
			OracleToMongoGridFsEvent subEvent = (OracleToMongoGridFsEvent) gridEvent.clone();
			subEvent.setRangeFilter(getRowIdFilter(minRid, maxRid, isLast));
			childEvent = subEvent;
		}

		childEvent.setEventName(rootEvent.getEventName() + HASH + (++eventCount));
		subEventList.add(childEvent);
		logger.info("SubEvent created with name :" + childEvent.getEventName());
		int size = subEventList.size();
		if (size >= 10) {
			eventDao.bulkInsert(subEventList);
			subEventList.clear();
			logger.info("EventList Saved");
		}
	}

	private SQLFilters getRowIdFilter(RowId minRid, RowId maxRid, boolean isLast) {
		SQLFilters firstRange = SQLFilters.getFilter(OperationsFactory.getMatchExpression(rangeColumn,
				SqlLiteralFactory.getLiteral(minRid, rangeColumn.getColumnType()), String.valueOf(MatchOperator.GTE)))
				.AND(OperationsFactory.getMatchExpression(rangeColumn,
						SqlLiteralFactory.getLiteral(maxRid, rangeColumn.getColumnType()),
						isLast ? String.valueOf(MatchOperator.LTE) : String.valueOf(MatchOperator.LT)));
		return firstRange;
	}

	private void processOracleToMongoEvent() throws SyncError {
		if (rootEvent.getMarker() != null && rootEvent.getMarker().isFailed()) {
			return;
		}
		logger.debug("Started processing RootEvent");
		long rowCount = 0;
		map = mappingDao.getMapping(rootEvent.getMapId());
		MongoObject mongoObject = ((OracleToMongoMap) map).getMapObject();
		getTableCount(mongoObject);
		logger.info("Table Count :" + tCount);
		deduceIdealRange();

		logger.info("Getting Connection");
		rowCount = getOrclToMongoCount(mongoObject, ((OracleToMongoEvent) rootEvent).getRangeFilter());
		logger.info("Row count for RootEvent : " + rowCount);
		if (rowCount > idealRange) {
			generateOptimalRange(rowCount);
			if (degree > 1) {
				this.rangeColumn.setTableAlias(mongoObject.getSourceTables().get(0).getTableAlias());
				String tableName = mongoObject.getSourceTables().get(0).getTableName();
				String query = ROW_ID_QUERY.replaceFirst("TNAME", tableName);
				String rowIdQuery = ROW_ID_QUERY_GREATER_THAN_ROWID.replaceFirst("TNAME", tableName);
				RowId rid = createSubEvents(query, null, tableName, false);
				while (subEventCount < (degree - 1)) {
					fetchCount = 0L;
					rid = createSubEvents(rowIdQuery, rid, tableName, true);
				}
			} else {
				NodeBalancer.INSTANCE.addEventToExecutor(rootEvent);
				// rootEvent.setProcess(true);
			}
		} else if (rowCount > 0) {
			NodeBalancer.INSTANCE.addEventToExecutor(rootEvent);
			// rootEvent.setProcess(true);
		} else {
			eventDao.updateEventStatus(rootEvent.getEventId(), SyncStatus.COMPLETE);
		}

	}

	private long getOrclToMongoGridFsCount(OracleToMongoGridFsMap gridMap, SQLFilters eventRangeFilter)
			throws SyncError {
		long count = 0;
		SQLFilters rangeFilter = null;

		if (gridMap.getFilters() != null) {
			rangeFilter = gridMap.getFilters();
			if (eventRangeFilter != null) {
				rangeFilter = rangeFilter.AND(eventRangeFilter);
			}
		} else {
			if (eventRangeFilter != null) {
				rangeFilter = eventRangeFilter;
			}
		}
		if (rangeFilter != null) {
			rangeFilter = rangeFilter.AND(Operations.isNotNull(gridMap.getInputStreamColumn()));
		} else {
			rangeFilter = SQLFilters.getFilter(Operations.isNotNull(gridMap.getInputStreamColumn()));
		}
		List<MatchAble> bindValues = new ArrayList<MatchAble>();
		String countQuery = new SelectQueryBuilder().select().from(gridMap.getStreamTable()).where(rangeFilter)
				.getCountQuery(bindValues);
		logger.info("Count Query for GridEvent " + countQuery);
		count = getCount(countQuery, bindValues);
		return count;
	}

	private long getOrclToMongoCount(MongoObject mongoObject, SQLFilters eventRangeFilter) throws SyncError {
		long count = 0;

		SQLFilters rangeFilter = null;

		if (mongoObject.getFilters() != null) {
			rangeFilter = mongoObject.getFilters();
			if (eventRangeFilter != null) {
				rangeFilter = rangeFilter.AND(eventRangeFilter);
			}
		} else {
			if (eventRangeFilter != null) {
				rangeFilter = eventRangeFilter;
			}
		}

		List<MatchAble> bindValues = new ArrayList<MatchAble>();
		String countQuery = new SelectQueryBuilder().select().from(mongoObject.getSourceTables().get(0))
				.where(rangeFilter).getCountQuery(bindValues);
		logger.info("Count Query for OracleToMongoEvent " + countQuery);
		count = getCount(countQuery, bindValues);
		return count;
	}

	private long getCount(String countQuery, List<MatchAble> bindValues) throws SyncError {
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		long count = 0;
		try {
			pstmt = connection.prepareStatement(countQuery);
			if (bindValues != null) {
				for (int index = 0; index < bindValues.size(); index++) {
					pstmt.setObject(index + 1, bindValues.get(index).getSqlExpressionForMatchable());
				}
			}
			rset = pstmt.executeQuery();
			rset.next();
			count = rset.getInt(1);
		} catch (SQLException e) {
			logger.error("Error while getting count", e);
			throw new SyncError(e);
		} catch (Exception e) {
			logger.error("Unexpected error while getting count", e);
			throw new SyncError(e);
		} finally {
			DbResourceUtils.closeResources(rset, pstmt, null);
		}
		return count;
	}

	private RowId createSubEvents(final String query, RowId rid, String tableName, boolean isChildQuery) throws SyncError {
		logger.info("createSubEvents called with parameters : isChildQuery =" + isChildQuery + " , rid = " + rid
				+ " , tableName = " + tableName + " , query = " + query);
		PreparedStatement rowIdpstmt = null;
		ResultSet rowIdSet = null;
		RowId maxRid = null;
		RowId minRid = null;
		try {
			rowIdpstmt = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
			if (isChildQuery) {
				rowIdpstmt.setRowId(1, rid);
			}
			rowIdpstmt.setFetchSize(5000);
			rowIdSet = rowIdpstmt.executeQuery();
			rowIdSet.next();
			minRid = rowIdSet.getRowId(1);
			for (++subEventCount; subEventCount < (degree - 1); subEventCount++) {
				rowIdSet.relative((int) optimalRange);
				maxRid = rowIdSet.getRowId(1);
				getSubEvent(minRid, maxRid, false);
				minRid = maxRid;
				fetchCount += optimalRange;
				if (fetchCount > 1000000L) {
					break;
				}
			}
			if (subEventCount == (degree - 1)) {
				rowIdSet.last();
				maxRid = rowIdSet.getRowId(1);
				getSubEvent(minRid, maxRid, true);
				logger.info("Total subEvents created :" + eventCount);
			}
		} catch (Exception e) {
			logger.error("Error while creating subEvents ", e);
			throw new SyncError(e);
		} finally {
			DbResourceUtils.closeResources(rowIdSet, rowIdpstmt, null);
		}
		return maxRid;
	}

	private void processGridEvent() throws SyncError {
		if (rootEvent.getMarker() != null && rootEvent.getMarker().isFailed()) {
			return;
		}
		logger.debug("Started processing RootEvent");
		long rowCount = 0;
		map = mappingDao.getMapping(rootEvent.getMapId());
		OracleToMongoGridFsMap gridMap = (OracleToMongoGridFsMap) map;
		OracleToMongoGridFsEvent gridEvent = (OracleToMongoGridFsEvent) rootEvent;
		// deduceIdealRange();
		idealRange = 1000;
		logger.info("Getting RowCount for GridEvent");
		rowCount = getOrclToMongoGridFsCount(gridMap, gridEvent.getRangeFilter());
		logger.info("Row count for RootEvent : " + rowCount);
		if (rowCount > idealRange) {
			generateOptimalRange(rowCount);
			// optimalRange = 2000;
			if (degree > 1) {
				this.rangeColumn.setTableAlias(gridMap.getStreamTable().getTableAlias());
				String tableName = gridMap.getStreamTable().getTableName();
				String query = GRID_ROW_ID_QUERY.replaceFirst("TNAME", tableName).replaceFirst("CNAME",
						gridMap.getInputStreamColumn().getColumnName());
				String rowIdQuery = GRID_ROW_ID_QUERY_GREATER_THAN_ROWID.replaceFirst("TNAME", tableName).replaceFirst("CNAME",
						gridMap.getInputStreamColumn().getColumnName());
				RowId rid = createSubEvents(query, null, tableName, false);
				while (subEventCount < (degree - 1)) {
					fetchCount = 0L;
					rid = createSubEvents(rowIdQuery, rid, tableName, true);
				}
				logger.info("Total subEvents created :" + eventCount);
			} else {
				NodeBalancer.INSTANCE.addEventToExecutor(rootEvent);
			}
		} else if (rowCount > 0) {
			NodeBalancer.INSTANCE.addEventToExecutor(rootEvent);
		} else {
			eventDao.updateEventStatus(rootEvent.getEventId(), SyncStatus.COMPLETE);
		}
	}
}