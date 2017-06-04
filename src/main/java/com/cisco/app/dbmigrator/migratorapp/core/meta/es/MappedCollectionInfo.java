package com.cisco.app.dbmigrator.migratorapp.core.meta.es;

import org.bson.conversions.Bson;

public class MappedCollectionInfo {
	private String collectionName;
	private String attributeName;
	private String attributeType;
	private Bson filters;
	public String getCollectionName() {
		return collectionName;
	}
	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}
	public String getAttributeName() {
		return attributeName;
	}
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}
	public String getAttributeType() {
		return attributeType;
	}
	public void setAttributeType(String attributeType) {
		this.attributeType = attributeType;
	}
	public Bson getFilters() {
		return filters;
	}
	public void setFilters(Bson filters) {
		this.filters = filters;
	}
}