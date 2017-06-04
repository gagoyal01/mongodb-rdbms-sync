package com.cisco.app.dbmigrator.migratorapp.utilities.oracle;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

public final class DbResourceUtils {
	private static Logger logger = Logger.getLogger(DbResourceUtils.class);
	private DbResourceUtils(){}
	public static void closeResources(ResultSet rset , Statement stmt, Connection connection){
		if(rset!=null){
			try {
				rset.close();
			} catch (SQLException e) {
				logger.error("Error in closing Resultset" , e);
			}
		}
		if(stmt!=null){
			try {
				stmt.close();
			} catch (SQLException e) {
				logger.error("Error in closing Statement" , e);
			}
		}
		if(connection!=null){
			try {
				connection.close();
			} catch (SQLException e) {
				logger.error("Error in closing Connection" , e);
			}
		}
	}
	
	public static boolean isValidConnection(Connection con){
		boolean isValid=true;
		try{
			if(con==null || con.isClosed()){
				logger.info("Connection is Null");
				isValid=false;
			}else{
				/*ValidConnection valCon= (ValidConnection) con;
				if(!valCon.isValid()){
					logger.info("Connection is invalid");
					isValid=false;
					valCon.setInvalid();
				}*/
			}	
		}catch(SQLException e){
			isValid=false;
			logger.error("Error in validating connection ",e);
		}	
		return isValid;
	}
}
