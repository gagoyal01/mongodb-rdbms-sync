package com.cisco.app.dbmigrator.migratorapp.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cisco.app.dbmigrator.migratorapp.constants.SyncConstants;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncEvent;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncAttrs;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncMapAndEventEncoder;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncNodeEncoder;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncConnectionInfo;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncUser;
import com.cisco.app.dbmigrator.migratorapp.logging.entity.SyncNodeDetails;
import com.cisco.app.dbmigrator.migratorapp.service.AdminService;
import com.cisco.app.dbmigrator.migratorapp.utilities.mongo.MongoDbUtilities;
import com.cisco.app.dbmigrator.migratorapp.utilities.oracle.OracleDBUtilities;

@Controller
public class AdminController {
	private static final Logger logger = Logger.getLogger(AdminController.class);
	private static final String USER_ID = "userId";
	private static final String ID = "id";
	private static final String USERDETAILSSTR = "userDetailsStr";
	private static final String CONNSTR = "connStr";
	private static final String DBNAME = "dbName";
	private static final String DBTYPE = "dbType";
	private static final String USERNAME = "userName";
	private static final String PASSWORD = "password";
	private static final String HOSTTOPORTMAP = "hostToPortMap";
	private static final String PORT = "port";
	private static final String CONNTYPE = "connType";
	private static final String SAVE = "save";
	private static final String TEST = "test";
	
	
	@Autowired
	private AdminService adminService;
	
	public AdminService getAdminService() {
		return adminService;
	}

	public void setAdminService(AdminService adminService) {
		this.adminService = adminService;
	}

	@SuppressWarnings({ "static-access", "rawtypes" })
	@RequestMapping(value = "/getNodeDetails")
	public void getNodeDetails(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.info("Inside getNodeDetails");
		String nodeDetailsStr = SyncConstants.EMPTY_STRING;
		List<SyncNodeDetails> nodesList = adminService.getNodeDetails();
		JSONArray array = new JSONArray();
		JSONObject nodeObj = null;
		if(nodesList!=null && !nodesList.isEmpty()){
			for(SyncNodeDetails nodeDet : nodesList){
				JSONArray eveArr = null;
				nodeObj = new JSONObject(new SyncNodeEncoder().getNodeJson(nodeDet.getNode()));
				JSONObject eventObj = null;
				if(nodeDet.getEvent()!=null && !nodeDet.getEvent().isEmpty()){
					eveArr = new JSONArray();
					String str = "";
					for(SyncEvent event : nodeDet.getEvent()){
						str = new SyncMapAndEventEncoder().getEventJson(event);
						eventObj = new JSONObject(str);
						eveArr.put(eventObj);
					}
				}
				nodeObj.put("events", eveArr);
				array.put(nodeObj);
			}
		nodeDetailsStr = array.toString();
		}
		response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
		response.getWriter().println(nodeDetailsStr);
		logger.info("getNodeDetails Completed");
	}
	
	@RequestMapping(value="saveUserDetails")
	public void saveUserDetails(HttpServletRequest request,HttpServletResponse response) throws IOException{
		logger.info("Inside saveUserDetails");
		String jsonStr = (request.getParameter(USERDETAILSSTR)==null || request.getParameter(USERDETAILSSTR).equalsIgnoreCase(SyncConstants.EMPTY_STRING))
				? SyncConstants.EMPTY_STRING : request.getParameter(USERDETAILSSTR);
		adminService.saveUserDetails(jsonStr);
		response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
		response.getWriter().println(jsonStr);
		logger.info("saveUserDetails Completed");
	}
	
