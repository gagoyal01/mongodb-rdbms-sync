package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities;

/**
 * @author pnilayam
 *
 */
public enum JoinType {
	INNER("JOIN"), LEFT_OUTER("LEFT OUTER JOIN"), RIGHT_OUTER("RIGHT OUTER JOIN"), FULL_OUTER("FULL OUTER JOIN");
	String joinExpression;

	private JoinType(String joinExpression) {
		this.joinExpression = joinExpression;
	}
	
	public String getJoinExpression(){
		return joinExpression;
	}
}
