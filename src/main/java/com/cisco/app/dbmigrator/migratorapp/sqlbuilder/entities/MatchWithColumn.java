package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities;

/**
 * @author pnilayam
 *
 */
public class MatchWithColumn {
	private MatchWithColumn() {
	}

	/**
	 * @param tableAlias
	 * @param columnName
	 * @return
	 */
	public static OracleColumn column(String tableAlias, String columnName) {
		OracleColumn column = new OracleColumn();
		column.setColumnName(columnName);
		column.setTableAlias(tableAlias);
		column.setColumnAlias(tableAlias+"_"+columnName);
		column.setParentColumn(false);
		return column;
	}
	/**
	 * @param tableAlias
	 * @param columnName
	 * @param isParentColumn
	 * @return
	 */
	public static OracleColumn column(String tableAlias, String columnName ,boolean isParentColumn) {
		OracleColumn column = new OracleColumn();
		column.setColumnName(columnName);
		column.setTableAlias(tableAlias);
		column.setColumnAlias(tableAlias+"_"+columnName);
		column.setParentColumn(isParentColumn);
		return column;
	}
}