	@RequestMapping(value="saveConnectionInfo")
	public void saveConnectionInfo(HttpServletRequest request,HttpServletResponse response){
		logger.info("Inside saveConnectionInfo");
		String jsonStr = (request.getParameter(CONNSTR)==null || request.getParameter(CONNSTR).equalsIgnoreCase(SyncConstants.EMPTY_STRING))
		? SyncConstants.EMPTY_STRING : request.getParameter(CONNSTR);
		adminService.saveconnectionInfo(jsonStr);
		logger.info("saveConnectionInfo Completed");
	}
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value="testAndSaveconnection")
	public String testAndSaveconnection(HttpServletRequest request,HttpServletResponse response) throws IOException{
		logger.info("Inside testAndSaveconnection");
		 String connectionStr = "NotConnected";
		 boolean flag = false;
		 String jsonStr = (request.getParameter(CONNSTR)==null || request.getParameter(CONNSTR).equalsIgnoreCase(SyncConstants.EMPTY_STRING))
					? SyncConstants.EMPTY_STRING : request.getParameter(CONNSTR);
		 Document document = parsefromJson(jsonStr);
			SyncConnectionInfo connectionInfo = new SyncConnectionInfo();
			String dbType = document.getString(DBTYPE);
			connectionInfo.setDbType(dbType);
			connectionInfo.setDbName(document.getString(DBNAME));
			connectionInfo.setUserName(document.getString(USERNAME));
			connectionInfo.setPassword(document.getString(PASSWORD));
			if(dbType.equalsIgnoreCase("Mongo")){
				Map<String,Double> hostToPortMap = new HashMap<String,Double>();
				List<Document> hostToPort = (List<Document>) document.get(HOSTTOPORTMAP);
				for(Document doc : hostToPort){
					hostToPortMap.put(doc.getString(SyncAttrs.HOST), doc.getDouble(PORT));
				}
				connectionInfo.setHostToPortMap(hostToPortMap);
				flag = MongoDbUtilities.getMongoClient(connectionInfo);
			}else{
				flag = OracleDBUtilities.testOracleConnection(connectionInfo);
			}
			if(flag && document.getString(CONNTYPE).toString().equalsIgnoreCase(SAVE)){
				saveConnectionInfo(request, response);
				connectionStr = "ConnectedAndSaved";
			}else if (flag && document.getString(CONNTYPE).toString().equalsIgnoreCase(TEST)){
				connectionStr = "Connected";
			}else{
				connectionStr = "Connection Can Not Be Established ";
			}
			response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
			response.getWriter().println(connectionStr);
			logger.info("testAndSaveconnection Completed");
		return connectionStr;
	}
	private static Document parsefromJson(String jsonString) {
		return Document.parse(jsonString);
	}
	
	@RequestMapping(value="getConnectionDetails")
	public void getConnectionDetails(HttpServletRequest request,HttpServletResponse response) throws IOException{
		logger.info("Inside getConnectionDetails");
		String dbStr = null;
		List<SyncConnectionInfo> infos = adminService.getconnectionDetails();
		if(infos!=null && !infos.isEmpty()){
			JSONObject jsonObject = null;
			JSONArray array = new JSONArray();
			for(SyncConnectionInfo connectionInfo : infos){
				jsonObject = new JSONObject();
				jsonObject.put(DBNAME, connectionInfo.getDbName());
				jsonObject.put(USERNAME, connectionInfo.getUserName());
				jsonObject.put(DBTYPE, connectionInfo.getDbType());
				array.put(jsonObject);
			}
			dbStr = array.toString();
		}
		response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
		response.getWriter().println(dbStr);
		logger.info("getConnectionDetails Completed");
	}
	
	@RequestMapping(value="deleteUser")
	public void deleteUser(HttpServletRequest request,HttpServletResponse response) throws IOException{
		logger.info("Inside deleteUser");
		String userId = (request.getParameter(USER_ID) == null || request.getParameter(USER_ID).equalsIgnoreCase(SyncConstants.EMPTY_STRING)) 
				? SyncConstants.EMPTY_STRING : request.getParameter(USER_ID).trim();
		adminService.deleteUser(userId);
		response.getWriter().println(SyncConstants.SUCCESS);
		logger.info("deleteUser Completed");
	}
	
	@RequestMapping(value="deleteConnection")
	public void deleteConnection(HttpServletRequest request,HttpServletResponse response) throws IOException{
		logger.info("Inside deleteConnection");
		String id = (request.getParameter(ID) == null || request.getParameter(ID).equalsIgnoreCase(SyncConstants.EMPTY_STRING)) 
				? SyncConstants.EMPTY_STRING : request.getParameter(ID).trim();
		adminService.deleteConnection(new ObjectId(id));
		response.getWriter().println(SyncConstants.SUCCESS);
		logger.info("deleteConnection Completed");
	}
	
	@RequestMapping(value="encryptPass")
	public void encryptPass(HttpServletRequest request,HttpServletResponse response) throws IOException{
		logger.info("Inside encryptPass");
		try {
			adminService.encryptPass();
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.info("encryptPass Completed");
	}
	
	@RequestMapping(value="searchConnection")
	public void searchConnection(HttpServletRequest request,HttpServletResponse response) throws IOException{
		logger.info("Inside searchConnection");
			String dbName = (request.getParameter(DBNAME) == null || request.getParameter(DBNAME).equalsIgnoreCase(SyncConstants.EMPTY_STRING))
					? SyncConstants.EMPTY_STRING : request.getParameter(DBNAME);
			String userName = (request.getParameter(USERNAME) == null || request.getParameter(USERNAME).equalsIgnoreCase(SyncConstants.EMPTY_STRING))
					? SyncConstants.EMPTY_STRING : request.getParameter(USERNAME);
			String dbType = (request.getParameter(DBTYPE) == null || request.getParameter(DBTYPE).equalsIgnoreCase(SyncConstants.EMPTY_STRING))
					? SyncConstants.EMPTY_STRING : request.getParameter(DBTYPE);
			SyncConnectionInfo connectionInfo = adminService.searchconnection(dbName, userName, dbType);
			JSONObject object = null;
			if(connectionInfo != null){
				object = new JSONObject(connectionInfo);
			}
			response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
			response.getWriter().println(object);
		logger.info("searchConnection Completed");
	}
	
	@RequestMapping(value="getUser")
	public void getUser(HttpServletRequest request,HttpServletResponse response) throws IOException{
		logger.info("Inside getUser");
		String userId = (request.getParameter(USER_ID) == null
				|| request.getParameter(USER_ID).equalsIgnoreCase(SyncConstants.EMPTY_STRING))
						? SyncConstants.EMPTY_STRING : request.getParameter(USER_ID);
		SyncUser syncUser = adminService.getUser(userId);
		JSONObject object = null;
		if (syncUser != null) {
			object = new JSONObject(syncUser);
			object.put("validUser", true);
		} else {
			object = new JSONObject();
			object.put("validUser", false);
		}
		response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
		response.getWriter().println(object);
		logger.info("getUser Completed");
	}
	
}
