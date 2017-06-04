package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.SQLFilters;

/**
 * @author pnilayam
 *
 */
public class OracleTable implements OracleEntity,Selectable {
	private static final String UNDERSCORE = "_";
	private String tableName;
	private String tableAlias;
	private Set<OracleColumn> columns;
	private List<JoinedTable> joinedTables;
	private List<OracleColumn> keyColumns;
	private int rank;
	
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
	public List<OracleColumn> getKeyColumns() {
		return keyColumns;
	}
	public void setKeyColumns(List<OracleColumn> keyColumns) {
		this.keyColumns = keyColumns;
	}
	public void addKeyColumn(String keyColumnName , String keyColumnType) {
		OracleColumn keyColumn= new OracleColumn();
		keyColumn.setTableAlias(tableAlias);
		keyColumn.setColumnName(keyColumnName);
		keyColumn.setColumnType(keyColumnType);
		keyColumn.setColumnAlias(tableAlias+UNDERSCORE+keyColumn.getColumnName());
		if(keyColumns==null){
			keyColumns= new ArrayList<OracleColumn>();
		}
		keyColumns.add(keyColumn);
	}
	public void addKeyColumn(OracleColumn column) {
		column.setColumnAlias(tableAlias+UNDERSCORE+column.getColumnName());
		if(keyColumns==null){
			keyColumns= new ArrayList<OracleColumn>();
		}
		keyColumns.add(column);
	}
	public String getTableAlias() {
		return tableAlias;
	}
	public void setTableAlias(String tableAlias) {
		this.tableAlias = tableAlias;
	}
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public Set<OracleColumn> getColumns() {
		if(columns==null){
			columns=new LinkedHashSet<OracleColumn>();
		}
		return columns;
	}
	
	/**
	 * @param column
	 */
	public void addColumn(OracleColumn column) {
		if(column==null){
			return;
		}
		if(null==column.getColumnAlias()||column.getColumnAlias().isEmpty()){
			column.setColumnAlias(tableAlias+UNDERSCORE+column.getColumnName());
		}
		getColumns().add(column);
	}
	/**
	 * @param columns
	 */
	public void addColumns(Set<OracleColumn> columns) {
		if(columns==null){
			return;
		}
		for(OracleColumn column : columns){
			addColumn(column);
		}
	}
	public List<JoinedTable> getJoinedTables() {
		if(joinedTables==null){
			joinedTables = new ArrayList<JoinedTable>(2);
		}
		return joinedTables;
	}
	/**
	 * @param joinedTable
	 */
	public void addJoinedTable(JoinedTable joinedTable){
		getJoinedTables().add(joinedTable);
	}
	/**
	 * @param table
	 * @param joinType
	 * @param joinConditions
	 */
	public void addJoinedTable(OracleTable table,JoinType joinType, SQLFilters joinConditions){		
		getJoinedTables().add(new JoinedTable(table, joinType, joinConditions));
	}
	/**
	 * @param table
	 * @param joinType
	 * @param joinConditions
	 */
	public void addJoinedTable(OracleTable table,String joinType, SQLFilters joinConditions){
		JoinType joinTypeEnumValue = null;
		if(null!=joinType&&!joinType.isEmpty()){
			 joinTypeEnumValue=JoinType.valueOf(joinType.toUpperCase());
		}else{
			joinTypeEnumValue=JoinType.INNER;
		}
		getJoinedTables().add(new JoinedTable(table, joinTypeEnumValue, joinConditions));
	}
	/**
	 * @param joinedTables
	 */
	public void addJoinedTables(List<JoinedTable> joinedTables) {
		getJoinedTables().addAll(joinedTables);
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tableAlias == null) ? 0 : tableAlias.hashCode());
		result = prime * result + ((tableName == null) ? 0 : tableName.hashCode());
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
		OracleTable other = (OracleTable) obj;
		if (tableAlias == null) {
			if (other.tableAlias != null)
				return false;
		} else if (!tableAlias.equals(other.tableAlias))
			return false;
		if (tableName == null) {
			if (other.tableName != null)
				return false;
		} else if (!tableName.equals(other.tableName))
			return false;
		return true;
	}
	
	
}
