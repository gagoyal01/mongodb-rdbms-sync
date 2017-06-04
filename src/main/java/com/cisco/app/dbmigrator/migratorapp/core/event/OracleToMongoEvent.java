package com.cisco.app.dbmigrator.migratorapp.core.event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWrapper;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.core.job.NodeBalancer;
import com.cisco.app.dbmigrator.migratorapp.core.job.SyncStatus;
import com.cisco.app.dbmigrator.migratorapp.core.map.OracleToMongoMap;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoAttribute;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoEntity;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoObject;
import com.cisco.app.dbmigrator.migratorapp.core.thread.OrclToMngReader;
import com.cisco.app.dbmigrator.migratorapp.core.thread.OrclToMngWriter;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncEventDao;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncMapDao;
import com.cisco.app.dbmigrator.migratorapp.mail.Mailer;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.Literal;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.MatchAble;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.MatchOperator;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.queries.SelectQueryBuilder;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.LogicalOperation;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.MatchOperation;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.SQLFilters;
import com.cisco.app.dbmigrator.migratorapp.utilities.cache.DBCacheManager;
import com.cisco.app.dbmigrator.migratorapp.utilities.mongo.MongoDbUtilities;
import com.cisco.app.dbmigrator.migratorapp.utilities.oracle.DbResourceUtils;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;

/**
 * Runnable Class to Process OracleDB to MongoDB data migration(one time event).
 * 
 * @author pnilayam
 *
 */
@SuppressWarnings("rawtypes")
public class OracleToMongoEvent extends SyncEvent<List<Document>>implements Cloneable {

	private static Logger logger = Logger.getLogger(OracleToMongoEvent.class);
	private String collectionName;
	private boolean saveNulls;
	private OracleParallelReadInfo parallelReadInfo;
	private SQLFilters rangeFilter;
	//private boolean process;
	private final CountDownLatch latch= new CountDownLatch(2);
	public SQLFilters getRangeFilter() {
		return rangeFilter;
	}

	public void setRangeFilter(SQLFilters rangeFilter) {
		this.rangeFilter = rangeFilter;
	}

/*	*//**
	 * @return the process
	 *//*
	public boolean isProcess() {
		return process;
	}

	*//**
	 * @param process the process to set
	 *//*
	public void setProcess(boolean process) {
		this.process = process;
	}*/

	public void run() {
		try {
			//Mailer.sendmail(this, null, null, Mailer.STARTED);
			if (marker == null) {
				marker = new SyncMarker();
			}
			if (parentEventId == null) {
				parentEventId = eventId;
			}
			marker.setStartTime(new Date());
			dataBuffer = new LinkedBlockingQueue<List<Document>>(batchSize);
			eventDao = new SyncEventDao();
			
			logger.info("OracleToMongoBasicEvent Thread Started at " + System.currentTimeMillis());

			OracleToMongoMap map = (OracleToMongoMap) new SyncMapDao().getMapping(mapId);
			if (isRetry) {
				clearOldData(map);
			}
			MongoObject mongoObject = map.getMapObject();
			if (rangeFilter != null) {
				if (mongoObject.getFilters() == null) {
					mongoObject.setFilters(rangeFilter);
				} else {
					mongoObject.getFilters().AND(rangeFilter);
				}
			}
			getStats(mongoObject, map.getSourceDbName(), map.getSourceUserName());
			if (marker.getTotalRows() != 0) {
				eventDao.updateMarker(eventId, marker);
				Thread reader;
				reader = new Thread(new OrclToMngReader(map.getMapObject(), batchSize, dataBuffer, marker, saveNulls,
						map.getSourceDbName(), map.getSourceUserName(), eventId, latch));
				reader.setName(eventName + "-Reader");
				reader.start();

				Thread writer;
				writer = new Thread(new OrclToMngWriter(dataBuffer, map.getTargetDbName(), map.getTargetUserName(),
						marker, eventId, collectionName , latch));

				writer.setName(eventName + "-Writer");
				writer.start();
			} else {
				marker.setEndTime(new Date());
				eventDao.updateMarker(eventId, marker);
				NodeBalancer.INSTANCE.markEventAsCompleted(eventId);
			}
			latch.await();
		} catch (SyncError e) {
			e.printStackTrace();
			e.setThreadName(eventName);
			eventDao.pushError(eventId, e);
			NodeBalancer.INSTANCE.markEventAsFailed(eventId);
			Mailer.sendmail(this, null, e, Mailer.FAILURE);	
		} catch (InterruptedException e) {
			e.printStackTrace();
			SyncError error = new SyncError(e);
			error.setThreadName(eventName);
			eventDao.pushError(eventId, error);
			NodeBalancer.INSTANCE.markEventAsFailed(eventId);
			Mailer.sendmail(this, null, e, Mailer.FAILURE);	
		} finally {
			dataBuffer = null;
			marker = null;
			logger.info("OracleToMongoBasicEvent Thread Completed at " + System.currentTimeMillis());
			//Mailer.sendmail(this, null, null, Mailer.COMPLETED);
		}
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		OracleToMongoEvent clonedEvent = (OracleToMongoEvent) super.clone();
		clonedEvent.setEventId(new ObjectId());
		clonedEvent.setParentEventId(this.getEventId());
		clonedEvent.setCreatedBy("SYSTEM");
		clonedEvent.setCreatedOn(new Date());
		clonedEvent.setParallelReadInfo(null);
		clonedEvent.setStatus(SyncStatus.PENDING);
		clonedEvent.setDataBuffer(null);
		clonedEvent.setMarker(null);
		clonedEvent.setRangeFilter(null);
		return clonedEvent;
	}

