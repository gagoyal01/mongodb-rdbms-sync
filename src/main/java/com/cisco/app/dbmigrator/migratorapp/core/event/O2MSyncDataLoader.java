package com.cisco.app.dbmigrator.migratorapp.core.event;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.logging.dao.O2MEventLogDao;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.O2MSyncEventLog;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.O2MSyncEventLog.O2MSyncEventInfo;
import com.cisco.app.dbmigrator.migratorapp.utilities.cache.DBCacheManager;
import com.cisco.app.dbmigrator.migratorapp.utilities.oracle.DbResourceUtils;

import oracle.sql.ARRAY;
import oracle.sql.STRUCT;

/**
 * Loader class to Load Event log(meta data) from Oracle DB and dump into MongoDB event log collections for O2M sync.
 * Events waiting for input data will read from MongoDB event log (capped collection) and transfer 
 * actual data from Oracle to MongoDB
 * @author pnilayam
 *
 */
public class O2MSyncDataLoader implements Runnable {
	private final Logger logger = Logger.getLogger(getClass());
	private ObjectId eventId;
	private String dbName;
	private String dbUserName;
	private String status;
	private Connection connection;
	private CallableStatement statement;
	private O2MEventLogDao eventLogDao;
	private int retryCount = 0;
	private EventType eventType = EventType.System;
	private final long repeatInterval;
	private final String appName;
	private volatile boolean isCancelled;
	private long waitTime = 1;
	private final CountDownLatch latch = new CountDownLatch(1);

	public O2MSyncDataLoader(long repeatInterval , String appName) {
		super();
		this.repeatInterval = repeatInterval;
		this.appName= appName;
	}

	/**
	 * @return the isCancelled
	 */
	public boolean isCancelled() {
		return isCancelled;
	}

	/**
	 * @param isCancelled the isCancelled to set
	 */
	public void setCancelled(boolean isCancelled) {
		this.isCancelled = isCancelled;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status
	 *            the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @return the eventId
	 */
	public ObjectId getEventId() {
		return eventId;
	}

	/**
	 * @param eventId
	 *            the eventId to set
	 */
	public void setEventId(ObjectId eventId) {
		this.eventId = eventId;
	}

	/**
	 * @return the eventType
	 */
	public EventType getEventType() {
		return eventType;
	}

	/**
	 * @param eventType
	 *            the eventType to set
	 */
	public void setEventType(EventType eventType) {
		this.eventType = eventType;
	}

	/**
	 * @return the dbName
	 */
	public String getDbName() {
		return dbName;
	}

	/**
	 * @param dbName
	 *            the dbName to set
	 */
	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	/**
	 * @return the dbUserName
	 */
	public String getDbUserName() {
		return dbUserName;
	}

	/**
	 * @param dbUserName
	 *            the dbUserName to set
	 */
	public void setDbUserName(String dbUserName) {
		this.dbUserName = dbUserName;
	}

	@Override
	public void run() {
		logger.info("Data Loader Started");
		try {
			eventLogDao = new O2MEventLogDao();
			Timer timer = null;
			timer = new Timer("Dataloader", true);
			timer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					try {
						if (retryCount >= 5 || isCancelled) {
							latch.countDown();
							cancel();	
							return;
						}
						if (connection == null || connection.isClosed()) {
							getResources();
							retryCount = 0;
							waitTime=30000;
						}
						statement = connection.prepareCall("{call XXCQO_SYNC_EVENT_LOG_PKG.LOAD_EVENT_LOG(?,?,?,?)}");
						statement.setString(1, appName);
						statement.registerOutParameter(2, Types.ARRAY, "XXCQO_SYNC_EVENT_LOG_TABLE");
						statement.registerOutParameter(3, Types.INTEGER);
						statement.registerOutParameter(4, Types.VARCHAR);
						load();
					} catch (Exception e) {
						logger.error("Error while loading data ", e);
						DbResourceUtils.closeResources(null, statement, connection);
					}finally{
						DbResourceUtils.closeResources(null, statement, null);
					}
				}
			}, 10, repeatInterval);

			latch.await();
			logger.info("Data Loader Terminated");
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			DbResourceUtils.closeResources(null, statement, connection);
			statement = null;
			connection = null;
			eventLogDao = null;
		}
	}

	public void getResources() throws SQLException, SyncError {
		try {
			retryCount++;
			waitTime*=retryCount;
			Thread.sleep(waitTime);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		DbResourceUtils.closeResources(null, statement, connection);
		connection = DBCacheManager.INSTANCE.getCachedOracleConnection(dbName, dbUserName);
	}

	public void load() throws SQLException {
		statement.execute();
		logger.info("Statement Executed");
		Array eventLogOraArray = statement.getArray(2);
		List<O2MSyncEventLog> eventLogList = readData(eventLogOraArray);
		eventLogDao.writeData(eventLogList);
		logger.info("Data Inserted");		
	}

	public List<O2MSyncEventLog> readData(Array oraArray) throws SQLException {
		List<O2MSyncEventLog> eventLogList = null;
		Object[] eventLogArr = (Object[]) oraArray.getArray();
		if (eventLogArr != null && eventLogArr.length > 0) {
			eventLogList = new ArrayList<O2MSyncEventLog>();
			O2MSyncEventLog eventLog = null;
			for (int i = 0; i < eventLogArr.length; i++) {
				eventLog = new O2MSyncEventLog();
				STRUCT attrArrStruct = (STRUCT) eventLogArr[i];
				Object [] attrArr = attrArrStruct.getAttributes();
				eventLog.setLogId(new ObjectId());
				eventLog.setEventId(String.valueOf(attrArr[0]).trim());
				eventLog.setOperation(String.valueOf(attrArr[1]));
				java.sql.Timestamp crOn = (java.sql.Timestamp) attrArr[2];
				if (crOn != null) {
					eventLog.setCrOn(new Date(crOn.getTime()));
				} else {
					eventLog.setCrOn(new Date());
				}
				ARRAY oraFilters = (ARRAY) attrArr[6];
				Object[] filterStructArr = (Object[]) oraFilters.getOracleArray();
				if (filterStructArr != null && filterStructArr.length > 0) {
					List<O2MSyncEventInfo> filterList = new ArrayList<O2MSyncEventInfo>();
					O2MSyncEventInfo filter = null;
					for (int j = 0 ; j < filterStructArr.length; j++) {
						STRUCT filterAttrArrStruct = (STRUCT) filterStructArr[j];
						Object[] filterAttrArr = filterAttrArrStruct.getAttributes();
						filter = new O2MSyncEventInfo();
						filter.setTableName(String.valueOf(filterAttrArr[0]).trim());
						filter.setColumnName(String.valueOf(filterAttrArr[1]).trim());
						filter.setColumnValue(String.valueOf(filterAttrArr[2]));
						filterList.add(filter);
					}
					eventLog.setEventFilters(filterList);
				}
				eventLogList.add(eventLog);
			}
		}
		return eventLogList;
	}
}
