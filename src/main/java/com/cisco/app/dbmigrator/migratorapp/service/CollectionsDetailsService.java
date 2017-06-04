package com.cisco.app.dbmigrator.migratorapp.service;

import java.util.List;

import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoObject;

public interface CollectionsDetailsService {

	public List<String> getAllCollections(String sourceDbName, String sourceSchemaName);

	public List<String> getAttributesForCollection(String sourceDbName, String sourceSchemaName, String collectionName);
	
	public MongoObject processCollection(String sourceDbName, String sourceSchemaName, String collectionName);
	
}
