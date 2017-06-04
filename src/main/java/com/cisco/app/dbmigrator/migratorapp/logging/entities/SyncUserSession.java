package com.cisco.app.dbmigrator.migratorapp.logging.entities;

import java.util.Date;

import org.bson.BsonDocument;
import org.bson.BsonDocumentWrapper;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

public class SyncUserSession implements Bson{
	
	private String sessionId;
	private String userid;
	private String clientIPAddress;
	private String clientHostName;
	private String clientAgent;
	private Date loginTime;
	public enum SessionAttributes{
		_id,userid,clientIPAddress,clientHostName,clientAgent,loginTime;
	}
	public String getSessionId() {
		return sessionId;
	}
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	public String getUserid() {
		return userid;
	}
	public Date getLoginTime() {
		return loginTime;
	}
	public void setLoginTime(Date loginTime) {
		this.loginTime = loginTime;
	}
	public void setUserid(String userid) {
		this.userid = userid;
	}
	public String getClientIPAddress() {
		return clientIPAddress;
	}
	public void setClientIPAddress(String clientIPAddress) {
		this.clientIPAddress = clientIPAddress;
	}
	public String getClientHostName() {
		return clientHostName;
	}
	public void setClientHostName(String clientHostName) {
		this.clientHostName = clientHostName;
	}
	public String getClientAgent() {
		return clientAgent;
	}
	public void setClientAgent(String clientAgent) {
		this.clientAgent = clientAgent;
	}
	@Override
	public <TDocument> BsonDocument toBsonDocument(Class<TDocument> arg0,
			CodecRegistry codecRegistry) {
		return new BsonDocumentWrapper<SyncUserSession>(this,
				codecRegistry.get(SyncUserSession.class));
	}
}
