package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.queries;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.JoinedTable;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.MatchAble;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleTable;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.QueryConstants;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.SortByColumn;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.SQLFilters;

/**
 * 
 * Class to build SELECT queries systematically.<br>
 * Example : <br>
 * <code><strong>
 * SelectQueryBuilder query = new SelectQueryBuilder();<br>
 * query.select().from(OracleTable... tables)<br>
 *				.where(SQLFilters filter)<br>
 *				.orderBy(SortByColumn... sortedColumns)<br>
 *				.getQuery();
 *</strong>
 *</code>
 *
 * @author pnilayam
 */

public class SelectQueryBuilder implements QueryBuilder {
	// private final Logger logger = Logger.getLogger(SelectQueryBuilder.class);
	private static final String SELECT_PARALLEL = "SELECT /*+PARALLEL(16)+*/ ";
	private static final String SELECT = "SELECT ";
	private static final String FROM = "FROM";
	private static final String ORDER_BY = "ORDER BY ";
	private static final String AS = "AS";
	private static final String COUNT = "COUNT(1)";
	private static final String MIN = "MIN";
	private static final String MAX = "MAX";
	private static final int MAX_IDENTIFIER_LENGTH = 27;
	private int aliasCounter=0;
	private SelectQueryComponents selectQueryComponents;

	public SelectQueryBuilder() {
		this.selectQueryComponents = new SelectQueryComponents();
		LinkedHashSet<OracleTable> tableList = new LinkedHashSet<OracleTable>();
		this.selectQueryComponents.setReferencedTables(tableList);
	}

	public AddTables select() {
		return new AddTables();
	}

	public final class AddTables {
		public AddTablesOrConditions from(OracleTable table, OracleTable... tables) {
			fromTables(table);
			fromTables(tables);
			return new AddTablesOrConditions();
		}

		private AddTables() {
		}
	}

	public final class AddTablesOrConditions {
		private AddTablesOrConditions() {
		}

		public AddTablesOrConditions from(OracleTable table, OracleTable... tables) {
			fromTables(table);
			fromTables(tables);
			return this;
		}

		public AddConditionsOrClause where(SQLFilters filter) {
			whereClause(filter);
			return new AddConditionsOrClause();
		}

		public String getQuery() {
			return build();
		}

		@SuppressWarnings("rawtypes")
		public String getCountQuery(List<MatchAble> bindValues) {
			return buildCountQuery(bindValues);
		}

		@SuppressWarnings("rawtypes")
		public String getPreparedStatement(List<MatchAble> bindValues, boolean parallel) {
			return prepareStatement(bindValues , parallel);
		}

		@SuppressWarnings("rawtypes")
		public String gMinMaxQuery(List<MatchAble> bindValues, OracleColumn rangeColumn) {
			return buildMinMaxQuery(bindValues, rangeColumn);
		}
	}

	public final class AddConditionsOrClause {
		private AddConditionsOrClause() {
		}

		public AddOrderByClause orderBy(SortByColumn... sortedColumns) {
			orderByClause(sortedColumns);
			return new AddOrderByClause();
		}

		public String getQuery() {
			return build();
		}

		@SuppressWarnings("rawtypes")
		public String getCountQuery(List<MatchAble> bindValues) {
			return buildCountQuery(bindValues);
		}

		@SuppressWarnings("rawtypes")
		public String getPreparedStatement(List<MatchAble> bindValues , boolean parallel) {
			return prepareStatement(bindValues , parallel);
		}

		@SuppressWarnings("rawtypes")
		public String getMinMaxQuery(List<MatchAble> bindValues, OracleColumn rangeColumn) {
			return buildMinMaxQuery(bindValues, rangeColumn);
		}
	}

	public final class AddOrderByClause {
		private AddOrderByClause() {
		}

		public AddOrderByClause orderBy(SortByColumn... sortedColumns) {
			orderByClause(sortedColumns);
			return new AddOrderByClause();
		}

		public String getQuery() {
			return build();
		}

		@SuppressWarnings("rawtypes")
		public String getCountQuery(List<MatchAble> bindValues) {
			return buildCountQuery(bindValues);
		}

		@SuppressWarnings("rawtypes")
		public String getMinMaxQuery(List<MatchAble> bindValues, OracleColumn rangeColumn) {
			return buildMinMaxQuery(bindValues, rangeColumn);
		}
	}

	private AddTablesOrConditions fromTables(OracleTable... tables) {
		LinkedHashSet<OracleTable> tableList = this.selectQueryComponents.getReferencedTables();
		if (tableList == null) {
			tableList = new LinkedHashSet<OracleTable>();
		}
		tableList.addAll(Arrays.asList(tables));
		this.selectQueryComponents.setReferencedTables(tableList);
		return new AddTablesOrConditions();
	}

	private AddConditionsOrClause whereClause(SQLFilters filter) {
		this.selectQueryComponents.setFilter(filter);
		return new AddConditionsOrClause();
	}

