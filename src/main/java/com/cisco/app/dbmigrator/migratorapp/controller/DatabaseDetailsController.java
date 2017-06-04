package com.cisco.app.dbmigrator.migratorapp.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cisco.app.dbmigrator.migratorapp.constants.SyncConstants;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncUser;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.UserActivity;
import com.cisco.app.dbmigrator.migratorapp.service.CollectionsDetailsService;
import com.cisco.app.dbmigrator.migratorapp.service.CollectionsDetailsServiceImpl;
import com.cisco.app.dbmigrator.migratorapp.service.DatabaseDetailsService;
import com.cisco.app.dbmigrator.migratorapp.service.DatabaseDetailsServiceImpl;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@Controller
public class DatabaseDetailsController {
	Logger logger = Logger.getLogger("DatabaseDetailsController");

	@RequestMapping(value = "/databaseDetails")
	public void fetchDatabaseDetails(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Gson gson = new Gson();
		String userActivityStr = SyncConstants.EMPTY_STRING;

		String sourceDbName = request.getParameter("sourceDatabaseName").trim().equalsIgnoreCase("") ? ""
				: request.getParameter("sourceDatabaseName").trim();
		String sourceSchemaName = request.getParameter("sourceDatabaseSchema").trim().equalsIgnoreCase("") ? ""
				: request.getParameter("sourceDatabaseSchema").trim();
		String targetDbName = request.getParameter("targetDatabaseName").trim().equalsIgnoreCase("") ? ""
				: request.getParameter("targetDatabaseName").trim();
		String targetSchemaName = request.getParameter("targetDatabaseSchema").trim().equalsIgnoreCase("") ? ""
				: request.getParameter("targetDatabaseSchema").trim();

		SyncUser userInfo = (SyncUser) request.getSession().getAttribute(SyncConstants.USER);

		logger.info("In Side Controller : sourceDbName>>" + sourceDbName + ">>sourceSchemaName>>" + sourceSchemaName
				+ ">>targetDbName>>" + targetDbName + ">>targetSchemaName>>" + targetSchemaName);

		// Creating UserActivity Object
		UserActivity userActivity = new UserActivity();
		userActivity.setUserId(userInfo.getUserid());
		userActivity.setSourceDbName(sourceDbName);
		userActivity.setSourceSchemaName(sourceSchemaName);
		userActivity.setTargetDbName(targetDbName);
		userActivity.setTargetSchemaName(targetSchemaName);

		userActivityStr = gson.toJson(userActivity);
		request.getSession().setAttribute("UserActivity", userActivity);

		response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
		response.getWriter().println(userActivityStr);
	}

	@RequestMapping(value = "/fetchTablesDetails")
	public void fetchTablesDetails(HttpServletRequest request, HttpServletResponse response) throws IOException {

		logger.info("Inside fetchTablesDetails");
		Gson gson = new Gson();
		String tableListJson = "";
		String pattern = (request.getParameter("pattern")).toUpperCase() + "%";
		logger.info("Searched Table pattern" + pattern);
		UserActivity userActivity = new UserActivity();
		userActivity.setSourceDbName(request.getParameter("sourceDatabaseName"));
		;
		logger.info("Source DB is:" + userActivity.getSourceDbName());
		userActivity.setSourceSchemaName(request.getParameter("sourceDatabaseSchema"));
		logger.info("Source schema is:" + userActivity.getSourceSchemaName());
		DatabaseDetailsService databaseDetailsService = new DatabaseDetailsServiceImpl();
		Map<String, Object> outPut = databaseDetailsService.fetchTablesDetails(userActivity, pattern);
		if ("SUCCESS".equalsIgnoreCase(String.valueOf(outPut.get("result")))) {
			@SuppressWarnings("unchecked")
			List<String> tableList = (List<String>) outPut.get("output");
			tableListJson = gson.toJson(tableList);
		}

		response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
		response.getWriter().println(tableListJson);

	}

	@RequestMapping(value = "/fetchColumnsDetails")
	public void fetchColumnsDetails(HttpServletRequest request, HttpServletResponse response) throws IOException {

		logger.info("Inside fetchColumnsDetails");
		Gson gson = new Gson();
		String jsonColumnsDtl = "";
		String tableName = request.getParameter("tableName").trim().equalsIgnoreCase("") ? ""
				: request.getParameter("tableName").trim();
		logger.info("Fetch column for TableName : " + tableName);

		UserActivity userActivity = new UserActivity();
		userActivity.setSourceDbName(request.getParameter("sourceDatabaseName"));
		logger.info("Source DB is:" + userActivity.getSourceDbName());
		userActivity.setSourceSchemaName(request.getParameter("sourceDatabaseSchema"));
		logger.info("Source schema is:" + userActivity.getSourceSchemaName());
		DatabaseDetailsService databaseDetailsService = new DatabaseDetailsServiceImpl();
		Map<String, Object> outPut = databaseDetailsService.fetchColumnsDetails(userActivity, tableName);
		List<OracleColumn> columnList = new ArrayList<OracleColumn>();
		if ("SUCCESS".equalsIgnoreCase(String.valueOf(outPut.get("result")))) {
			@SuppressWarnings("unchecked")
			Map<String, OracleColumn> columnsMap = (Map<String, OracleColumn>) outPut.get("output");
			columnList.addAll(columnsMap.values());
			jsonColumnsDtl = gson.toJson(columnList);
			logger.info(jsonColumnsDtl);
		}
		response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
		response.getWriter().println(jsonColumnsDtl);

	}

