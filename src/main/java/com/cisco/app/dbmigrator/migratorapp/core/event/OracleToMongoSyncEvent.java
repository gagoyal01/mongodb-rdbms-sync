package com.cisco.app.dbmigrator.migratorapp.core.event;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.bson.Document;

import com.cisco.app.dbmigrator.migratorapp.core.job.NodeBalancer;
import com.cisco.app.dbmigrator.migratorapp.core.map.OracleToMongoMap;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoAttribute;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoObject;
import com.cisco.app.dbmigrator.migratorapp.core.thread.OrclToMngSyncReader;
import com.cisco.app.dbmigrator.migratorapp.core.thread.OrclToMngSyncWriter;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncMapDao;
import com.cisco.app.dbmigrator.migratorapp.mail.Mailer;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.JoinedTable;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleTable;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.OperationsFactory;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.SQLFilters;
/**
 * Runnable Class to Process OracleDB to MongoDB data replication.
 * 
 * @author pnilayam
 *
 */
public class OracleToMongoSyncEvent extends SyncEvent<List<Document>> {
	private List<MongoAttribute> keyAttributes;
	private boolean saveNulls;
	private String collectionName;
	private boolean pollBased;	
	private final CountDownLatch latch = new CountDownLatch(2);
	private O2MSyncPollInfo pollInfo;
	/**
	 * @return the pollInfo
	 */
	public O2MSyncPollInfo getPollInfo() {
		return pollInfo;
	}

	/**
	 * @param pollInfo the pollInfo to set
	 */
	public void setPollInfo(O2MSyncPollInfo pollInfo) {
		this.pollInfo = pollInfo;
	}

	/**
	 * @return the pollBased
	 */
	public boolean isPollBased() {
		return pollBased;
	}

	/**
	 * @param pollBased the pollBased to set
	 */
	public void setPollBased(boolean pollBased) {
		this.pollBased = pollBased;
	}
	/**
	 * @return the saveNulls
	 */
	public boolean isSaveNulls() {
		return saveNulls;
	}

	/**
	 * @param saveNulls the saveNulls to set
	 */
	public void setSaveNulls(boolean saveNulls) {
		this.saveNulls = saveNulls;
	}

	/**
	 * @return the collectionName
	 */
	public String getCollectionName() {
		return collectionName;
	}

	/**
	 * @param collectionName the collectionName to set
	 */
	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	/**
	 * @return the keyAttributes
	 */
	public List<MongoAttribute> getKeyAttributes() {
		return keyAttributes;
	}

	/**
	 * @param keyAttributes
	 *            the keyAttributes to set
	 */
	public void setKeyAttributes(List<MongoAttribute> keyAttributes) {
		this.keyAttributes = keyAttributes;
	}

	@Override
	public void run() {
		Timer timer=null;
		try {
			//Mailer.sendmail(this, null, null, Mailer.STARTED);
			if (marker == null) {
				marker = new SyncMarker();
			}
			if (parentEventId == null) {
				parentEventId = eventId;
			}
			marker.setStartTime(new Date());
			dataBuffer = new LinkedBlockingQueue<List<Document>>(10);
			OracleToMongoMap map = (OracleToMongoMap) new SyncMapDao().getMapping(mapId);
			MongoObject mongoObject = map.getMapObject();
			SQLFilters syncFilter = null;
			if(pollBased){
				syncFilter = getPollingFilter(mongoObject);
			}else{
				syncFilter = getEventLogFilters(mongoObject);
			} 
			mongoObject.setFilters(syncFilter);

			OrclToMngSyncReader reader = new OrclToMngSyncReader(map, saveNulls, dataBuffer, marker, eventId, latch,pollBased, pollInfo);
			final Thread readerThread = new Thread(reader);
			readerThread.setName(eventName + "_SYNC_READER");
			readerThread.start();

			OrclToMngSyncWriter writer = new OrclToMngSyncWriter(map.getTargetDbName(), map.getTargetUserName(),
					collectionName, keyAttributes, dataBuffer, marker, eventId);
			final Thread writerThread = new Thread(writer);
			writerThread.setName(eventName + "_SYNC_WRITER");
			writerThread.start();
			
			timer = new Timer(true);
			timer.scheduleAtFixedRate(new TimerTask() {				
				@Override
				public void run() {
					if(marker.isFailed()){
						latch.countDown();
						latch.countDown();
						readerThread.interrupt();
						writerThread.interrupt();
					}
				}
			}, new Date(), 5000);
			latch.await();
		} catch (Exception e) {
			e.printStackTrace();
			Mailer.sendmail(this, null, e, Mailer.FAILURE);		
		}finally{
			dataBuffer=null;
			timer.cancel();
			NodeBalancer.INSTANCE.markEventAsFailed(eventId);
		}		
	}
	
