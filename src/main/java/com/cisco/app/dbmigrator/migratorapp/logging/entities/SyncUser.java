package com.cisco.app.dbmigrator.migratorapp.logging.entities;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BsonDocument;
import org.bson.BsonDocumentWrapper;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

public class SyncUser implements Bson{
	public enum UserDetailAttributes{
		_id,lastLoginTime,savedSourceDbList,savedTargetDbList,sourceDbMap,targetDbMap,roles;
	}
	private String userid;
	private Map<String, Set<String>> sourceDbMap;
	private Map<String, Set<String>> targetDbMap;
	private List<String> userRoles;
	public Map<String, Set<String>> getSourceDbMap() {
		return sourceDbMap;
	}
	public void setSourceDbMap(Map<String, Set<String>> sourceDbMap) {
		this.sourceDbMap = sourceDbMap;
	}
	public Map<String, Set<String>> getTargetDbMap() {
		return targetDbMap;
	}
	public void setTargetDbMap(Map<String, Set<String>> targetDbMap) {
		this.targetDbMap = targetDbMap;
	}
	public String getUserid() {
		return userid;
	}
	public void setUserid(String userid) {
		this.userid = userid;
	}
	public List<String> getUserRoles() {
		return userRoles;
	}
	public void setUserRoles(List<String> userRoles) {
		this.userRoles = userRoles;
	}
	@Override
	public <TDocument> BsonDocument toBsonDocument(Class<TDocument> arg0,
			CodecRegistry codecRegistry) {
		return new BsonDocumentWrapper<SyncUser>(this,
				codecRegistry.get(SyncUser.class));
	}
	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder("{ ");
		builder.append(String.valueOf(UserDetailAttributes._id)).append(userid).append(",");
		builder.append(String.valueOf(sourceDbMap)).append(",");
		builder.append(String.valueOf(targetDbMap)).append(",");
		builder.append(String.valueOf(userRoles));
		builder.append(" }");
		return builder.toString();
	}

}