	@RequestMapping(value = "/fetchAllCollections")
	public void fetchAllCollections(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.info("Inside fetchAllCollections");
		String collArrayStr = null;
		String sourceDbName = request.getParameter("sourceDatabaseName").trim().equalsIgnoreCase("") ? ""
				: request.getParameter("sourceDatabaseName").trim();
		String sourceSchemaName = request.getParameter("sourceDatabaseSchema").trim().equalsIgnoreCase("") ? ""
				: request.getParameter("sourceDatabaseSchema").trim();
		CollectionsDetailsService collectionDetailsService = new CollectionsDetailsServiceImpl();
		List<String> collectionList = collectionDetailsService.getAllCollections(sourceDbName, sourceSchemaName);
		Gson gson = new Gson();
		if (collectionList != null && !collectionList.isEmpty()) {
			collArrayStr = gson.toJson(collectionList);
		}
		response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
		response.getWriter().println(collArrayStr);
	}

	@RequestMapping(value = "/fetchAttributesForCollection")
	public void fetchAttributesForCollection(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		logger.info("Inside fetchAttributesForCollection");
		String attrArrayStr = null;
		String sourceDbName = request.getParameter("sourceDatabaseName").trim().equalsIgnoreCase("") ? ""
				: request.getParameter("sourceDatabaseName").trim();
		String sourceSchemaName = request.getParameter("sourceDatabaseSchema").trim().equalsIgnoreCase("") ? ""
				: request.getParameter("sourceDatabaseSchema").trim();
		String collectionName = request.getParameter("collectionName").trim().equalsIgnoreCase("") ? ""
				: request.getParameter("collectionName").trim();
		CollectionsDetailsService collectionDetailsService = new CollectionsDetailsServiceImpl();
		List<String> attrList = collectionDetailsService.getAttributesForCollection(sourceDbName, sourceSchemaName,
				collectionName);
		Gson gson = new Gson();
		if (attrList != null && !attrList.isEmpty()) {
			attrArrayStr = gson.toJson(attrList);
		}
		response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
		response.getWriter().println(attrArrayStr);
	}
	
	@RequestMapping(value = "/fetchSequenceNames")
	public void fetchSequenceNames(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.info("Inside fetchSequenceNames");
		String seqPattern = request.getParameter("seqPattern").trim().equalsIgnoreCase("") ? SyncConstants.EMPTY_STRING :
							request.getParameter("seqPattern").toUpperCase().trim();
		UserActivity userActivity = new UserActivity();
		userActivity.setSourceDbName(request.getParameter("sourceDatabaseName"));
		logger.info("Source DB is:" + userActivity.getSourceDbName());
		userActivity.setSourceSchemaName(request.getParameter("sourceDatabaseSchema"));
		logger.info("Source schema is:" + userActivity.getSourceSchemaName());
		DatabaseDetailsService databaseDetailsService = new DatabaseDetailsServiceImpl();
		List<String> seqNameList = databaseDetailsService.getMatchedSequences(userActivity,seqPattern+"%");
		String seqNameStr = SyncConstants.EMPTY_STRING;
		Gson gson = new Gson();
		if (seqNameList != null && !seqNameList.isEmpty()) {
			seqNameStr = gson.toJson(seqNameList);
		}
		logger.debug("Map Json fetched is : " + seqNameStr);
		response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
		response.getWriter().println(seqNameStr);
		logger.info("fetchSequenceNames Completed");
	}

	@RequestMapping(value = "/fetchColumnsForMultipleTables")
	public void fetchColumnsForMultipleTables(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.info("Inside fetchColumnsForMultipleTables");
		String jsonColumnsDtl = "";
		List<String> tableNameList = null;
		JsonArray jsonArray = new JsonArray();
		Gson gson = new Gson();
		String tableName = request.getParameter("tableName").trim().equalsIgnoreCase("") ? ""
				: request.getParameter("tableName").trim();
		logger.info("Fetch column for TableName : " + tableName);
		String sourceDatabaseName = request.getParameter("sourceDatabaseName");
		logger.info("Source DB is:" + sourceDatabaseName);
		String sourceDatabaseSchema = request.getParameter("sourceDatabaseSchema");
		logger.info("Source schema is:" + sourceDatabaseSchema);
		if(tableName != null && !tableName.isEmpty()){
			tableNameList = Arrays.asList(tableName.split(","));
		}
		DatabaseDetailsService databaseDetailsService = new DatabaseDetailsServiceImpl();
		Map<String, Object> outPut = databaseDetailsService.fetchColumnsForMultipleTables(sourceDatabaseName,sourceDatabaseSchema, tableNameList);
		if (SyncConstants.SUCCESS.equalsIgnoreCase(String.valueOf(outPut.get("result")))) {
			@SuppressWarnings("unchecked")
			Map<String, OracleColumn> columnsMap = (Map<String, OracleColumn>) outPut.get("output");
			for(Entry<String,OracleColumn> entry : columnsMap.entrySet()){
				JsonObject obj = new JsonObject();
				obj.addProperty("tableName", entry.getKey());
				JsonElement jsonElement = gson.toJsonTree(entry.getValue());
				obj.add("columns", jsonElement);
				jsonArray.add(obj);
			}
		}
		jsonColumnsDtl = jsonArray.toString();
		response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
		response.getWriter().println(jsonColumnsDtl);
		logger.info("Inside fetchColumnsForMultipleTables Completed");
	}
}