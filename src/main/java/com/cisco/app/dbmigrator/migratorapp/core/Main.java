package com.cisco.app.dbmigrator.migratorapp.core;

import java.sql.SQLException;

public class Main {

	public static void main(String[] args) throws SQLException {
		//testInsertQueryBuilder();
		// TODO Auto-generated method stub

		/*
		 * String tableName= "oe_sets"; String table2 = "oe_order_headers_all";
		 * String table3 = "oe_order_lines_all"; String table4 =
		 * "OE_ORDER_HOLDS_ALL"; Connection con = null; MongoDatabase
		 * mongoDatabase=null;
		 * 
		 * 
		 * OracleConnectionInfo oracleInfo = new OracleConnectionInfo();
		 * oracleInfo.setUserName("APPS"); oracleInfo.setPassword("Ju463m39t");
		 * oracleInfo.setUrl("jdbc:oracle:oci:@DV1CG1");
		 * 
		 * MongoConnectionInfo mongoConnectionInfo = new MongoConnectionInfo();
		 * mongoConnectionInfo.setDbName("test");
		 * 
		 * 
		 * 
		 * 
		 * OracleToMongoBasicEvent basicEvent =
		 * OracleToMongoBasicEventBuilder.getBasicEventBuilder(tableName, null)
		 * .addOracleConnectionInfo(oracleInfo)
		 * .addMongoConnectionInfo(mongoConnectionInfo) .build();
		 * //basicEvent.run(); Thread basicEventThread = new Thread(basicEvent);
		 * basicEventThread.start();
		 * 
		 * OracleToMongoBasicEvent basicEvent2 =
		 * OracleToMongoBasicEventBuilder.getBasicEventBuilder(table2, null)
		 * .addOracleConnectionInfo(oracleInfo)
		 * .addMongoConnectionInfo(mongoConnectionInfo) .build(); Thread
		 * basicEventThread2 = new Thread(basicEvent2);
		 * basicEventThread2.start();
		 * 
		 * OracleToMongoBasicEvent basicEvent3 =
		 * OracleToMongoBasicEventBuilder.getBasicEventBuilder(table3, null)
		 * .addOracleConnectionInfo(oracleInfo)
		 * .addMongoConnectionInfo(mongoConnectionInfo) .build(); Thread
		 * basicEventThread3 = new Thread(basicEvent3);
		 * basicEventThread3.start();
		 * 
		 * OracleToMongoBasicEvent basicEvent4 =
		 * OracleToMongoBasicEventBuilder.getBasicEventBuilder(table4, null)
		 * .addOracleConnectionInfo(oracleInfo)
		 * .addMongoConnectionInfo(mongoConnectionInfo) .build(); Thread
		 * basicEventThread4 = new Thread(basicEvent4);
		 * basicEventThread4.start();
		 * 
		 */

		/*
		 * UserDetailDao dao = new UserDetailDao(); UserDetailCollection user =
		 * dao.getUser("gagoyal");
		 * 
		 * System.out.println(user.toString()); Map <String, Set<String>> source
		 * = user.getSourceDbMap(); for(Map.Entry<String, Set<String>> obj
		 * :source.entrySet()){ System.out.println(obj.getKey());
		 * System.out.println(obj.getValue()); }
		 */
		// testCaching();
	}

	/*
	 * public static void testCaching(){ System.out.println("Start");
	 * OracleDBUtilities.getOracleConnectionPool("DV1CG1", "APPS");
	 * System.out.println("End"); System.out.println("Start");
	 * OracleDBUtilities.getOracleConnectionPool("DV1CG1", "APPS");
	 * System.out.println("End"); System.out.println("Start");
	 * OracleDBUtilities.getOracleConnectionPool("DV1CG1", "APPS");
	 * System.out.println("End");
	 * 
	 * }
	 */

	/*public static void testInsertQueryBuilder() {
		Set<OracleTable> tableSet = new HashSet<>();
		OracleTable table = new OracleTable();
		table.setTableName("test");
		table.setTableAlias("t0");

		OracleColumn column1 = new OracleColumn();
		column1.setColumnName("col1");
		column1.setTableAlias("t0");

		OracleColumn column2 = new OracleColumn();
		column2.setColumnName("col2");
		column2.setTableAlias("t0");

		table.addColumn(column1);
		table.addColumn(column2);
		tableSet.add(table);

		Set<String> columnAliasSet = new HashSet<>();

		InsertQueryBuilder builder = new InsertQueryBuilder();
		System.out.println(builder.insertAll().intoTables(tableSet).prepareInsertStatement(columnAliasSet));
		for(String columnAlias : columnAliasSet){
			System.out.println(columnAlias);
		}
	}*/
}
