package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities;

/**
 * Enum representing all logical operators<br>
 * Available operators : 
 * 	<dd> i)   AND<br>
 * 	<dd> ii)  OR<br>
 * 	<dd> iii) NOT<br> 
 * <dt>
 * @author pnilayam
 *
 */
public enum LogicalOperator{
	AND("AND"),OR("OR"),NOT("NOT");
	private String expression;
	private LogicalOperator(String expression){
		this.expression=expression;
	}
	public String getExpression(){
		return expression;
	}
}
