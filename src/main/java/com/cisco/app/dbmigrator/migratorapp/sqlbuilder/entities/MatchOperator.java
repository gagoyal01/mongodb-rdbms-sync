package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities;

/**
 * Enum representing all Match operators<br>
 * Available operators :
 * <dd>EQ("=")<br>
 * NE("!=")<br>
 * GT(">")<br>
 * GTE(">=")<br>
 * LT("<")<br>
 * LTE("<=")<br>
 * LIKE("LIKE")<br>
 * EXISTS("EXISTS")<br>
 * IS_NULL("IS NULL")<br>
 * IS_NOT_NULL("IS NOT NULL")<br>
 * BETWEEN("BETWEEN") 
 * </dd> 
 * 
 * @author pnilayam
 *
 */
public enum MatchOperator{
	EQ("="),NE("!="),GT(">"),GTE(">="),LT("<"),LTE("<="),LIKE("LIKE"),EXISTS("EXISTS"),IS_NULL("IS NULL"),IS_NOT_NULL("IS NOT NULL"),BETWEEN("BETWEEN");
	private String expression;
	private MatchOperator(String expression){
		this.expression=expression;
	}
	public String getExpression(){
		return expression;
	}
}
