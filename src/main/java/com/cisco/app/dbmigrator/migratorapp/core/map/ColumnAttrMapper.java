package com.cisco.app.dbmigrator.migratorapp.core.map;

import java.util.List;
import java.util.Map;

import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoAttribute;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.Literal;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;

/**
 * Mapper Class to Map Oracle Column to MongoDB Attribute. Holds values for
 * Default Column Value/ Sequence Name / Parent Attribute and other relevant
 * data needed for match and data transfer.
 * 
 * @author pnilayam
 *
 */
public class ColumnAttrMapper {
	private OracleColumn columnData;
	private boolean isParentColumn;
	private boolean isSeqGenerated;
	private String seqName;
	private List<String> ignoreList;
	private Literal<Object> literalValueForColumn;
	private MongoAttribute attribute;
	private boolean isParentAttribute;
	private String parentAttributeNode;
	private boolean isChildAttribute;
	private String childAttributeNode;
	private Map<String, String> replacementMap;
	/**
	 * @return the isChildAttribute
	 */
	public boolean isChildAttribute() {
		return isChildAttribute;
	}

	/**
	 * @param isChildAttribute the isChildAttribute to set
	 */
	public void setChildAttribute(boolean isChildAttribute) {
		this.isChildAttribute = isChildAttribute;
	}

	/**
	 * @return the childAttributeNode
	 */
	public String getChildAttributeNode() {
		return childAttributeNode;
	}

	/**
	 * @param childAttributeNode the childAttributeNode to set
	 */
	public void setChildAttributeNode(String childAttributeNode) {
		this.childAttributeNode = childAttributeNode;
	}
	
	public Map<String, String> getReplacementMap() {
		return replacementMap;
	}

	public void setReplacementMap(Map<String, String> replacementMap) {
		this.replacementMap = replacementMap;
	}

	public OracleColumn getColumnData() {
		return columnData;
	}

	public void setColumnData(OracleColumn columnData) {
		this.columnData = columnData;
	}

	public boolean isParentAttribute() {
		return isParentAttribute;
	}

	public void setParentAttribute(boolean isParentAttribute) {
		this.isParentAttribute = isParentAttribute;
	}

	public String getParentAttributeNode() {
		return parentAttributeNode;
	}

	public void setParentAttributeNode(String parentAttributeNode) {
		this.parentAttributeNode = parentAttributeNode;
	}

	public OracleColumn getColumn() {
		return columnData;
	}

	public void setColumn(OracleColumn columnData) {
		this.columnData = columnData;
	}

	public MongoAttribute getAttribute() {
		return attribute;
	}

	public void setAttribute(MongoAttribute attribute) {
		this.attribute = attribute;
	}

	public boolean isParentColumn() {
		return isParentColumn;
	}

	public void setParentColumn(boolean isParentColumn) {
		this.isParentColumn = isParentColumn;
	}

	public boolean isSeqGenerated() {
		return isSeqGenerated;
	}

	public void setSeqGenerated(boolean isSeqGenerated) {
		this.isSeqGenerated = isSeqGenerated;
	}

	public String getSeqName() {
		return seqName;
	}

	public void setSeqName(String seqName) {
		this.seqName = seqName;
	}

	public List<String> getIgnoreList() {
		return ignoreList;
	}

	public void setIgnoreList(List<String> ignoreList) {
		this.ignoreList = ignoreList;
	}

	public Literal<Object> getLiteralValueForColumn() {
		return literalValueForColumn;
	}

	public void setLiteralValueForColumn(Literal<Object> literalValueForColumn) {
		this.literalValueForColumn = literalValueForColumn;
	}
}
