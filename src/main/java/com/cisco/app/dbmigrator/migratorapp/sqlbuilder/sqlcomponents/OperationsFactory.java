package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents;

import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.MatchAble;
@SuppressWarnings("rawtypes")
public class OperationsFactory {

	public static MatchOperation getMatchExpression(MatchAble leftExpression, MatchAble rightExpression,
			String operationType) {

		if ("eq".equalsIgnoreCase(operationType)) {
			return Operations.eq(leftExpression, rightExpression);
		} else if ("gt".equalsIgnoreCase(operationType)) {
			return Operations.gt(leftExpression, rightExpression);
		} 
		else if ("gte".equalsIgnoreCase(operationType)) {
			return Operations.gte(leftExpression, rightExpression);
		} 
		else if ("lt".equalsIgnoreCase(operationType)) {
			return Operations.lt(leftExpression, rightExpression);
		} 
		else if ("lte".equalsIgnoreCase(operationType)) {
			return Operations.lte(leftExpression, rightExpression);
		} 
		else if ("ne".equalsIgnoreCase(operationType)) {
			return Operations.ne(leftExpression, rightExpression);
		} 
		else if ("IS_NULL".equalsIgnoreCase(operationType)) {
			return Operations.isNull(leftExpression);
		} 
		else if ("IS_NOT_NULL".equalsIgnoreCase(operationType)) {
			return Operations.isNotNull(leftExpression);
		}
		else if ("like".equalsIgnoreCase(operationType)) {
			return Operations.like(leftExpression, rightExpression);
		}
		else {
			return Operations.eq(leftExpression, rightExpression);
		}

	}

}
