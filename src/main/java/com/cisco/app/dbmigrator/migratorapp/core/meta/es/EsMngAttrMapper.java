package com.cisco.app.dbmigrator.migratorapp.core.meta.es;

import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoAttribute;

public class EsMngAttrMapper {
	private MongoAttribute mongoAttribute;
	private EsAttribute esAttribute;
	private String collectionName;
	public String getCollectionName() {
		return collectionName;
	}
	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}
	public MongoAttribute getMongoAttribute() {
		return mongoAttribute;
	}
	public void setMongoAttribute(MongoAttribute mongoAttribute) {
		this.mongoAttribute = mongoAttribute;
	}
	public EsAttribute getEsAttribute() {
		return esAttribute;
	}
	public void setEsAttribute(EsAttribute esAttribute) {
		this.esAttribute = esAttribute;
	}	
}
