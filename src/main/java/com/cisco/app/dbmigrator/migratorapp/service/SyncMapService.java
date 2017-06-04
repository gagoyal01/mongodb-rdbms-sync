package com.cisco.app.dbmigrator.migratorapp.service;

import java.util.List;

import com.cisco.app.dbmigrator.migratorapp.core.map.MongoToOracleMap;
import com.cisco.app.dbmigrator.migratorapp.core.map.SyncMap;

public interface SyncMapService {
	public void saveMappings(String jsonString);

	public SyncMap loadMapping(String mappingId);

	public List<SyncMap> getAllMappings();

	public String getMappingTables(String mappingId);

	public void deleteMapping(String mappingId);
	
	public List<SyncMap> getMappingByMapType(String mapType);
	
	public MongoToOracleMap translateMap(String mappingId);
}
