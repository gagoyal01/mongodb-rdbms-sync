package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities;

import java.sql.Clob;

public class ClobLiteral implements Literal<Clob> {

	private Clob value;
	
	public ClobLiteral() {}

	@Override
	public Clob getSqlExpressionForMatchable() {
		return value;
	}

	@Override
	public Literal<Clob> setLiteralValue(Clob value) {
		this.value=value;
		return this;
	}

	@Override
	public String getLiteralType() {
		return SqlColumnType.CLOB;
	}

	@Override
	public Clob getLiteralValue() {
		return value;
	}	
}
