package com.cisco.app.dbmigrator.migratorapp.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cisco.app.dbmigrator.migratorapp.logging.entities.UserActivity;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;
import com.cisco.app.dbmigrator.migratorapp.utilities.cache.DBCacheManager;
import com.cisco.app.dbmigrator.migratorapp.utilities.oracle.OracleDBUtilities;

public class DatabaseDetailsServiceImpl implements DatabaseDetailsService {

	Logger logger = Logger.getLogger("DatabaseDetailsServiceImpl");	
	OracleDBUtilities utilities = new OracleDBUtilities();
	@Override
	public Map<String, Object> fetchTablesDetails(UserActivity userActivity, String tablePattern) {
		Map<String, Object> outPut = new HashMap<String, Object>();

		try {

			Connection connection = DBCacheManager.INSTANCE
					.getCachedOracleConnection(userActivity.getSourceDbName(), userActivity.getSourceSchemaName());
			List<String> tableList = utilities.getAllTables(connection,
					userActivity.getSourceSchemaName(), tablePattern);
			outPut.put("output", tableList);
			outPut.put("result", "SUCCESS");
		} catch (Exception e) {
			outPut.put("result", e.getMessage());
			logger.error("Error while fetching table details", e);
		}
		return outPut;
	}

	@Override
	public Map<String, Object> fetchColumnsDetails(UserActivity userActivity, String tableName) {
		Map<String, Object> outPut = new HashMap<String, Object>();
		logger.info("Inside Impl fetchColumnsDetails");
		Connection connection = null;
		try {
			connection = DBCacheManager.INSTANCE
					.getCachedOracleConnection(userActivity.getSourceDbName(), userActivity.getSourceSchemaName());
			Map<String, OracleColumn> columnsMap = utilities.loadAllColumns(tableName, connection);
			outPut.put("output", columnsMap);
			outPut.put("result", "SUCCESS");
		} catch (Exception e) {
			outPut.put("result", e.getMessage());
			logger.error("Error while fetching column details", e);
		}finally{
			try {
				if(connection!=null){
					connection.close();
				}
			} catch (SQLException e) {
				logger.warn("Error while closing connection", e);
			}
		}
		return outPut;
	}

	@Override
	public List<String> getMatchedSequences(UserActivity userActivity,String seqPattern) {
		List<String> seqNameList = new ArrayList<String>();
		try{
			Connection con= DBCacheManager.INSTANCE
			.getCachedOracleConnection(userActivity.getSourceDbName(), userActivity.getSourceSchemaName());
			seqNameList = utilities.getMatchedSequences(con, seqPattern);
		}catch(Exception e){
			logger.error("Error while getting sequence details", e);
		}
		return seqNameList;
	}

	@Override
	public Map<String, Object> fetchColumnsForMultipleTables(String sourceDatabaseName,String sourceDatabaseSchema, List<String> tableNameList) {
		Map<String, Object> outPut = new HashMap<String, Object>();
		Map<String, OracleColumn> columnsMap = new HashMap<String,OracleColumn>();
		Map<String, Object> tableColumnMap = new HashMap<String,Object>();
		logger.info("Inside Impl fetchColumnsForMultipleTables");
		Connection connection = null;
		try {
			connection = DBCacheManager.INSTANCE
					.getCachedOracleConnection(sourceDatabaseName,sourceDatabaseSchema);
			if(tableNameList !=null && !tableNameList.isEmpty()){
				for(String tableName : tableNameList){
					columnsMap = utilities.loadAllColumns(tableName, connection);
					tableColumnMap.put(tableName,columnsMap.values().toArray());
				}
			}
			outPut.put("output", tableColumnMap);
			outPut.put("result", "SUCCESS");
		} catch (Exception e) {
			outPut.put("result", e.getMessage());
			logger.error("Error while getting columns for multiple tables" , e);
		}finally{
			try {
				if(connection!=null){
					connection.close();
				}
			} catch (SQLException e) {
				logger.warn("Error while closing connection", e);
			}
		}
		return outPut;
	}

}
