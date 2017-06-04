package com.cisco.app.dbmigrator.migratorapp.service;

import java.util.List;

import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncConnectionInfo;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncUser;
import com.cisco.app.dbmigrator.migratorapp.logging.entity.SyncNodeDetails;

public interface AdminService {

	public List<SyncNodeDetails> getNodeDetails();
	
	public void saveUserDetails(String jsonStr);
	
	public void saveconnectionInfo(String jsonStr);
	
	public List<SyncConnectionInfo> getconnectionDetails();
	
	public void deleteUser(String userId);
	
	public void deleteConnection(ObjectId id);
	
	public void encryptPass() throws Exception;
	
	public SyncConnectionInfo searchconnection(String dbName,String userName,String dbType);
	
	public SyncUser getUser(String userId);
}
