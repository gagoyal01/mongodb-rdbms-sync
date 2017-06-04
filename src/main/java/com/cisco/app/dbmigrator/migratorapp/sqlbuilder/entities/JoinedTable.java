package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities;

import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.SQLFilters;

/**
 * @author pnilayam
 *
 */
public class JoinedTable {
	@Override
	public String toString() {
		return "JoinedTable [table=" + table + ", joinType=" + joinType + ", filters=" + filters + "]";
	}
	private OracleTable table;
	private JoinType joinType;
	private SQLFilters filters;
	
	public JoinedTable(OracleTable table, JoinType joinType, SQLFilters filters) {
		super();
		this.table = table;
		this.joinType = joinType;
		this.filters = filters;
	}
	public JoinedTable(){}
	public SQLFilters getFilters() {
		return filters;
	}
	public void setFilters(SQLFilters filters) {
		this.filters = filters;
	}
	public OracleTable getTable() {
		return table;
	}
	public void setTable(OracleTable table) {
		this.table = table;
	}
	public JoinType getJoinType() {
		return joinType;
	}
	public void setJoinType(JoinType joinType) {
		this.joinType = joinType;
	}
	/* This is capable of one level join only as of now. need to write recursive logic to implement nth level of join*/
	public StringBuilder getJoinedExpression(){
		StringBuilder builder = new StringBuilder();
		builder.append(QueryConstants.SPACE)
			.append(joinType.getJoinExpression())
			.append(QueryConstants.SPACE)
			.append(table.getTableName())
			.append(QueryConstants.SPACE)
			.append(table.getTableAlias())
			.append(QueryConstants.NEXT_LINE)
			.append(QueryConstants.INDENT)
			.append(QueryConstants.INDENT)
			.append(QueryConstants.ON)
			.append(QueryConstants.SPACE)
			.append(QueryConstants.OPEN_PARANTHESIS)
			.append(filters.getFilterExpression())
			.append(QueryConstants.CLOSE_PARANTHESIS);
		return builder;
	}	
}
