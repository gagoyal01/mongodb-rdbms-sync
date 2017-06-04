package com.cisco.app.dbmigrator.migratorapp.core.meta.es;

public class EsAttribute implements EsEntity{
	
	private String attributeName;
	private String attributeType;
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
	@Override
	public String getEntityType() {
		return String.valueOf(EsEntityTypes.EsAttribute);
	}
}