	private SQLFilters getPollingFilter(MongoObject mongoObject){
		OracleColumn dummyParentColumn = getDummyParentColumn(pollInfo.getPollingColumn(), mongoObject);
		SQLFilters sqlFilter = SQLFilters.getFilter(OperationsFactory.getMatchExpression(pollInfo.getPollingColumn(), dummyParentColumn, "gte"));
		return sqlFilter;
	}

	private SQLFilters getEventLogFilters(MongoObject mongoObject) {
		OracleColumn keyColumn = keyAttributes.get(0).getMappedOracleColumn();
		OracleColumn dummyParentColumn = getDummyParentColumn(keyColumn, mongoObject);
		SQLFilters sqlFilters = SQLFilters
				.getFilter(OperationsFactory.getMatchExpression(keyColumn, dummyParentColumn, "eq"));
		if (keyAttributes.size() > 1) {
			for (int i = 1; i < keyAttributes.size(); i++) {
				OracleColumn nextKeyColumn = keyAttributes.get(i).getMappedOracleColumn();
				dummyParentColumn = getDummyParentColumn(nextKeyColumn, mongoObject);
				sqlFilters.AND(OperationsFactory.getMatchExpression(nextKeyColumn, dummyParentColumn, "eq"));
			}
		}
		return sqlFilters;
	}

	private OracleColumn getDummyParentColumn(OracleColumn keyColumn, MongoObject mongoObject) {
		OracleColumn dummyParentColumn = new OracleColumn();
		dummyParentColumn.setParentColumn(true);
		String tableName = getOracleTableName(mongoObject.getSourceTables().get(0), keyColumn);
		dummyParentColumn.setTableAlias(tableName);
		dummyParentColumn.setColumnName(keyColumn.getColumnName());
		dummyParentColumn.setColumnType(keyColumn.getColumnType());
		return dummyParentColumn;
	}

	private String getOracleTableName(OracleTable sourceTable, OracleColumn keyColumn) {
		String tableName = null;
		if (sourceTable.getColumns().contains(keyColumn)) {
			tableName = sourceTable.getTableName();
		}
		if (tableName == null && sourceTable.getJoinedTables() != null && !sourceTable.getJoinedTables().isEmpty()) {
			for (JoinedTable joinedTable : sourceTable.getJoinedTables()) {
				tableName = getOracleTableName(joinedTable.getTable(), keyColumn);
				if (tableName != null) {
					break;
				}
			}
		}
		return tableName;
	}
	
	public static final class O2MSyncPollInfo{
		private Date lastReadTime;
		private int interval;
		private String timeUnit;
		private OracleColumn pollingColumn;
		/**
		 * @return the lastReadTime
		 */
		public Date getLastReadTime() {
			return lastReadTime;
		}
		/**
		 * @param lastReadTime the lastReadTime to set
		 */
		public void setLastReadTime(Date lastReadTime) {
			this.lastReadTime = lastReadTime;
		}
		/**
		 * @return the interval
		 */
		public int getInterval() {
			return interval;
		}
		/**
		 * @param interval the interval to set
		 */
		public void setInterval(int interval) {
			this.interval = interval;
		}
		/**
		 * @return the timeUnit
		 */
		public String getTimeUnit() {
			return timeUnit;
		}
		/**
		 * @param timeUnit the timeUnit to set
		 */
		public void setTimeUnit(String timeUnit) {
			this.timeUnit = timeUnit;
		}
		/**
		 * @return the pollingColumn
		 */
		public OracleColumn getPollingColumn() {
			return pollingColumn;
		}
		/**
		 * @param pollingColumn the pollingColumn to set
		 */
		public void setPollingColumn(OracleColumn pollingColumn) {
			this.pollingColumn = pollingColumn;
		}
	}
}