	private SelectQueryBuilder orderByClause(SortByColumn... sortedColumns) {
		LinkedHashSet<SortByColumn> sortedColumnList = this.selectQueryComponents.getSortedColumns();
		if (sortedColumnList == null) {
			sortedColumnList = new LinkedHashSet<SortByColumn>();
		}
		sortedColumnList.addAll(Arrays.asList(sortedColumns));
		this.selectQueryComponents.setSortedColumns(sortedColumnList);
		return this;
	}

	private StringBuilder addColumnsToQuery(OracleTable table, StringBuilder outputQuery) {
		Set<OracleColumn> columnSet = table.getColumns();
		for (OracleColumn column : columnSet) {
			outputQuery.append(QueryConstants.NEXT_LINE).append(QueryConstants.INDENT)
					.append(table.getTableAlias() == null ? table.getTableName() : table.getTableAlias()).append(QueryConstants.DOT)
					.append(column.getColumnName()).append(QueryConstants.SPACE).append(AS).append(QueryConstants.SPACE);
			String columnAlias = column.getColumnAlias();
			if (null != columnAlias && !columnAlias.isEmpty()) {
				if (columnAlias.length() > MAX_IDENTIFIER_LENGTH) {
					columnAlias = columnAlias.substring(0, MAX_IDENTIFIER_LENGTH)+aliasCounter++;
					column.setColumnAlias(columnAlias);
				}
			} else {
				columnAlias = column.getColumnName();
			}
			outputQuery.append(columnAlias).append(QueryConstants.COMMA);
		}
		return outputQuery;
	}

	private String build() {
		StringBuilder outputQuery = new StringBuilder();
		outputQuery.append(SELECT);
		for (OracleTable table : selectQueryComponents.getReferencedTables()) {
			addColumnsToQuery(table, outputQuery);
			List<JoinedTable> joinedTables = table.getJoinedTables();
			for (JoinedTable joinedTable : joinedTables) {
				addColumnsToQuery(joinedTable.getTable(), outputQuery);
			}
		}
		outputQuery.deleteCharAt(outputQuery.lastIndexOf(QueryConstants.COMMA)).append(QueryConstants.NEXT_LINE);
		outputQuery.append(FROM);
		for (OracleTable table : selectQueryComponents.getReferencedTables()) {
			outputQuery.append(QueryConstants.NEXT_LINE).append(QueryConstants.INDENT).append(table.getTableName()).append(QueryConstants.SPACE)
					.append(table.getTableAlias() != null ? table.getTableAlias() : QueryConstants.SPACE);
			List<JoinedTable> joinedTables = table.getJoinedTables();
			for (JoinedTable joinedTable : joinedTables) {
				outputQuery.append(joinedTable.getJoinedExpression());
			}
			outputQuery.append(QueryConstants.COMMA);
		}
		outputQuery.deleteCharAt(outputQuery.lastIndexOf(QueryConstants.COMMA)).append(QueryConstants.NEXT_LINE);
		if (selectQueryComponents.getFilter() != null) {
			outputQuery.append(QueryConstants.WHERE).append(QueryConstants.SPACE);
			outputQuery.append(selectQueryComponents.getFilter().getFilterExpression());
			outputQuery.append(QueryConstants.NEXT_LINE);
		}
		if (selectQueryComponents.getSortedColumns() != null && !selectQueryComponents.getSortedColumns().isEmpty()) {
			outputQuery.append(ORDER_BY);
			for (SortByColumn column : selectQueryComponents.getSortedColumns()) {
				outputQuery.append(column);
			}
			outputQuery.deleteCharAt(outputQuery.lastIndexOf(QueryConstants.COMMA));
		}
		return outputQuery.toString();
	}

	@SuppressWarnings("rawtypes")
	private String prepareStatement(List<MatchAble> bindValues, boolean parallel) {
		// List<MatchAble> bindValues= new ArrayList<MatchAble>();
		StringBuilder outputQuery = new StringBuilder();
		if(!parallel){
			outputQuery.append(SELECT);
		}else{
			outputQuery.append(SELECT_PARALLEL);
		}
		for (OracleTable table : selectQueryComponents.getReferencedTables()) {
			addColumnsToQuery(table, outputQuery);
			List<JoinedTable> joinedTables = table.getJoinedTables();
			for (JoinedTable joinedTable : joinedTables) {
				addColumnsToQuery(joinedTable.getTable(), outputQuery);
			}
		}
		outputQuery.deleteCharAt(outputQuery.lastIndexOf(QueryConstants.COMMA)).append(QueryConstants.NEXT_LINE);
		outputQuery.append(FROM);
		for (OracleTable table : selectQueryComponents.getReferencedTables()) {
			outputQuery.append(QueryConstants.NEXT_LINE).append(QueryConstants.INDENT).append(table.getTableName()).append(QueryConstants.SPACE)
					.append(table.getTableAlias() != null ? table.getTableAlias() : QueryConstants.SPACE);
			List<JoinedTable> joinedTables = table.getJoinedTables();
			for (JoinedTable joinedTable : joinedTables) {
				outputQuery.append(joinedTable.getJoinedExpression());
			}
			outputQuery.append(QueryConstants.COMMA);
		}
		outputQuery.deleteCharAt(outputQuery.lastIndexOf(QueryConstants.COMMA)).append(QueryConstants.NEXT_LINE);
		if (selectQueryComponents.getFilter() != null) {
			outputQuery.append(QueryConstants.WHERE).append(QueryConstants.SPACE);
			outputQuery.append(selectQueryComponents.getFilter().getPositionedExpression(bindValues));
			outputQuery.append(QueryConstants.NEXT_LINE);
		}
		if (selectQueryComponents.getSortedColumns() != null && !selectQueryComponents.getSortedColumns().isEmpty()) {
			outputQuery.append(ORDER_BY);
			for (SortByColumn column : selectQueryComponents.getSortedColumns()) {
				outputQuery.append(column);
			}
			outputQuery.deleteCharAt(outputQuery.lastIndexOf(QueryConstants.COMMA));
		}
		return outputQuery.toString();
	}

