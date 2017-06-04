package com.cisco.app.dbmigrator.migratorapp.utilities.oracle;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncConnectionInfo;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;

/**
 * @author pnilayam
 *
 */
public class OracleDBUtilities {
	private  Logger logger = Logger.getLogger(OracleDBUtilities.class);
	/**
	 * @param tableName
	 * @param con
	 * @return
	 */
	public Map<String, OracleColumn> loadAllColumns(String tableName, Connection con)
	{
		logger.info("Loading all columns for table " + tableName);
		ResultSetMetaData metaData = null;	
		PreparedStatement stmt =null;	    
	    Map<String , OracleColumn> columns = new HashMap<String, OracleColumn>();
	    try {
			stmt = con.prepareStatement("SELECT * FROM "+tableName+" WHERE 1=2");
			stmt.execute();
			metaData= stmt.getMetaData();
	    	int columnCount= metaData.getColumnCount();
	    	logger.info("Column count for table "+tableName +" is "+columnCount);
	    	logger.info("Begin Loading column metdata");
	    	for(int count =1 ;count<=columnCount;count++){
	    		OracleColumn column = new OracleColumn();
	    		String columnName = metaData.getColumnName(count);
		    	column.setColumnName(columnName);
		    	column.setColumnType(metaData.getColumnTypeName(count));
		    	column.setPrecision(metaData.getPrecision(count));
		    	column.setNullable(metaData.isNullable(count)==1?true:false);		
		    	columns.put(columnName, column);
	    	}
	    	logger.info("Column metadata loaded for all columns. Column map size : "+columns.size());
		} catch (Exception e) {
			logger.warn("Error while loading column info",e);
		}
	    finally{
	    	try {
	    		if(stmt!=null){
	    			stmt.close();
	    		}
			} catch (SQLException e) {
				logger.warn("Exception while closing statement or connection ", e);
			}
	    }
		return columns;	   
	}	

	/**
	 * @param con
	 * @param schemaName
	 * @param tablePattern
	 * @return
	 */
	public List<String> getAllTables(Connection con, String schemaName,String tablePattern){
		logger.info("Start getAllTables "+tablePattern);
		ResultSet rs =null;
		List<String> tableList = new ArrayList<String>();
		try {
			 DatabaseMetaData meta = con.getMetaData();
			 rs = meta.getTables(null, schemaName, tablePattern, new String[] {"TABLE" , "SYNONYM"});

			 logger.info("Fetched Database metadata. Begin processing tables");
			 while(rs.next()){
				 String tableName = rs.getString("TABLE_NAME");
				 logger.debug("Load details for table : "+tableName);
				 tableList.add(tableName);				 
			 }
		} catch (SQLException e) {
			logger.error("Error in getting Table List", e);
		}
		finally{
			try {
				if(rs!=null){
					rs.close();
				}
				if(con!=null && !con.isClosed()){
					con.close();
				}
			} catch (SQLException e) {
				logger.warn("Error in closing resources", e);
			}
			
		}		
		return tableList;		
	}
	/**
	 * @param connection
	 * @param countQuery
	 * @return
	 */
	public int getTotalRowCount(Connection connection, String countQuery){
		logger.info("Start of method getTotalRowCount");
		int rowCount=0;
		PreparedStatement pstmt=null;
		ResultSet rs =null;
		try {
			pstmt = connection.prepareStatement(countQuery);
			rs = pstmt.executeQuery();
			logger.info("Query Executed to get RowCount");
			rs.next();
			rowCount=rs.getInt(1);
			logger.info("Rowcount Fecthed : "+rowCount);
		} catch (SQLException e) {
			logger.error("Error while getting Column count",e);
		} catch (Exception e) {
				logger.error("Error while getting Column count",e);
		}finally{
			try {
				if(pstmt!=null){
					pstmt.close();
				}
				if(connection!=null){
					connection.close();
				}
			} catch (SQLException e) {
				logger.warn("Error in closing resources", e);
			}
		}
		return rowCount;
	}
	
	public List<String> getMatchedSequences(Connection connection , String seqNamePattern){
		logger.info("Loading all sequences with pattern " + seqNamePattern);
		PreparedStatement stmt =null;	   
		ResultSet rset = null;
		List<String> seqList = new ArrayList<String>();
	    try {
			stmt = connection.prepareStatement("select SEQUENCE_NAME from USER_SEQUENCES WHERE  SEQUENCE_NAME  LIKE ?");
			stmt.setString(1, seqNamePattern);
			rset =stmt.executeQuery();
			while(rset.next()){
				seqList.add(rset.getString(1));
			}
	    	logger.info("Sequence List loaded. Size : "+seqList.size());
		} catch (Exception e) {
			logger.warn("Error while loading Sequence List",e);
		}
	    finally{
	    	try {
	    		if(stmt!=null){
	    			stmt.close();
	    		}
	    		if(connection!=null){
	    			connection.close();
	    		}
			} catch (SQLException e) {
				logger.warn("Exception while closing statement or connection ", e);
			}
	    }
		return seqList;	   
	}
	public static boolean testOracleConnection(SyncConnectionInfo connectionInfo){
		boolean flag = false;
		Connection con;
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			con = DriverManager.getConnection(  
			"jdbc:oracle:oci:@"+connectionInfo.getDbName(),connectionInfo.getUserName(),connectionInfo.getPassword());
			if(con!=null){
				flag = true;
				con.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} 
		return flag;
	}
}
