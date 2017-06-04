package com.cisco.app.dbmigrator.migratorapp.core.event;

import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.bson.BsonDocument;
import org.bson.BsonDocumentWrapper;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.constants.SyncConstants;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncEventDao;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncMapDao;

/**
 * Base Class for all SyncEvents.
 * 
 * @author pnilayam
 *
 * @param <T> Extending classes should use any relevant Class Type as the Param. 
 * 			  Blocking Queue to hold data will be initialized with this class type.
 */
public abstract class SyncEvent<T> implements Bson, Runnable{
	protected ObjectId eventId;
	protected ObjectId parentEventId;
	protected String eventName;
	protected EventType eventType;
	protected String comments;
	protected String createdBy;
	protected Date createdOn;
	protected String approvedBy;
	protected Date approvedOn;
	protected String status;
	protected ObjectId mapId;
	protected String mapName;
	protected SyncMarker marker;
	protected List<SyncError> errorList;
	protected BlockingQueue<T> dataBuffer;
	protected SyncMapDao mappingDao;
	protected SyncEventDao eventDao;
	protected boolean isRetry;
	protected int batchSize = SyncConstants.DEFAULT_BATCH_SIZE;
	protected String notifIds;
	
	/**
	 * @return the notifIds
	 */
	public String getNotifIds() {
		return notifIds;
	}
	/**
	 * @param notifIds the notifIds to set
	 */
	public void setNotifIds(String notifIds) {
		this.notifIds = notifIds;
	}
	/**
	 * @return the isRetry
	 */
	public boolean isRetry() {
		return isRetry;
	}
	/**
	 * @param isRetry the isRetry to set
	 */
	public void setRetry(boolean isRetry) {
		this.isRetry = isRetry;
	}
	public List<SyncError> getErrorList() {
		return errorList;
	}
	public void setErrorList(List<SyncError> errorList) {
		this.errorList = errorList;
	}
	public ObjectId getParentEventId() {
		return parentEventId;
	}
	public void setParentEventId(ObjectId parentEventId) {
		this.parentEventId = parentEventId;
	}
	public int getBatchSize() {
		return batchSize;
	}
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}
	public SyncMapDao getMappingDao() {
		return mappingDao;
	}
	public void setMappingDao(SyncMapDao mappingDao) {
		this.mappingDao = mappingDao;
	}
	public ObjectId getEventId() {
		return eventId;
	}
	public void setEventId(ObjectId eventId) {
		this.eventId = eventId;
	}
	public String getEventName() {
		return eventName;
	}
	public void setEventName(String eventName) {
		this.eventName = eventName;
	}
	public EventType getEventType() {
		return eventType;
	}
	public void setEventType(EventType eventType) {
		this.eventType = eventType;
	}
	public String getComments() {
		return comments;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}
	public String getCreatedBy() {
		return createdBy;
	}
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}
	public Date getCreatedOn() {
		return createdOn;
	}
	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}
	public String getApprovedBy() {
		return approvedBy;
	}
	public void setApprovedBy(String approvedBy) {
		this.approvedBy = approvedBy;
	}
	public Date getApprovedOn() {
		return approvedOn;
	}
	public void setApprovedOn(Date approvedOn) {
		this.approvedOn = approvedOn;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public ObjectId getMapId() {
		return mapId;
	}
	public void setMapId(ObjectId mapId) {
		this.mapId = mapId;
	}
	public String getMapName() {
		return mapName;
	}
	public void setMapName(String mapName) {
		this.mapName = mapName;
	}
	public SyncMarker getMarker() {
		return marker;
	}
	public void setMarker(SyncMarker marker) {
		this.marker = marker;
	}
	public BlockingQueue<T> getDataBuffer() {
		return dataBuffer;
	}
	public void setDataBuffer(BlockingQueue<T> dataBuffer) {
		this.dataBuffer = dataBuffer;
	}
	@SuppressWarnings("rawtypes")
	@Override
	public <TDocument> BsonDocument toBsonDocument(Class<TDocument> arg0, CodecRegistry codecRegistry) {
		return new BsonDocumentWrapper<SyncEvent>(this, codecRegistry.get(SyncEvent.class));
	}
}
