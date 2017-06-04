package com.cisco.app.dbmigrator.migratorapp.logging.entities;

import java.util.ArrayList;
import java.util.List;

import org.bson.BsonDocument;
import org.bson.BsonDocumentWrapper;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

public class SyncNode implements Bson{
	private ObjectId id;
	private String hostName;
	private String nodeName;
	private String jvmName;
	private String state;
	private String lifeCycle;
	private int concurrencyLevel;
	private long totalHeapSize;
	private long usedHeapSize;
	private List<ObjectId> eventList;
	private List<String> eventTypes;
	private List<ObjectId> systemEvents;
	private String UUID;
	private long lastPingTime;
	private long failureTime;
	/**
	 * @return the failureTime
	 */
	public long getFailureTime() {
		return failureTime;
	}
	/**
	 * @param failureTime the failureTime to set
	 */
	public void setFailureTime(long failureTime) {
		this.failureTime = failureTime;
	}
	/**
	 * @return the lastPingTime
	 */
	public long getLastPingTime() {
		return lastPingTime;
	}
	/**
	 * @param lastPingTime the lastPingTime to set
	 */
	public void setLastPingTime(long lastPingTime) {
		this.lastPingTime = lastPingTime;
	}
	/**
	 * @return the uUID
	 */
	public String getUUID() {
		return UUID;
	}
	/**
	 * @param uUID the uUID to set
	 */
	public void setUUID(String uUID) {
		UUID = uUID;
	}
	/**
	 * @return the systemEvents
	 */
	public List<ObjectId> getSystemEvents() {
		if(systemEvents==null){
			systemEvents= new ArrayList<ObjectId>();
		}
		return systemEvents;
	}
	/**
	 * @param systemEvents the systemEvents to set
	 */
	public void setSystemEvents(List<ObjectId> systemEvents) {
		this.systemEvents = systemEvents;
	}
	/**
	 * @return the eventTypes
	 */
	public List<String> getEventTypes() {
		return eventTypes;
	}
	/**
	 * @param eventTypes the eventTypes to set
	 */
	public void setEventTypes(List<String> eventTypes) {
		this.eventTypes = eventTypes;
	}
	public long getTotalHeapSize() {
		return totalHeapSize;
	}
	public void setTotalHeapSize(long totalHeapSize) {
		this.totalHeapSize = totalHeapSize;
	}
	public long getUsedHeapSize() {
		return usedHeapSize;
	}
	public void setUsedHeapSize(long usedHeapSize) {
		this.usedHeapSize = usedHeapSize;
	}
	public List<ObjectId> getEventList() {
		return eventList;
	}
	public void setEventList(List<ObjectId> eventList) {
		this.eventList = eventList;
	}
	public String getLifeCycle() {
		return lifeCycle;
	}
	public void setLifeCycle(String lifeCycle) {
		this.lifeCycle = lifeCycle;
	}
	public ObjectId getId() {
		return id;
	}
	public void setId(ObjectId id) {
		this.id = id;
	}
	public String getHostName() {
		return hostName;
	}
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	public String getNodeName() {
		return nodeName;
	}
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}
	public String getJvmName() {
		return jvmName;
	}
	public void setJvmName(String jvmName) {
		this.jvmName = jvmName;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public int getConcurrencyLevel() {
		return concurrencyLevel;
	}
	public void setConcurrencyLevel(int concurrencyLevel) {
		this.concurrencyLevel = concurrencyLevel;
	}
	@Override
	public <TDocument> BsonDocument toBsonDocument(Class<TDocument> arg0, CodecRegistry codecRegistry) {
		return new BsonDocumentWrapper<SyncNode>(this, codecRegistry.get(SyncNode.class));
	}
}
