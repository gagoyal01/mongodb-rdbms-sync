package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.queries;

import java.util.LinkedHashSet;

import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleTable;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.SortByColumn;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.SQLFilters;

public class SelectQueryComponents {

	private LinkedHashSet<OracleTable> referencedTables;
	private LinkedHashSet<SortByColumn> sortedColumns;
	private SQLFilters filter;

	public SQLFilters getFilter() {
		return filter;
	}

	public void setFilter(SQLFilters filter) {
		this.filter = filter;
	}

	public LinkedHashSet<SortByColumn> getSortedColumns() {
		return sortedColumns;
	}

	public void setSortedColumns(LinkedHashSet<SortByColumn> sortedColumns) {
		this.sortedColumns = sortedColumns;
	}

	public LinkedHashSet<OracleTable> getReferencedTables() {
		return referencedTables;
	}

	public void setReferencedTables(LinkedHashSet<OracleTable> referencedTables) {
		this.referencedTables = referencedTables;
	}
	
}
