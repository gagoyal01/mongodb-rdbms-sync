package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities;

import oracle.sql.ROWID;

public class RowIdLiteral implements Literal<ROWID> {
	ROWID value;
	@Override
	public ROWID getSqlExpressionForMatchable() {
		return value;
	}

	@Override
	public Literal<ROWID> setLiteralValue(ROWID value) {
		this.value=value;
		return this;
	}
	
	public Literal<ROWID> setLiteralValue(String value) {		
		return setLiteralValue(value.getBytes());
	}
	
	public Literal<ROWID> setLiteralValue(byte [] value) {
		ROWID rowId = new ROWID(value);
		this.value=rowId;
		return this;
	}

	@Override
	public String getLiteralType() {
		return SqlColumnType.ROWID;
	}

	@Override
	public ROWID getLiteralValue() {
		return value;
	}

}