	private void getStats(MongoObject mongoObject, String sourceDbName, String sourceSchemaName) throws SyncError {
		SelectQueryBuilder queryBuilder = new SelectQueryBuilder();
		List<MatchAble> bindvalues = new ArrayList<MatchAble>();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		String countQuery = queryBuilder.select().from(mongoObject.getSourceTables().get(0))
				.where(mongoObject.getFilters()).getCountQuery(bindvalues);
		Connection connection = null;
		try {
			connection = DBCacheManager.INSTANCE.getCachedOracleConnection(sourceDbName, sourceSchemaName);
			stmt = connection.prepareStatement(countQuery);
			if (bindvalues != null) {
				for (int index = 0; index < bindvalues.size(); index++) {
					stmt.setObject(index + 1, bindvalues.get(index).getSqlExpressionForMatchable());
				}
			}
			rs = stmt.executeQuery();
			logger.debug("Query Executed to get RowCount");
			rs.next();
			int totalRows = rs.getInt(1);
			logger.info("Rowcount Fecthed : " + totalRows);
			marker.setTotalRows(totalRows);
		} catch (SQLException e) {
			logger.error("Error while getting total count of rows to be processed", e);
			throw new SyncError(e);
		} finally {
			DbResourceUtils.closeResources(rs, stmt, connection);
		}
	}

	public OracleParallelReadInfo getParallelReadInfo() {
		return parallelReadInfo;
	}

	public void setParallelReadInfo(OracleParallelReadInfo parallelReadInfo) {
		this.parallelReadInfo = parallelReadInfo;
	}

	public boolean isSaveNulls() {
		return saveNulls;
	}

	public void setSaveNulls(boolean saveNulls) {
		this.saveNulls = saveNulls;
	}

	public String getCollectionName() {
		return collectionName;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	@Override
	public <TDocument> BsonDocument toBsonDocument(Class<TDocument> arg0, CodecRegistry codecRegistry) {
		return new BsonDocumentWrapper<OracleToMongoEvent>(this, codecRegistry.get(OracleToMongoEvent.class));
	}
	
	
	private void clearOldData(OracleToMongoMap map) throws SyncError {
		try{
			MongoCollection collection =DBCacheManager.INSTANCE.getCachedMongoPool(map.getTargetDbName(), map.getTargetUserName())
					.getDatabase(map.getTargetDbName()).getCollection(collectionName);
			if(parentEventId!=null || parentEventId!=eventId){
				OracleToMongoEvent parentEvent = (OracleToMongoEvent) eventDao.getEvent(parentEventId);
				String mongoAttributeName=null;
				List<MongoEntity> identifiers = map.getMapObject().getIdentifierList();
				//TODO : modify to take care of complex identifiers
				if(identifiers!=null){
					for(MongoEntity entity : identifiers){
						MongoAttribute attribute= (MongoAttribute)entity;
						if(attribute.getMappedOracleColumn().equals(parentEvent.getParallelReadInfo().getRangeColumn())){
							mongoAttributeName=attribute.getAttributeName();
							break;
						}
					}
				}
				if(mongoAttributeName==null){
					List<MongoEntity> attributeList = map.getMapObject().getAttributes();
					for(MongoEntity entity : attributeList){
						if(entity instanceof MongoAttribute){
							MongoAttribute attribute= (MongoAttribute)entity;
							if(attribute.getMappedOracleColumn().equals(parentEvent.getParallelReadInfo())){
								mongoAttributeName=attribute.getAttributeName();
								break;
							}
						}
					}
				}
				

				Bson filter=null;
				if(rangeFilter!=null){
					Bson firstRange = getRangeBson(rangeFilter.getMatchOperation(), mongoAttributeName);
					Bson secondRange = null;
					Set<LogicalOperation> logicalOperations = rangeFilter.getLogicaloperations();
					for(LogicalOperation operation : logicalOperations){
						secondRange = getRangeBson(operation.getMatchOperation(), mongoAttributeName);
						break;//It will have just one operation
					}
					filter = Filters.and(firstRange,secondRange);
				}
				
				DeleteResult result = collection.deleteMany(filter);
				logger.info("Number or old rows deleted for Thread : "+Thread.currentThread().getName()+ " : " + result.getDeletedCount());		
			}else{
				collection.drop();
			}
		} catch(Exception e) {
			throw new SyncError(e);
		}
	}
	
	private Bson getRangeBson(MatchOperation matchOperation , String attributeName){
		Object literalValue=null;
		Bson filter =null;
		if(matchOperation!=null){
			MatchAble rightExpression =matchOperation.getRightExpression();
			if(rightExpression instanceof Literal){
				literalValue = ((Literal)rightExpression).getLiteralValue();
			}else{
				Literal leftExpression =(Literal) matchOperation.getLeftExpression();
				literalValue = leftExpression.getLiteralValue();
			}
			MatchOperator matchOperator = matchOperation.getOperator();
			filter = MongoDbUtilities.getFilterBson(matchOperator, attributeName, literalValue);			
		}
		return filter;
	}
}
