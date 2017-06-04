package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents;

import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.Literal;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.LogicalOperator;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.MatchAble;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.MatchOperator;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.QueryConstants;
@SuppressWarnings("rawtypes")
public class Operations {
	public static MatchOperation eq(MatchAble leftExpression, MatchAble rightExpression){
		MatchOperation operation = new MatchOperation(leftExpression, rightExpression, MatchOperator.EQ){};
		return operation;		
	}
	public static MatchOperation gt(MatchAble leftExpression, MatchAble rightExpression){
		MatchOperation operation = new MatchOperation(leftExpression, rightExpression, MatchOperator.GT){};
		return operation;		
	}
	public static MatchOperation gte(MatchAble leftExpression, MatchAble rightExpression){
		MatchOperation operation = new MatchOperation(leftExpression, rightExpression, MatchOperator.GTE){};
		return operation;		
	}
	public static MatchOperation lt(MatchAble leftExpression, MatchAble rightExpression){
		MatchOperation operation = new MatchOperation(leftExpression, rightExpression, MatchOperator.LT){};
		return operation;		
	}
	public static MatchOperation lte(MatchAble leftExpression, MatchAble rightExpression){
		MatchOperation operation = new MatchOperation(leftExpression, rightExpression, MatchOperator.LTE){};
		return operation;		
	}
	public static MatchOperation ne(MatchAble leftExpression, MatchAble rightExpression){
		MatchOperation operation = new MatchOperation(leftExpression, rightExpression, MatchOperator.NE){};
		return operation;		
	}
	public static MatchOperation isNull(MatchAble leftExpression){
		MatchOperation operation = new MatchOperation(leftExpression, null, MatchOperator.IS_NULL){};
		return operation;		
	}
	public static MatchOperation isNotNull(MatchAble leftExpression){
		MatchOperation operation = new MatchOperation(leftExpression, null, MatchOperator.IS_NOT_NULL){};
		return operation;		
	}
	public static MatchOperation like(MatchAble leftExpression, MatchAble rightExpression){
		MatchOperation operation = new MatchOperation(leftExpression, rightExpression, MatchOperator.LIKE){};
		return operation;		
	}
	public static MatchOperation between(OracleColumn leftExpression, Literal from, Literal upto){
		final StringBuilder builder = new StringBuilder();
		builder.append(from.getLiteralValue()).append(QueryConstants.SPACE).append(LogicalOperator.AND.getExpression())
				.append(QueryConstants.SPACE).append(upto.getLiteralValue());
		Literal literal =new Literal() {
			public String getLiteralValue() {
				return builder.toString();
			}

			public Literal setLiteralValue(Object value) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getLiteralType() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getSqlExpressionForMatchable() {
				// TODO Auto-generated method stub
				return null;
			}
		};		
		MatchOperation operation = new MatchOperation(leftExpression, literal, MatchOperator.BETWEEN){};
		return operation;		
	}
	public static MatchOperation exists(String subQuery){
		final StringBuilder builder = new StringBuilder();
		builder.append(QueryConstants.OPEN_PARANTHESIS).append(subQuery).append(QueryConstants.CLOSE_PARANTHESIS);
		Literal literal =new Literal() {
			public String getLiteralValue() {
				return builder.toString();
			}

			public Literal setLiteralValue(Object value) {
				// TODO Auto-generated method stub
				return null;
			}


			@Override
			public String getLiteralType() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getSqlExpressionForMatchable() {
				// TODO Auto-generated method stub
				return null;
			}
		};	
		MatchOperation operation = new MatchOperation(null, literal, MatchOperator.EXISTS){};
		return operation;		
	}
}
