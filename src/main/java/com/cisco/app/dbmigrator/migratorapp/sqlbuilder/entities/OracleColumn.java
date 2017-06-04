package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class OracleColumn implements OracleEntity, MatchAble<Object> {
	private static final int MAX_IDENTIFIER_LENGTH = 27;
	private String columnName;
	private String columnAlias;
	private String columnType;
	private int precision;
	private boolean isNullable;
	private String tableAlias;
	private boolean isParentColumn;
	private String defaultValue; // Use literalfactory to get proper value
	private List<String> ignoreList;
	private Literal<Object> literalValueForColumn;

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public List<String> getIgnoreList() {
		return ignoreList;
	}

	public void setIgnoreList(List<String> ignoreList) {
		this.ignoreList = ignoreList;
	}

	public boolean isParentColumn() {
		return isParentColumn;
	}

	public void setParentColumn(boolean isParentColumn) {
		this.isParentColumn = isParentColumn;
	}

	public String getTableAlias() {
		return tableAlias;
	}

	public void setTableAlias(String tableAlias) {
		this.tableAlias = tableAlias;
	}

	public String getColumnAlias() {
		if (columnAlias == null) {
			columnAlias = tableAlias + "_" + columnName;
		}
		if (columnAlias.length() > MAX_IDENTIFIER_LENGTH) {
			columnAlias = columnAlias.substring(0, MAX_IDENTIFIER_LENGTH);
		}
		return columnAlias;
	}

	public void setColumnAlias(String columnAlias) {
		this.columnAlias = columnAlias;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public String getColumnType() {
		return columnType;
	}

	public void setColumnType(String columnType) {
		this.columnType = columnType;
	}

	public int getPrecision() {
		return precision;
	}

	public void setPrecision(int precision) {
		this.precision = precision;
	}

	public boolean isNullable() {
		return isNullable;
	}

	public void setNullable(boolean isNullable) {
		this.isNullable = isNullable;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((columnAlias == null) ? 0 : columnAlias.hashCode());
		result = prime * result + ((columnName == null) ? 0 : columnName.hashCode());
		result = prime * result + ((tableAlias == null) ? 0 : tableAlias.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OracleColumn other = (OracleColumn) obj;
		if (columnAlias == null) {
			if (other.columnAlias != null)
				return false;
		} else if (!columnAlias.equals(other.columnAlias))
			return false;
		if (columnName == null) {
			if (other.columnName != null)
				return false;
		} else if (!columnName.equals(other.columnName))
			return false;
		if (tableAlias == null) {
			if (other.tableAlias != null)
				return false;
		} else if (!tableAlias.equals(other.tableAlias))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "OracleColumn [columnName=" + columnName + ", columnAlias=" + columnAlias + ", columnType=" + columnType
				+ ", precision=" + precision + ", isNullable=" + isNullable + ", tableAlias=" + tableAlias + "]";
	}

	@Override
	public Object getSqlExpressionForMatchable() {
		if (isParentColumn) {
			return literalValueForColumn.getLiteralValue();
		}
		return new StringBuilder().append(tableAlias).append(QueryConstants.DOT).append(columnName).toString();
	}

	@SuppressWarnings("unchecked")
	public void extractColumnValueFromParentRow(ResultSet parentRow) throws SQLException {
		Object columnValue = parentRow.getObject(columnAlias);
		literalValueForColumn = SqlLiteralFactory.getLiteral(columnValue, columnType);
	}

	@SuppressWarnings("rawtypes")
	public Literal getLiteralValueForColumn() {
		return literalValueForColumn;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setLiteralValueForColumn(Literal literalValueForColumn) {
		this.literalValueForColumn = literalValueForColumn;
	}
}