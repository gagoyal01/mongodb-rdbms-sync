package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents;

import java.util.List;
import java.util.Set;

import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.LogicalOperator;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.MatchAble;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.QueryConstants;

/**
 * @author pnilayam
 *
 */
public abstract class LogicalOperation {

	private MatchOperation matchOperation;
	private SQLFilters subConditions;
	private LogicalOperator operator;
	public LogicalOperation(MatchOperation matchOperation,LogicalOperator operator) {
		super();
		this.matchOperation = matchOperation;
		this.operator=operator;
	}
	public LogicalOperation(SQLFilters subConditions,LogicalOperator operator) {
		super();
		this.subConditions = subConditions;
		this.operator=operator;
	}
	public MatchOperation getMatchOperation() {
		return matchOperation;
	}
	public LogicalOperator getOperator() {
		return operator;
	}
	public SQLFilters getOperation() {
		return subConditions;
	}	
	/**
	 * @return
	 */
	public String getLogicalExpression() {
		StringBuilder builder = new StringBuilder();	
		builder.append(QueryConstants.NEXT_LINE).append(QueryConstants.SPACE).append(getOperator().getExpression()).append(QueryConstants.SPACE);
		if(matchOperation!=null){
			builder.append(matchOperation.getMatchExpression());	
		}
		else if(subConditions!=null){
			builder.append(QueryConstants.OPEN_PARANTHESIS);
			builder.append(subConditions.getFilterExpression());
			builder.append(QueryConstants.CLOSE_PARANTHESIS);
		}
		return builder.toString();
	}
	/**
	 * @param literals
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public String getPositionedLogicalExpression(List<MatchAble> literals) {
		StringBuilder builder = new StringBuilder();	
		builder.append(QueryConstants.NEXT_LINE).append(QueryConstants.SPACE).append(getOperator().getExpression()).append(QueryConstants.SPACE);
		if(matchOperation!=null){
			builder.append(matchOperation.getPositionedExpression(literals));	
		}
		else if(subConditions!=null){
			builder.append(QueryConstants.OPEN_PARANTHESIS);
			builder.append(subConditions.getPositionedExpression(literals));
			builder.append(QueryConstants.CLOSE_PARANTHESIS);
		}
		return builder.toString();
	}
	public String getMergeExpression(Set<OracleColumn> keyColumns) {
		StringBuilder builder = new StringBuilder();
		builder.append(QueryConstants.NEXT_LINE).append(QueryConstants.SPACE).append(getOperator().getExpression()).append(QueryConstants.SPACE);
		if(matchOperation!=null){
			builder.append(matchOperation.getMergeExpression(keyColumns));	
		}
		else if(subConditions!=null){
			builder.append(QueryConstants.OPEN_PARANTHESIS);
			builder.append(subConditions.getMergeQueryExpression(keyColumns));
			builder.append(QueryConstants.CLOSE_PARANTHESIS);
		}
		return builder.toString();
	}
}
