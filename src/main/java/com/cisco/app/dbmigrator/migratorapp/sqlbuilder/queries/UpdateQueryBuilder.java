package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.queries;

import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleTable;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.QueryConstants;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents.SQLFilters;

public class UpdateQueryBuilder {
	private static final String UPDATE = "UPDATE ";
	private static final String SET = "SET";
	private final UpdateQueryComponents queryComponents;
	
	public UpdateQueryBuilder(){
		super();
		queryComponents = new UpdateQueryComponents();
	}
	
	public AddTable update(){
		return new AddTable();
	}
	
	public final class AddTable{
		public AddFilter table(OracleTable table){
			addTable(table);
			return new AddFilter();
		}
		private AddTable(){}
	}
	
	public final class AddFilter{
		private AddFilter(){}
		public GetQuery addFilter(SQLFilters filter){
			where(filter);
			return new GetQuery();
		}
		public String getQuery(){
			return buildQuery();
		}
	}
	
	public final class GetQuery{
		private GetQuery(){}
		public String getQuery(){
			return buildQuery();
		}
	}

	private void addTable(OracleTable table){
		queryComponents.setTable(table);
	}
	private UpdateQueryBuilder where(SQLFilters filters){
		queryComponents.setFilters(filters);
		return this;
	}
	private String buildQuery() {
		StringBuilder builder = new StringBuilder(UPDATE);
		OracleTable table = queryComponents.getTable();
		builder.append(table.getTableName()).append(QueryConstants.NEXT_LINE).append(QueryConstants.INDENT).append(SET);
		for (OracleColumn column : table.getColumns()) {
			builder.append(column.getColumnName()).append(QueryConstants.SPACE).append(QueryConstants.EQUALS)
					.append(QueryConstants.SPACE).append(QueryConstants.COLON).append(column.getColumnAlias())
					.append(QueryConstants.COMMA).append(QueryConstants.NEXT_LINE);
		}
		builder.deleteCharAt(builder.lastIndexOf(QueryConstants.COMMA));
		if (queryComponents.getFilters() != null) {
			builder.append(QueryConstants.WHERE).append(QueryConstants.SPACE);
			builder.append(queryComponents.getFilters().getFilterExpression());
			builder.append(QueryConstants.NEXT_LINE);
		}
		return builder.toString().trim();
	}
}
