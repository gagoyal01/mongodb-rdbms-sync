package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.queries;

import java.util.HashSet;
import java.util.Set;

import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleTable;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.QueryConstants;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.SQLFilters;

public class MergeQueryBuilder {
	private static final String MERGE_INTO = "MERGE INTO ";
	private static final String USING = "USING";
	private static final String ON = "ON";
	private static final String MATCHED = "WHEN MATCHED THEN";
	private static final String NOT_MATCHED = "WHEN NOT MATCHED THEN";
	private static final String INSERT = "INSERT";
	private static final String UPDATE = "UPDATE";
	private static final String SET = "SET";
	private static final String VALUES = "VALUES";
	private static final String SELECT_FROM_DUAL = "SELECT 1 FROM DUAL";

	private final MergeQueryComponents queryComponents;

	private MergeQueryBuilder() {
		super();
		this.queryComponents = new MergeQueryComponents();
	}
	
	public static MergeQueryBuilder getBuilder(){
		return new MergeQueryBuilder();
	}

	public AddTargetTable merge() {
		return new AddTargetTable();
	}

	public final class AddTargetTable {
		private AddTargetTable() {
		}

		public AddSourceTable into(OracleTable targetTable) {
			queryComponents.setTargetTable(targetTable);
			return new AddSourceTable();
		}
	}

	public final class AddSourceTable {
		private AddSourceTable() {
		}

		public AddFilter using(OracleTable sourceTable) {
			queryComponents.setSourceTable(sourceTable);
			return new AddFilter();
		}

		public AddFilter usingDual() {
			return new AddFilter();
		}
	}

	public final class AddFilter {
		private AddFilter() {
		}

		public GetQuery on(SQLFilters filters) {
			queryComponents.setFilters(filters);
			return new GetQuery();
		}
	}

	public final class GetQuery {
		private GetQuery() {
		}

		public String getMergeQuery(Set<String> columnAliasSet) {
			return buildQuery(columnAliasSet);
		}
	}

	private String buildQuery(Set<String> columnAliasSet) {
		StringBuilder builder = new StringBuilder(MERGE_INTO);
		OracleTable targetTable = queryComponents.getTargetTable();
		builder.append(targetTable.getTableName()).append(QueryConstants.SPACE).append(targetTable.getTableAlias())
				.append(QueryConstants.NEXT_LINE).append(QueryConstants.INDENT).append(USING)
				.append(QueryConstants.OPEN_PARANTHESIS).append(SELECT_FROM_DUAL)
				.append(QueryConstants.CLOSE_PARANTHESIS).append(QueryConstants.NEXT_LINE).append(ON)
				.append(QueryConstants.SPACE).append(QueryConstants.OPEN_PARANTHESIS);
		Set<OracleColumn> keyColumns= new HashSet<OracleColumn>();
		/*//TODO : This is temp. build proper logic to process SQL filters for Named notation 
		
		OracleColumn keyColumn = (OracleColumn) queryComponents.getFilters().getMatchOperation().getLeftExpression();
		builder.append(keyColumn.getSqlExpressionForMatchable()).append(QueryConstants.SPACE).append(QueryConstants.EQUALS)
				.append(QueryConstants.SPACE).append(QueryConstants.COLON).append(keyColumn.getColumnAlias());*/

		builder.append(queryComponents.getFilters().getMergeQueryExpression(keyColumns))
				.append(QueryConstants.CLOSE_PARANTHESIS).append(QueryConstants.NEXT_LINE).append(QueryConstants.INDENT)
				.append(MATCHED).append(QueryConstants.NEXT_LINE).append(UPDATE).append(QueryConstants.SPACE)
				.append(SET).append(QueryConstants.NEXT_LINE);
		for (OracleColumn column : targetTable.getColumns()) {
			if(!keyColumns.contains(column) ){
				builder.append(column.getColumnName()).append(QueryConstants.SPACE).append(QueryConstants.EQUALS)
				.append(QueryConstants.SPACE).append(QueryConstants.COLON).append(column.getColumnAlias())
				.append(QueryConstants.COMMA).append(QueryConstants.NEXT_LINE);
			}
			columnAliasSet.add(column.getColumnAlias());
		}
		if(builder.lastIndexOf(QueryConstants.COMMA) >=0)
			builder.deleteCharAt(builder.lastIndexOf(QueryConstants.COMMA));
				builder.append(QueryConstants.INDENT).append(NOT_MATCHED).append(QueryConstants.NEXT_LINE).append(INSERT)
				.append(QueryConstants.NEXT_LINE).append(QueryConstants.INDENT).append(QueryConstants.OPEN_PARANTHESIS);
		for (OracleColumn column : targetTable.getColumns()) {
			builder.append(column.getColumnName()).append(QueryConstants.COMMA);
		}
		builder.deleteCharAt(builder.lastIndexOf(QueryConstants.COMMA)).append(QueryConstants.CLOSE_PARANTHESIS);
		builder.append(QueryConstants.SPACE).append(VALUES).append(QueryConstants.OPEN_PARANTHESIS);
		for (OracleColumn column : targetTable.getColumns()) {
			builder.append(QueryConstants.COLON).append(column.getColumnAlias()).append(QueryConstants.COMMA);
		}
		builder.deleteCharAt(builder.lastIndexOf(QueryConstants.COMMA)).append(QueryConstants.CLOSE_PARANTHESIS);

		return builder.toString();
	}
}
