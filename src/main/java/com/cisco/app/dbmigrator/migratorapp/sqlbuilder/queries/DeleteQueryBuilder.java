package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.queries;

import java.util.HashSet;
import java.util.Set;

import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.QueryConstants;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.SQLFilters;

public class DeleteQueryBuilder implements QueryBuilder {
	private static final String DELETE_FROM = "DELETE FROM ";
	String FROM = "FROM";

	private DeleteQueryComponents queryComponents;

	public DeleteQueryBuilder() {
		this.queryComponents = new DeleteQueryComponents();
	}

	public FromTable delete() {
		return new FromTable();
	}

	public final class FromTable {
		private FromTable() {
		}

		public AddFilter from(String tableName, String tableAlias) {
			fromTable(tableName, tableAlias);
			return new AddFilter();
		}
	}

	public final class AddFilter {
		private AddFilter() {
		}

		public GetQuery where(SQLFilters filters) {
			whereCondition(filters);
			return new GetQuery();
		}

		public String getQuery(Set<String> columnAliasSet) {
			return buildQuery(columnAliasSet);
		}
	}

	public final class GetQuery {
		private GetQuery() {
		}

		public String getQuery(Set<String> columnAliasSet) {
			return buildQuery(columnAliasSet);
		}
	}

	private void fromTable(String tableName, String tableAlias) {
		queryComponents.setTableName(tableName);
		queryComponents.setTableAlias(tableAlias);
	}

	private void whereCondition(SQLFilters filters) {
		queryComponents.setFilter(filters);
	}

	private String buildQuery(Set<String> columnAliasSet) {
		StringBuilder builder = new StringBuilder(DELETE_FROM);
		builder.append(queryComponents.getTableName()).append(QueryConstants.SPACE)
				.append(queryComponents.getTableAlias()).append(QueryConstants.NEXT_LINE).append(QueryConstants.INDENT)
				.append(QueryConstants.WHERE).append(QueryConstants.SPACE);

		Set<OracleColumn> keyColumns = new HashSet<OracleColumn>();		
		builder.append(queryComponents.getFilter().getMergeQueryExpression(keyColumns));
		//TODO : need to revisit this
		for(OracleColumn column : keyColumns){
			columnAliasSet.add(column.getColumnAlias());
		}
		return builder.toString();
	}

	private final class DeleteQueryComponents {
		private DeleteQueryComponents() {
		}

		private String tableName;
		private String tableAlias;

		public String getTableAlias() {
			return tableAlias;
		}

		public void setTableAlias(String tableAlias) {
			this.tableAlias = tableAlias;
		}

		private SQLFilters filter;

		public String getTableName() {
			return tableName;
		}

		public void setTableName(String tableName) {
			this.tableName = tableName;
		}

		public SQLFilters getFilter() {
			return filter;
		}

		public void setFilter(SQLFilters filter) {
			this.filter = filter;
		}
	}
}
