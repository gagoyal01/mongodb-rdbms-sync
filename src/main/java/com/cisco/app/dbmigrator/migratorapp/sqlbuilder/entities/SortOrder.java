package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities;

public enum SortOrder {
	ASCENDING("ASC"), DESCECNDING("DESC");
	String sortOrder;

	private SortOrder(String sortOrder) {
		this.sortOrder = sortOrder;
	}

	public String sortOrder() {
		return sortOrder;
	}
}