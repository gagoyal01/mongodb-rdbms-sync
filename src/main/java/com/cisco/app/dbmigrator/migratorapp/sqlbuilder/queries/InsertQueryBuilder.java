package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.queries;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleTable;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.QueryConstants;

public class InsertQueryBuilder {
	private static final String INSERT_ALL = "INSERT ALL ";
	private static final String INSERT = "INSERT ";
	private static final String INTO = "INTO";
	private static final String VALUES = "VALUES";
	private static final String SELECT_FROM_DUAL = "SELECT 1 FROM DUAL";

	private InsertQueryComponents insertQueryComponents;

	public InsertQueryBuilder() {
		super();
		this.insertQueryComponents = new InsertQueryComponents();
		insertQueryComponents.setTables(new LinkedHashSet<OracleTable>());
		insertQueryComponents.setColumnValues(new HashMap<String, Object>());
	}

	public AddAllTables insertAll() {
		return new AddAllTables();
	}

	public AddTable insert() {
		return new AddTable();
	}

	public final class AddTable {
		private AddTable() {
		}

		public GetInsertQuery into(OracleTable table) {
			insertQueryComponents.getTables().add(table);
			return new GetInsertQuery();
		}
	}

	public final class AddAllTables {
		private AddAllTables() {
		}

		public GetInsertAllQuery intoTables(OracleTable table, OracleTable... tables) {
			insertQueryComponents.getTables().add(table);
			insertQueryComponents.getTables().addAll(Arrays.asList(tables));
			return new GetInsertAllQuery();
		}

		public GetInsertAllQuery intoTables(List<OracleTable> tables) {
			insertQueryComponents.getTables().addAll(tables);
			return new GetInsertAllQuery();
		}
	}

	public final class GetInsertQuery {
		private GetInsertQuery() {
		}

		public String getQuery(Set<String> columnAliasSet) {
			return prepareInsertStatement(columnAliasSet);
		}
	}

	public final class GetInsertAllQuery {
		private GetInsertAllQuery() {
		}

		public String getQuery(Set<String> columnAliasSet) {
			return prepareInsertAllStatement(columnAliasSet);
		}
	}

	private String prepareInsertAllStatement(Set<String> columnAliasSet) {
		StringBuilder builder = new StringBuilder();
		builder.append(INSERT_ALL);
		Set<OracleTable> tableSet = insertQueryComponents.getTables();
		for (OracleTable table : tableSet) {
			builder.append(QueryConstants.NEXT_LINE).append(QueryConstants.INDENT);
			builder.append(INTO).append(QueryConstants.SPACE).append(table.getTableName())
					.append(QueryConstants.OPEN_PARANTHESIS);
			for (OracleColumn column : table.getColumns()) {
				builder.append(column.getColumnName()).append(QueryConstants.COMMA);
			}
			builder.deleteCharAt(builder.lastIndexOf(QueryConstants.COMMA)).append(QueryConstants.CLOSE_PARANTHESIS);
			builder.append(QueryConstants.SPACE).append(VALUES).append(QueryConstants.OPEN_PARANTHESIS);
			for (OracleColumn column : table.getColumns()) {
				builder.append(QueryConstants.COLON).append(column.getColumnAlias()).append(QueryConstants.COMMA);
				columnAliasSet.add(column.getColumnAlias());
			}
			builder.deleteCharAt(builder.lastIndexOf(QueryConstants.COMMA)).append(QueryConstants.CLOSE_PARANTHESIS);
		}
		builder.append(QueryConstants.NEXT_LINE).append(SELECT_FROM_DUAL);
		return builder.toString();
	}

	private String prepareInsertStatement(Set<String> columnAliasSet) {
		StringBuilder builder = new StringBuilder();
		OracleTable table = insertQueryComponents.getTables().iterator().next();
		builder.append(INSERT);		
		builder.append(QueryConstants.NEXT_LINE).append(QueryConstants.INDENT);
		builder.append(INTO).append(QueryConstants.SPACE).append(table.getTableName())
				.append(QueryConstants.OPEN_PARANTHESIS);
		for (OracleColumn column : table.getColumns()) {
			builder.append(column.getColumnName()).append(QueryConstants.COMMA);
		}
		builder.deleteCharAt(builder.lastIndexOf(QueryConstants.COMMA)).append(QueryConstants.CLOSE_PARANTHESIS);
		builder.append(QueryConstants.SPACE).append(VALUES).append(QueryConstants.OPEN_PARANTHESIS);
		for (OracleColumn column : table.getColumns()) {
			builder.append(QueryConstants.COLON).append(column.getColumnAlias()).append(QueryConstants.COMMA);
			columnAliasSet.add(column.getColumnAlias());
		}
		builder.deleteCharAt(builder.lastIndexOf(QueryConstants.COMMA)).append(QueryConstants.CLOSE_PARANTHESIS);
		return builder.toString();
	}
}
