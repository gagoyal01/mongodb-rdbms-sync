package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.LogicalOperator;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.MatchAble;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;

/**
 * 
 * Helper Class to Add filter conditions for SQL Query<br>
 * Use static method SQLFilters.getFilter to get filter with one expression<br>
 * Example : <br>
 * <code><dd>
 * 		SQLFilters<br>
				.getFilter(	Operations.eq(MatchWithColumn.column("t1", "column1"), <br>
										    MatchWithColumn.column("t2", "column2")))<br>
				.AND(Operations.eq(MatchWithColumn.column("t1", "column3"),<br> 
									NumberLiteral.setNumber(5)))<br>
				.AND(Operations.ne(MatchWithColumn.column("t2", "column4"),<br> 
									VarcharLiteral.setVarchar("abc")));</dd>
 * </code>
 * @author pnilayam
 * 
 */
public class SQLFilters{
	
	private MatchOperation matchOperation;	
	private LinkedHashSet<LogicalOperation> logicaloperations;
	
	public SQLFilters(){}
	
	public static SQLFilters getFilter(MatchOperation operation){
		SQLFilters filter = new SQLFilters();
		filter.matchOperation= operation;
		return filter;
	}
	
	public SQLFilters OR(MatchOperation operation){
		LogicalOperation logicalOperation = new LogicalOperation(operation,LogicalOperator.OR){};
		getInitializedSet().add(logicalOperation);
		return this;
	}
	public SQLFilters addLogicalOperation(MatchOperation operation , String logicalOperator){
		LogicalOperator operator = LogicalOperator.valueOf(logicalOperator);
		LogicalOperation logicalOperation = new LogicalOperation(operation,operator){};
		getInitializedSet().add(logicalOperation);
		return this;
	}
	public SQLFilters addOperation(MatchOperation operation , String logicalOperator){
		if (null==logicalOperator ||logicalOperator.isEmpty()){
			this.matchOperation = operation;
		}else{
			addLogicalOperation(operation,logicalOperator);
		}
		return this;
	}
	public SQLFilters OR(SQLFilters subConditions){
		LogicalOperation logicalOperation = new LogicalOperation(subConditions,LogicalOperator.OR){};
		getInitializedSet().add(logicalOperation);
		return this;
	}
	
	public SQLFilters AND(MatchOperation operation){
		LogicalOperation logicalOperation = new LogicalOperation(operation,LogicalOperator.AND){};
		getInitializedSet().add(logicalOperation);
		return this;		
	}
	
	public SQLFilters AND(SQLFilters subConditions){
		LogicalOperation logicalOperation = new LogicalOperation(subConditions,LogicalOperator.AND){};
		getInitializedSet().add(logicalOperation);
		return this;		
	}
	// Will think about it later
	protected SQLFilters NOT(MatchOperation operation){
		LogicalOperation logicalOperation = new LogicalOperation(operation,LogicalOperator.NOT){};
		getInitializedSet().add(logicalOperation);
		return this;		
	}

	public MatchOperation getMatchOperation() {
		return matchOperation;
	}

	public LinkedHashSet<LogicalOperation> getLogicaloperations() {
		return logicaloperations;
	}
	private LinkedHashSet<LogicalOperation> getInitializedSet(){
		if(logicaloperations==null){
			logicaloperations = new LinkedHashSet<LogicalOperation>();
		}
		return logicaloperations;
	}
	public String getFilterExpression(){
		StringBuilder builder = new StringBuilder();	
		builder.append(matchOperation.getMatchExpression());
		if(logicaloperations!=null){
			for (LogicalOperation logicalOperation : getLogicaloperations()) {
				builder.append(logicalOperation.getLogicalExpression());
			}	
		}		
		return builder.toString();
	}
	@SuppressWarnings("rawtypes")
	public String getPositionedExpression(List<MatchAble> literals){
		StringBuilder builder = new StringBuilder();	
		builder.append(matchOperation.getPositionedExpression(literals));
		if(logicaloperations!=null){
			for (LogicalOperation logicalOperation : getLogicaloperations()) {
				builder.append(logicalOperation.getPositionedLogicalExpression(literals));
			}	
		}		
		return builder.toString();
	}
	
	public String getMergeQueryExpression(Set<OracleColumn> keyColumns){
		StringBuilder builder = new StringBuilder();
		builder.append(matchOperation.getMergeExpression(keyColumns));
		if(logicaloperations!=null){
			for (LogicalOperation logicalOperation : getLogicaloperations()) {
				builder.append(logicalOperation.getMergeExpression(keyColumns));
			}	
		}
		return builder.toString();
	}
}
