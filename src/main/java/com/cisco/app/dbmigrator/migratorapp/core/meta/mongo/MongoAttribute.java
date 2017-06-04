package com.cisco.app.dbmigrator.migratorapp.core.meta.mongo;

import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;

public class MongoAttribute implements MongoEntity{
	private String attributeName;
	private MongoAttributeType attributeType;
	private OracleColumn mappedOracleColumn;
	private boolean isIdentifier;
	private String defaultValue;
	public String getAttributeName() {
		return attributeName;
	}
	public boolean isIdentifier() {
		return isIdentifier;
	}
	public void setIdentifier(boolean isIdentifier) {
		this.isIdentifier = isIdentifier;
	}
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}
	public String getAttributeType() {
		return String.valueOf(attributeType);
	}
	public void setAttributeType(String attributeType) {
		this.attributeType = MongoAttributeType.valueOf(attributeType);
	}
	public void setAttributeType(MongoAttributeType attributeType) {
		this.attributeType = attributeType;
	}
	public OracleColumn getMappedOracleColumn() {
		return mappedOracleColumn;
	}
	public void setMappedOracleColumn(OracleColumn mappedOracleColumn) {
		this.mappedOracleColumn = mappedOracleColumn;
	}
	public String getDefaultValue() {
		return defaultValue;
	}
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
	@Override
	public String toString() {
		return "MongoAttribute [attributeName=" + attributeName + ", attributeType=" + attributeType
				+ ", mappedOracleColumn=" + mappedOracleColumn + "]";
	}
	@Override
	public String getEntityType() {
		return String.valueOf(attributeType);
	}
}
