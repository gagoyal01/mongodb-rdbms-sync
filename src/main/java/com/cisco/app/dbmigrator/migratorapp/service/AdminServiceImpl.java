package com.cisco.app.dbmigrator.migratorapp.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.config.SyncConfig;
import com.cisco.app.dbmigrator.migratorapp.constants.SyncConstants;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncConnectionDao;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncNodeDetailsDao;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncUserDao;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncConnectionInfo;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncUser;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncUser.UserDetailAttributes;
import com.cisco.app.dbmigrator.migratorapp.logging.entity.SyncNodeDetails;
import com.cisco.app.dbmigrator.migratorapp.utilities.encrypt.EncryptorDecryptor;

public class AdminServiceImpl implements AdminService{
	private static final String SCHEMAS = "schemas";
	private static final String ID = "_id";
	private static final String DBNAME = "dbName";
	private static final String DBTYPE = "dbType";
	private static final String USERNAME = "userName";
	private static final String PASSWORD = "password";
	private static final String HOSTTOPORTMAP = "hostToPortMap";
	private static final String HOST = "host";
	private static final String PORT = "port";
	private SyncNodeDetailsDao syncNodeDetailsDao = null;
	
	private SyncUserDao syncUserDao = null; 
	
	private SyncConnectionDao connectionDao = null;
	
	public SyncNodeDetailsDao getSyncNodeDetailsDao() {
		return syncNodeDetailsDao;
	}

	public void setSyncNodeDetailsDao(SyncNodeDetailsDao syncNodeDetailsDao) {
		this.syncNodeDetailsDao = syncNodeDetailsDao;
	}
	
	public SyncUserDao getSyncUserDao() {
		return syncUserDao;
	}

	public void setSyncUserDao(SyncUserDao syncUserDao) {
		this.syncUserDao = syncUserDao;
	}

	public SyncConnectionDao getConnectionDao() {
		return connectionDao;
	}

	public void setConnectionDao(SyncConnectionDao connectionDao) {
		this.connectionDao = connectionDao;
	}

	@Override
	public List<SyncNodeDetails> getNodeDetails() {
		return syncNodeDetailsDao.getNodeDetails(getLifeCycle());
	}

	private String getLifeCycle() {
		return SyncConfig.INSTANCE.getDbProperty(SyncConstants.LIFE)!=null?SyncConfig.INSTANCE.getDbProperty(SyncConstants.LIFE):"dev";
	}

	private static Document parsefromJson(String jsonString) {
		return Document.parse(jsonString);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void saveUserDetails(String jsonStr) {
		Document document = parsefromJson(jsonStr);
		SyncUser user = new SyncUser();
		Map<String,Set<String>> sourceDbMap = new HashMap<String,Set<String>>();
		Map<String,Set<String>> targetDbMap = new HashMap<String,Set<String>>();
		user.setUserid(document.getString(String.valueOf(ID)));
		List<Document> sourceDbList = (List<Document>) document.get(String.valueOf(UserDetailAttributes.sourceDbMap));
		for (Document doc : sourceDbList) {
			Set<String> schemaSet = new HashSet<String>((List<String>)doc.get(SCHEMAS));
			sourceDbMap.put(doc.getString(DBNAME), schemaSet);
		}
		user.setSourceDbMap(sourceDbMap);
		List<Document> targetDbList = (List<Document>) document.get(String.valueOf(UserDetailAttributes.targetDbMap));
		for (Document doc : targetDbList) {
			Set<String> schemaSet = new HashSet<String>((List<String>)doc.get(SCHEMAS));
			targetDbMap.put(doc.getString(DBNAME), schemaSet);
		}
		user.setTargetDbMap(targetDbMap);
		user.setUserRoles((List<String>) document.get(UserDetailAttributes.roles));
		syncUserDao.updateUser(user);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void saveconnectionInfo(String jsonStr) {
		Document document = parsefromJson(jsonStr);
		SyncConnectionInfo connectionInfo = new SyncConnectionInfo();
		if(document.getObjectId(ID)!=null){
			connectionInfo.setConnectionId(document.getObjectId(ID));
		}
		String dbType = document.getString(DBTYPE);
		connectionInfo.setDbType(dbType);
		connectionInfo.setDbName(document.getString(DBNAME));
		connectionInfo.setUserName(document.getString(USERNAME));
		connectionInfo.setPassword(document.getString(PASSWORD));
		if(dbType!=null && dbType.equalsIgnoreCase("Mongo")){
			Map<String,Double> hostToPortMap = new HashMap<String,Double>();
			List<Document> hostToPort = (List<Document>) document.get(HOSTTOPORTMAP);
			for(Document doc : hostToPort){
				hostToPortMap.put(doc.getString(HOST), doc.getDouble(PORT));
			}
			connectionInfo.setHostToPortMap(hostToPortMap);
		}
		connectionDao.updateConnection(connectionInfo);
	}

	@Override
	public List<SyncConnectionInfo> getconnectionDetails() {
		return connectionDao.getConnectionDetails();
	}

	@Override
	public void deleteUser(String userId) {
		syncUserDao.deleteUser(userId);
	}

	@Override
	public void deleteConnection(ObjectId id) {
		connectionDao.deleteConnetion(id);
	}

	@Override
	public void encryptPass() throws Exception {
		List<SyncConnectionInfo> passwordList = connectionDao.getPassword();
		for(SyncConnectionInfo connectionInfo : passwordList){
			String pass = connectionInfo.getPassword();
			ObjectId id = connectionInfo.getConnectionId();
			if(pass!=null){
				byte[] encPass = EncryptorDecryptor.encrypt(pass,null);
				connectionDao.setEncryptedPassword(id, encPass);
			}
		}
	}

	@Override
	public SyncConnectionInfo searchconnection(String dbName, String userName, String dbType) {
		return connectionDao.searchConnection(dbName, userName, dbType);
	}

	@Override
	public SyncUser getUser(String userId) {
		return syncUserDao.getUser(userId);
	}
}
