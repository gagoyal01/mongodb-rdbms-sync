package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities;

/**
 * @author pnilayam
 *
 */
public class SortByColumn {
	private String columnAlias;
	private SortOrder sortOrder;

	private SortByColumn() {
	}

	/**
	 * @param tableAlias
	 * @param columnName
	 * @param sortOrder
	 * @return
	 */
	public static SortByColumn column(String tableAlias, String columnName, SortOrder sortOrder) {
		SortByColumn column = new SortByColumn();
		column.columnAlias = tableAlias + QueryConstants.DOT + columnName;
		column.sortOrder = sortOrder;
		return column;
	}

	/**
	 * @param tableAlias
	 * @param columnName
	 * @return
	 */
	public static SortByColumn column(String tableAlias, String columnName) {
		SortByColumn column = new SortByColumn();
		column.columnAlias = tableAlias + QueryConstants.DOT + columnName;
		column.sortOrder = SortOrder.ASCENDING;
		return column;
	}

	/**
	 * @param columnAlias
	 * @param sortOrder
	 * @return
	 */
	public static SortByColumn column(String columnAlias, SortOrder sortOrder) {
		SortByColumn column = new SortByColumn();
		column.columnAlias = columnAlias;
		column.sortOrder = sortOrder;
		return column;
	}

	/**
	 * @param columnAlias
	 * @return
	 */
	public static SortByColumn column(String columnAlias) {
		SortByColumn column = new SortByColumn();
		column.columnAlias = columnAlias;
		column.sortOrder = SortOrder.ASCENDING;
		return column;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((columnAlias == null) ? 0 : columnAlias.hashCode());
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
		SortByColumn other = (SortByColumn) obj;
		if (columnAlias == null) {
			if (other.columnAlias != null)
				return false;
		} else if (!columnAlias.equals(other.columnAlias))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(columnAlias).append(QueryConstants.SPACE).append(sortOrder.sortOrder())
				.append(QueryConstants.COMMA);
		return builder.toString();
	}
}
