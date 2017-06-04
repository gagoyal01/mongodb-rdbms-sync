package com.cisco.app.dbmigrator.migratorapp.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cisco.app.dbmigrator.migratorapp.constants.SyncConstants;
import com.cisco.app.dbmigrator.migratorapp.core.map.MongoToOracleMap;
import com.cisco.app.dbmigrator.migratorapp.core.map.SyncMap;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncMapAndEventEncoder;
import com.cisco.app.dbmigrator.migratorapp.service.SyncMapService;

@Controller
public class SyncMapController {
	private static final Logger logger = Logger.getLogger(SyncMapController.class);
	private static final String MAPPING_ID = "mappingId";
	private static final String MAP_JSON = "mapJson";
	private static final String MAP_TYPE = "mapType";
	
	@Autowired
	private SyncMapService mapService;

	public SyncMapService getMapService() {
		return mapService;
	}

	public void setMapService(SyncMapService mapService) {
		this.mapService = mapService;
	}

	@RequestMapping(value = "/getMappingTables")
	public void getMappingTables(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.info("Inside getMappingTables");
		String mappingTablesStr = SyncConstants.EMPTY_STRING;
		String mappingId = request.getParameter(MAPPING_ID).trim().equalsIgnoreCase(SyncConstants.EMPTY_STRING)
				? SyncConstants.EMPTY_STRING : request.getParameter(MAPPING_ID).trim();
		logger.debug("Process getMappingTables for requested mappingId : " + mappingId);
		mappingTablesStr = mapService.getMappingTables(mappingId);
		logger.debug("Sending mapping table list Json " + mappingTablesStr);
		response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
		response.getWriter().println(mappingTablesStr);
		logger.info("getMappingTables Completed");
	}

	@RequestMapping(value = "/deleteMapping")
	public void deleteMapping(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.info("Inside deleteEventMapping");
		String mappingId = request.getParameter(MAPPING_ID);
		mapService.deleteMapping(mappingId);
		response.getWriter().println(SyncConstants.SUCCESS);
		logger.info("deleteEventMapping Completed");
	}

	@RequestMapping(value = "/loadMapping")
	public void loadMapping(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.info("Inside loadMapping");
		String mappingId = request.getParameter(MAPPING_ID).trim().equalsIgnoreCase(SyncConstants.EMPTY_STRING)
				? SyncConstants.EMPTY_STRING: request.getParameter(MAPPING_ID).trim();
		logger.debug("Loading request for mappingId" + mappingId);
		SyncMap map = mapService.loadMapping(mappingId);
		String mappingStr = SyncMapAndEventEncoder.getMapJson(map);
		logger.debug("Map Json fetched is : " + mappingStr);
		response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
		response.getWriter().println(mappingStr);
		logger.info("loadMapping Completed");
	}

	@RequestMapping(value = "/getAllMappings")
	public void getAllMappings(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.info("Inside getAllMappings");
		List<SyncMap> mappingList = mapService.getAllMappings();
		String mappingStr = SyncConstants.EMPTY_STRING;
		JSONArray array = new JSONArray();
		if (mappingList != null && !mappingList.isEmpty()) {
			JSONObject jsonObject = null;
			for (SyncMap map : mappingList) {
				String mapJson = SyncMapAndEventEncoder.getMapJson(map);
				jsonObject = new JSONObject(mapJson);
				array.put(jsonObject);
			}
		}
		mappingStr = array.toString();
		logger.debug("Map Json fetched is : " + mappingStr);
		response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
		response.getWriter().println(mappingStr);
		logger.info("getAllMappings Completed");
	}

	@RequestMapping(value = "/saveMappings")
	public void saveMappings(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.info("Inside saveMappings");
		String mapJson = request.getParameter(MAP_JSON).trim().equalsIgnoreCase(SyncConstants.EMPTY_STRING)
				? SyncConstants.EMPTY_STRING: request.getParameter(MAP_JSON).trim();
		logger.debug("Map JSON from UI " + mapJson);
		mapService.saveMappings(mapJson);
		logger.info("saveMappings Completed");
	}
	
	@RequestMapping(value = "/getMappingByMapType")
	public void getMappingByMapType(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.info("Inside getCollectionStructures");
		String mapType = request.getParameter(MAP_TYPE).trim().equalsIgnoreCase(SyncConstants.EMPTY_STRING)
				? SyncConstants.EMPTY_STRING: request.getParameter(MAP_TYPE).trim();
		List<SyncMap> mappingList = mapService.getMappingByMapType(mapType);
		String mappingStr = SyncConstants.EMPTY_STRING;
		JSONArray array = new JSONArray();
		if (mappingList != null && !mappingList.isEmpty()) {
			JSONObject jsonObject = null;
			for (SyncMap map : mappingList) {
				String mapJson = SyncMapAndEventEncoder.getMapJson(map);
				jsonObject = new JSONObject(mapJson);
				array.put(jsonObject);
			}
		}
		mappingStr = array.toString();
		logger.debug("Map Json fetched is : " + mappingStr);
		response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
		response.getWriter().println(mappingStr);
		logger.info("getCollectionStructures Completed");
	}
	
	@RequestMapping(value= "/getTranslatedMap")
	public void getTranslatedMap(HttpServletRequest request,HttpServletResponse response) throws IOException{
		logger.info("inside getTranslatedMap");
		String mappingId = request.getParameter("mappingId").trim().equalsIgnoreCase(SyncConstants.EMPTY_STRING)
				? SyncConstants.EMPTY_STRING : request.getParameter("mappingId").trim();
		MongoToOracleMap map = mapService.translateMap(mappingId);
		String mapStr = SyncConstants.EMPTY_STRING;
		if(map !=null){
			mapStr = SyncMapAndEventEncoder.getMapJson(map);
		}
		response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
		response.getWriter().println(mapStr);
		logger.info("getTranslatedMap Completed");
	}
}
