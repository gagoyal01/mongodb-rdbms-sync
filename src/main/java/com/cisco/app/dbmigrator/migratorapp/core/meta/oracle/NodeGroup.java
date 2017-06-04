package com.cisco.app.dbmigrator.migratorapp.core.meta.oracle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.cisco.app.dbmigrator.migratorapp.core.map.ColumnAttrMapper;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleTable;

public class NodeGroup implements Comparable<NodeGroup>{
	private String nodeName;
	private List<OracleTable> tableList;
	private Map<String , ColumnAttrMapper> columnAttrMappers;
	private List<String> referenceAttributes;
	private List<NodeGroup> childGroups;
	private String nodeType;//ARRAY/COLLECTIONS
	private ColumnAttrMapper parentColumnMapper;
	/**
	 * @return the nodeType
	 */
	public String getNodeType() {
		return nodeType;
	}
	/**
	 * @param nodeType the nodeType to set
	 */
	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}
	/**
	 * @return the parentColumnMapper
	 */
	public ColumnAttrMapper getParentColumnMapper() {
		return parentColumnMapper;
	}
	/**
	 * @param parentColumnMapper the parentColumnMapper to set
	 */
	public void setParentColumnMapper(ColumnAttrMapper parentColumnMapper) {
		this.parentColumnMapper = parentColumnMapper;
	}
	private int rank;
	/**
	 * @return the childGroups
	 */
	public List<NodeGroup> getChildGroups() {
		return childGroups;
	}
	/**
	 * @param childGroups the childGroups to set
	 */
	public void setChildGroups(List<NodeGroup> childGroups) {
		this.childGroups = childGroups;
	}
	/**
	 * @param childGroups ChildGroup to add to existing list
	 */
	public void addChildGroup(NodeGroup childGroup) {
		if(childGroups==null){
			childGroups= new ArrayList<NodeGroup>();
		}
		this.childGroups.add(childGroup);
	}
	/**
	 * @return the rank
	 */
	public int getRank() {
		return rank;
	}
	/**
	 * @param rank the rank to set
	 */
	public void setRank(int rank) {
		this.rank = rank;
	}
	public List<String> getReferenceAttributes() {
		return referenceAttributes;
	}
	public void setReferenceAttributes(List<String> referenceAttributes) {
		this.referenceAttributes = referenceAttributes;
	}
	public String getNodeName() {
		return nodeName;
	}
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}
	public List<OracleTable> getTableList() {
		return tableList;
	}
	public void setTableList(List<OracleTable> tableList) {
		this.tableList = tableList;
	}
	public Map<String, ColumnAttrMapper> getColumnAttrMappers() {
		return columnAttrMappers;
	}
	public void setColumnAttrMappers(Map<String, ColumnAttrMapper> columnAttrMappers) {
		this.columnAttrMappers = columnAttrMappers;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((columnAttrMappers == null) ? 0 : columnAttrMappers.hashCode());
		result = prime * result + ((nodeName == null) ? 0 : nodeName.hashCode());
		result = prime * result + ((referenceAttributes == null) ? 0 : referenceAttributes.hashCode());
		result = prime * result + ((tableList == null) ? 0 : tableList.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NodeGroup other = (NodeGroup) obj;
		if (columnAttrMappers == null) {
			if (other.columnAttrMappers != null)
				return false;
		} else if (!columnAttrMappers.equals(other.columnAttrMappers))
			return false;
		if (nodeName == null) {
			if (other.nodeName != null)
				return false;
		} else if (!nodeName.equals(other.nodeName))
			return false;
		if (referenceAttributes == null) {
			if (other.referenceAttributes != null)
				return false;
		} else if (!referenceAttributes.equals(other.referenceAttributes))
			return false;
		if (tableList == null) {
			if (other.tableList != null)
				return false;
		} else if (!tableList.equals(other.tableList))
			return false;
		return true;
	}
	@Override
	public int compareTo(NodeGroup o) {
		String DOT = ".";
		if(o==null){
			return 1;
		}
		if(nodeName.contains(DOT) && o.getNodeName().contains(DOT)){
			String currentNodeParent = nodeName.substring(0, nodeName.lastIndexOf(DOT));
			String comparedNodeParent = o.getNodeName().substring(0, o.getNodeName().lastIndexOf(DOT));
			int compVal= currentNodeParent.compareToIgnoreCase(comparedNodeParent);
			if(compVal==0){
				return this.rank-o.getRank();
			}else{
				return compVal;
			}
		}else{
			return this.nodeName.compareToIgnoreCase(o.getNodeName());
		}
	}
}