	@SuppressWarnings("rawtypes")
	private String buildCountQuery(List<MatchAble> bindValues) {
		StringBuilder outputQuery = new StringBuilder();
		outputQuery.append(SELECT).append(QueryConstants.SPACE).append(COUNT).append(QueryConstants.NEXT_LINE).append(FROM);
/*		for (OracleTable table : selectQueryComponents.getReferencedTables()) {
			outputQuery.append(QueryConstants.NEXT_LINE).append(QueryConstants.INDENT).append(table.getTableName()).append(QueryConstants.SPACE)
					.append(table.getTableAlias() != null ? table.getTableAlias() : QueryConstants.SPACE).append(QueryConstants.COMMA);
		}*/
		for (OracleTable table : selectQueryComponents.getReferencedTables()) {
			outputQuery.append(QueryConstants.NEXT_LINE).append(QueryConstants.INDENT).append(table.getTableName()).append(QueryConstants.SPACE)
					.append(table.getTableAlias() != null ? table.getTableAlias() : QueryConstants.SPACE);
			List<JoinedTable> joinedTables = table.getJoinedTables();
			if(joinedTables!=null && !joinedTables.isEmpty()){
				for (JoinedTable joinedTable : joinedTables) {
					outputQuery.append(joinedTable.getJoinedExpression());
				}
			}			
			outputQuery.append(QueryConstants.COMMA);
		}
		outputQuery.deleteCharAt(outputQuery.lastIndexOf(QueryConstants.COMMA)).append(QueryConstants.NEXT_LINE);
		if (selectQueryComponents.getFilter() != null) {
			outputQuery.append(QueryConstants.WHERE).append(QueryConstants.SPACE);
			outputQuery.append(selectQueryComponents.getFilter().getPositionedExpression(bindValues));
			outputQuery.append(QueryConstants.NEXT_LINE);
		}
		return outputQuery.toString();
	}

	@SuppressWarnings("rawtypes")
	private String buildMinMaxQuery(List<MatchAble> bindValues, OracleColumn rangeColumn) {
		StringBuilder outputQuery = new StringBuilder();
		outputQuery.append(SELECT).append(QueryConstants.SPACE).append(MIN).append(QueryConstants.OPEN_PARANTHESIS)
				.append(rangeColumn.getTableAlias()).append(QueryConstants.DOT).append(rangeColumn.getColumnName())
				.append(QueryConstants.CLOSE_PARANTHESIS).append(QueryConstants.SPACE).append(QueryConstants.COMMA).append(MAX).append(QueryConstants.OPEN_PARANTHESIS)
				.append(rangeColumn.getTableAlias()).append(QueryConstants.DOT).append(rangeColumn.getColumnName())
				.append(QueryConstants.CLOSE_PARANTHESIS).append(QueryConstants.SPACE).append(QueryConstants.NEXT_LINE).append(FROM);

		for (OracleTable table : selectQueryComponents.getReferencedTables()) {
			outputQuery.append(QueryConstants.NEXT_LINE).append(QueryConstants.INDENT).append(table.getTableName()).append(QueryConstants.SPACE)
					.append(table.getTableAlias() != null ? table.getTableAlias() : QueryConstants.SPACE);
			List<JoinedTable> joinedTables = table.getJoinedTables();
			if(joinedTables!=null && !joinedTables.isEmpty()){
				for (JoinedTable joinedTable : joinedTables) {
					outputQuery.append(joinedTable.getJoinedExpression());
				}
			}			
			outputQuery.append(QueryConstants.COMMA);
		}
		
		outputQuery.deleteCharAt(outputQuery.lastIndexOf(QueryConstants.COMMA)).append(QueryConstants.NEXT_LINE);
		if (selectQueryComponents.getFilter() != null) {
			outputQuery.append(QueryConstants.WHERE).append(QueryConstants.SPACE);
			outputQuery.append(selectQueryComponents.getFilter().getPositionedExpression(bindValues));
			outputQuery.append(QueryConstants.NEXT_LINE);
		}
		return outputQuery.toString();
	}
}
