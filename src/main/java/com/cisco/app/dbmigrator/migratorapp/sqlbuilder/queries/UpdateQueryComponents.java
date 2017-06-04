package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.queries;

import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleTable;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.SQLFilters;

public class UpdateQueryComponents {
	private OracleTable table;
	private SQLFilters filters;
	public OracleTable getTable() {
		return table;
	}
	public void setTable(OracleTable table) {
		this.table = table;
	}
	public SQLFilters getFilters() {
		return filters;
	}
	public void setFilters(SQLFilters filters) {
		this.filters = filters;
	}
}
