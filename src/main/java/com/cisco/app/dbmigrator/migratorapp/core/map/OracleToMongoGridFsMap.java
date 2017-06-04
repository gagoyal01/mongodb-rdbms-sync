package com.cisco.app.dbmigrator.migratorapp.core.map;

import java.util.Map;

import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleTable;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.SQLFilters;

public class OracleToMongoGridFsMap extends SyncMap{
	private Map<String, ColumnAttrMapper> metaAttributes;
	private OracleTable streamTable;
	private String collectionName;
	private OracleColumn fileNameColumn;
	private OracleColumn inputStreamColumn;
	private SQLFilters filters;
	public SQLFilters getFilters() {
		return filters;
	}
	public void setFilters(SQLFilters filters) {
		this.filters = filters;
	}
	public Map<String, ColumnAttrMapper> getMetaAttributes() {
		return metaAttributes;
	}
	public void setMetaAttributes(Map<String, ColumnAttrMapper> metaAttributes) {
		this.metaAttributes = metaAttributes;
	}
	public OracleTable getStreamTable() {
		return streamTable;
	}
	public void setStreamTable(OracleTable streamTable) {
		this.streamTable = streamTable;
	}
	public OracleColumn getInputStreamColumn() {
		return inputStreamColumn;
	}
	public void setInputStreamColumn(OracleColumn inputStreamColumn) {
		this.inputStreamColumn = inputStreamColumn;
	}
	public String getCollectionName() {
		return collectionName;
	}
	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}
	public OracleColumn getFileNameColumn() {
		return fileNameColumn;
	}
	public void setFileNameColumn(OracleColumn fileNameColumn) {
		this.fileNameColumn = fileNameColumn;
	}
}