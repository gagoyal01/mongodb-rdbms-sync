package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents;

import java.util.List;
import java.util.Set;

import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.Literal;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.MatchAble;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.MatchOperator;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.QueryConstants;

/**
 * Helper Class to represent any match operation.<br>
 * Contains three attributes :<br>
 * a) MatchAble leftExpression : Can be a column or a literal<br>
 * b) MatchAble rightExpression : Can be a column or a literal<br>
 * c) MatchOperator operator
 * 
 * @author pnilayam
 *
 */
@SuppressWarnings("rawtypes")
public abstract class MatchOperation {
	protected MatchAble leftExpression;
	protected MatchAble rightExpression;
	protected MatchOperator operator;

	public MatchAble getLeftExpression() {
		return leftExpression;
	}

	public void setLeftExpression(MatchAble leftExpression) {
		this.leftExpression = leftExpression;
	}

	public MatchAble getRightExpression() {
		return rightExpression;
	}

	public void setRightExpression(MatchAble rightExpression) {
		this.rightExpression = rightExpression;
	}

	public MatchOperator getOperator() {
		return operator;
	}

	public void setOperator(MatchOperator operator) {
		this.operator = operator;
	}

	public MatchOperation(MatchAble leftExpression, MatchAble rightExpression, MatchOperator operator) {
		this.leftExpression = leftExpression;
		this.rightExpression = rightExpression;
		this.operator = operator;
	}

	public StringBuilder getMatchExpression() {
		StringBuilder builder = new StringBuilder();
		builder.append(leftExpression.getSqlExpressionForMatchable());
		builder.append(QueryConstants.SPACE).append(operator.getExpression()).append(QueryConstants.SPACE);
		builder.append(rightExpression.getSqlExpressionForMatchable());
		return builder;
	}

	public StringBuilder getPositionedExpression(List<MatchAble> literals) {
		StringBuilder builder = new StringBuilder();
		builder.append(evaluateExpression(leftExpression, literals));
		builder.append(QueryConstants.SPACE).append(operator.getExpression()).append(QueryConstants.SPACE);
		if(rightExpression!=null){
			builder.append(evaluateExpression(rightExpression, literals));	
		}		
		return builder;
	}

	private String evaluateExpression(MatchAble matchAble, List<MatchAble> literals) {
		String expression = null;
		if (matchAble instanceof OracleColumn) {
			OracleColumn column = (OracleColumn) matchAble;
			if (column.isParentColumn()) {
				expression = QueryConstants.BIND_MARKER;
				literals.add(matchAble);
			} else {
				expression = (String) matchAble.getSqlExpressionForMatchable();
			}
		} else if (matchAble instanceof Literal) {
			literals.add(matchAble);
			expression = QueryConstants.BIND_MARKER;
		}
		return expression;
	}

	public Object getMergeExpression(Set<OracleColumn> keyColumns) {
		StringBuilder builder = new StringBuilder();
		OracleColumn keyColumn = (OracleColumn) leftExpression;
		keyColumns.add(keyColumn);
		if (!keyColumn.isNullable()) {
			builder.append(keyColumn.getSqlExpressionForMatchable()).append(QueryConstants.SPACE)
					.append(QueryConstants.EQUALS).append(QueryConstants.SPACE).append(QueryConstants.COLON)
					.append(keyColumn.getColumnAlias());
		} else {
			builder.append(QueryConstants.NVL).append(QueryConstants.OPEN_PARANTHESIS)
					.append(keyColumn.getSqlExpressionForMatchable()).append(QueryConstants.SPACE)
					.append(QueryConstants.COMMA).append("0").append(QueryConstants.CLOSE_PARANTHESIS)
					.append(QueryConstants.SPACE).append(QueryConstants.EQUALS).append(QueryConstants.SPACE)
					.append(QueryConstants.NVL).append(QueryConstants.OPEN_PARANTHESIS).append(QueryConstants.COLON)
					.append(keyColumn.getColumnAlias()).append(QueryConstants.SPACE).append(QueryConstants.COMMA)
					.append("0").append(QueryConstants.CLOSE_PARANTHESIS);
		}
		return builder;
	}
}
