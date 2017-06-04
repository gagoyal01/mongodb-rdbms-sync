package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.queries;

import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleTable;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.SQLFilters;

public class MergeQueryComponents {
	private OracleTable targetTable;
	private OracleTable sourceTable;
	private SQLFilters filters;
	public OracleTable getSourceTable() {
		return sourceTable;
	}
	public void setSourceTable(OracleTable sourceTable) {
		this.sourceTable = sourceTable;
	}
	public OracleTable getTargetTable() {
		return targetTable;
	}
	public void setTargetTable(OracleTable targetTable) {
		this.targetTable = targetTable;
	}
	public SQLFilters getFilters() {
		return filters;
	}
	public void setFilters(SQLFilters filters) {
		this.filters = filters;
	}
}
