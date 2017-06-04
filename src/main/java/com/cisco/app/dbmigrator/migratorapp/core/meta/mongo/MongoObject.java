package com.cisco.app.dbmigrator.migratorapp.core.meta.mongo;

import java.util.ArrayList;
import java.util.List;

import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleTable;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.SQLFilters;

/**
 * @author pnilayam
 *
 */
public class MongoObject implements MongoEntity{
	private String collectionName;
	private String collectionType;
	private List<OracleTable> sourceTables;
	private List<MongoEntity> attributes;
	private List<MongoEntity> identifierList;
	private SQLFilters filters;
	private List<OracleColumn> referencedColumns;	
	public List<OracleColumn> getReferencedColumns() {
		return referencedColumns;
	}
	public void setReferencedColumns(List<OracleColumn> referencedColumns) {
		this.referencedColumns = referencedColumns;
	}
	public String getCollectionType() {
		return collectionType;
	}
	public void setCollectionType(String collectionType) {
		this.collectionType = collectionType;
	}
	public List<MongoEntity> getIdentifierList() {
		return identifierList;
	}
	public void setIdentifierList(List<MongoEntity> identifierList) {
		this.identifierList = identifierList;
	}
	public SQLFilters getFilters() {
		return filters;
	}
	public void setFilters(SQLFilters filters) {
		this.filters = filters;
	}
	public String getCollectionName() {
		return collectionName;
	}
	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}
	public List<OracleTable> getSourceTables() {
		return sourceTables;
	}
	public void setSourceTables(List<OracleTable> sourceTables) {
		this.sourceTables = sourceTables;
	}
	public List<MongoEntity> getAttributes() {
		initializeAttributeList();
		return attributes;
	}
	public void setAttributes(List<MongoEntity> attributes) {
		this.attributes = attributes;
	}
	private void initializeAttributeList(){
		if(attributes==null){
			attributes=new ArrayList<MongoEntity>();
		}
	}
	private void initializeIdentifierList(){
		if(identifierList==null){
			identifierList=new ArrayList<MongoEntity>();
		}
	}
	public void addEntity(MongoEntity entity) {
		initializeAttributeList();
		this.attributes.add(entity);
	}
	public void addIdentifierEntity(MongoEntity entity) {
		initializeIdentifierList();
		this.identifierList.add(entity);
	}
	@Override
	public String getEntityType() {
		return collectionType;
	}
	public void addReferencedColumns(OracleColumn column) {
		initializeReferencedColumns();
		this.referencedColumns.add(column);
	}
	private void initializeReferencedColumns(){
		if(referencedColumns==null){
			referencedColumns=new ArrayList<OracleColumn>();
		}
	}
}
