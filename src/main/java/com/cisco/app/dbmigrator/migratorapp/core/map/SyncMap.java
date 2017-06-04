package com.cisco.app.dbmigrator.migratorapp.core.map;

import java.util.Date;

import org.bson.BsonDocument;
import org.bson.BsonDocumentWrapper;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

public abstract class SyncMap implements Bson{
	protected ObjectId mapId;
	protected String mapName;
	protected String createdBy;
	protected String approvedBy;
	protected Date createdOn;
	protected Date approvedOn;
	protected String comments;
	protected String sourceDbName;
	protected String sourceUserName;
	protected String targetDbName;
	protected String targetUserName;
	protected MapType mapType;
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
	public String getCreatedBy() {
		return createdBy;
	}
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}
	public String getApprovedBy() {
		return approvedBy;
	}
	public void setApprovedBy(String approvedBy) {
		this.approvedBy = approvedBy;
	}
	public Date getCreatedOn() {
		return createdOn;
	}
	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}
	public Date getApprovedOn() {
		return approvedOn;
	}
	public void setApprovedOn(Date approvedOn) {
		this.approvedOn = approvedOn;
	}
	public String getComments() {
		return comments;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}
	public String getSourceDbName() {
		return sourceDbName;
	}
	public void setSourceDbName(String sourceDbName) {
		this.sourceDbName = sourceDbName;
	}
	public String getTargetDbName() {
		return targetDbName;
	}
	public void setTargetDbName(String targetDbName) {
		this.targetDbName = targetDbName;
	}
	public String getSourceUserName() {
		return sourceUserName;
	}
	public void setSourceUserName(String sourceUserName) {
		this.sourceUserName = sourceUserName;
	}
	public String getTargetUserName() {
		return targetUserName;
	}
	public void setTargetUserName(String targetUserName) {
		this.targetUserName = targetUserName;
	}
	public MapType getMapType() {
		return mapType;
	}
	public void setMapType(MapType mapType) {
		this.mapType = mapType;
	}
	public void setMapType(String mapType) {
		this.mapType = MapType.valueOf(mapType);
	}	
	@Override
	public <TDocument> BsonDocument toBsonDocument(Class<TDocument> arg0, CodecRegistry codecRegistry) {
		return new BsonDocumentWrapper<SyncMap>(this, codecRegistry.get(SyncMap.class));
	}
}
