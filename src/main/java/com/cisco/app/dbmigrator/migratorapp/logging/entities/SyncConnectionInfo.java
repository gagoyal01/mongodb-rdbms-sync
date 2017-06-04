package com.cisco.app.dbmigrator.migratorapp.logging.entities;

import java.util.Map;

import org.bson.BsonDocument;
import org.bson.BsonDocumentWrapper;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

public class SyncConnectionInfo implements Bson{
	private ObjectId connectionId;
	private String connectionName;
	private String dbType;
	private String dbName;
	private String userName; 
	private String password;
	private Map<String, Double> hostToPortMap;
	private String url;
	private Map<String,String> esSettings;
	public Map<String, String> getEsSettings() {
		return esSettings;
	}
	public void setEsSettings(Map<String, String> esSettings) {
		this.esSettings = esSettings;
	}
	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}
	/**
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}
	public enum ConnectionInfoAttributes{
		_id,connectionName,isDefaultConnection,dbType,dbName,userName,password,hostToPortMap,host,port,url;
	}
	
	public ObjectId getConnectionId() {
		return connectionId;
	}
	public void setConnectionId(ObjectId connectionId) {
		this.connectionId = connectionId;
	}
	public String getConnectionName() {
		return connectionName;
	}
	public void setConnectionName(String connectionName) {
		this.connectionName = connectionName;
	}
	public String getDbType() {
		return dbType;
	}
	public void setDbType(String dbType) {
		this.dbType = dbType;
	}
	public String getDbName() {
		return dbName;
	}
	public void setDbName(String dbName) {
		this.dbName = dbName;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public Map<String, Double> getHostToPortMap() {
		return hostToPortMap;
	}
	public void setHostToPortMap(Map<String, Double> hostToPortMap) {
		this.hostToPortMap = hostToPortMap;
	}
	@Override
	public <TDocument> BsonDocument toBsonDocument(Class<TDocument> arg0,
			CodecRegistry codecRegistry) {
		System.out.println("toBsonDocument called");
		return new BsonDocumentWrapper<SyncConnectionInfo>(this,
				codecRegistry.get(SyncConnectionInfo.class));
	}
	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder("{ ");
		builder.append(String.valueOf(ConnectionInfoAttributes._id)).append(connectionId).append(",");
		builder.append(String.valueOf(ConnectionInfoAttributes.connectionName)).append(connectionName).append(",");
		builder.append(String.valueOf(ConnectionInfoAttributes.dbType)).append(dbType);
		builder.append(String.valueOf(ConnectionInfoAttributes.dbName)).append(dbName).append(",");
		builder.append(String.valueOf(ConnectionInfoAttributes.userName)).append(userName);
		return builder.toString();
	}
}
