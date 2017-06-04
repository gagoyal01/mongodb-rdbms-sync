package com.cisco.app.dbmigrator.migratorapp.service;

import java.util.List;
import java.util.Map;

import com.cisco.app.dbmigrator.migratorapp.logging.entities.UserActivity;

public interface DatabaseDetailsService {
	
	public Map<String,Object> fetchTablesDetails(UserActivity userActivity,String pattern);
	public Map<String,Object> fetchColumnsDetails(UserActivity userActivity,String tableName);
	public List<String> getMatchedSequences(UserActivity userActivity,String seqPattern);
	public Map<String,Object> fetchColumnsForMultipleTables(String sourceDatabaseName,String sourceDatabaseSchema,List<String> tableNameList);
}
