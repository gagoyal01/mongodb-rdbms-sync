package com.cisco.app.dbmigrator.migratorapp.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cisco.app.dbmigrator.migratorapp.constants.SyncConstants;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoObject;
import com.cisco.app.dbmigrator.migratorapp.service.CollectionsDetailsService;
import com.cisco.app.dbmigrator.migratorapp.service.CollectionsDetailsServiceImpl;
import com.google.gson.Gson;


@Controller
public class ESController {
	private static final Logger logger = Logger.getLogger(ESController.class);
	
	@RequestMapping(value="/getAllCollections")
	public void getAllCollections(HttpServletRequest request,HttpServletResponse response){
		Gson gson = new Gson();
		String collectionListJson = "";
		String sourceDatabaseName = request.getParameter("sourceDatabaseName")!=null ? request.getParameter("sourceDatabaseName").trim() : null;
		String sourceSchemaName = request.getParameter("sourceSchemaName")!=null ? request.getParameter("sourceSchemaName").trim() : null;
		CollectionsDetailsService collectionDetailsService = new CollectionsDetailsServiceImpl();
		try {
			List<String> collectionList = collectionDetailsService.getAllCollections(sourceDatabaseName, sourceSchemaName);
			collectionListJson = gson.toJson(collectionList);
			response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
			response.getWriter().println(collectionListJson);
		} catch (Exception e) {
			logger.info("Exception while fetching collection "+e.getMessage());
		}
	}
	
	@RequestMapping(value="/getAttributesForCollection")
	public void getAttributesForCollection(HttpServletRequest request,HttpServletResponse response){
		Gson gson = new Gson();
		String attrListJson = "";
		String sourceDatabaseName = request.getParameter("sourceDatabaseName")!=null ? request.getParameter("sourceDatabaseName").trim() : null;
		String sourceSchemaName = request.getParameter("sourceSchemaName")!=null ? request.getParameter("sourceSchemaName").trim() : null;
		String collectionName = request.getParameter("collectionName")!=null ? request.getParameter("collectionName") : null;
		CollectionsDetailsService collectionDetailsService = new CollectionsDetailsServiceImpl();
		try {
			MongoObject obj = collectionDetailsService.processCollection(sourceDatabaseName,sourceSchemaName,collectionName);
			attrListJson = gson.toJson(obj);
			response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
			response.getWriter().println(attrListJson);
			System.out.println(attrListJson);
		} catch (Exception e) {
			logger.info("Exception while fetching attributes for collection "+e.getMessage());
		}
	}
}
