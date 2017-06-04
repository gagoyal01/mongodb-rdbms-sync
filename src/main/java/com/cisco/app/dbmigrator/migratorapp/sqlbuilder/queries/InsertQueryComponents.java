package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.queries;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleTable;

public class InsertQueryComponents {
	private Set<OracleTable> tables;
	private Map<String,Object> columnValues;
	public Set<OracleTable> getTables() {
		return tables;
	}
	public void setTables(Set<OracleTable> tables) {
		if(tables==null){
			tables = new LinkedHashSet<OracleTable>();
		}
		this.tables = tables;
	}
	public Map<String, Object> getColumnValues() {
		return columnValues;
	}
	public void setColumnValues(Map<String, Object> columnValues) {
		this.columnValues = columnValues;
	}
}
