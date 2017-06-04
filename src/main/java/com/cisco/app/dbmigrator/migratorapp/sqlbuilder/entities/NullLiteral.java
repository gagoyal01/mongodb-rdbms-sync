package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities;

@SuppressWarnings("rawtypes")
public class NullLiteral implements Literal{

	@Override
	public Object getSqlExpressionForMatchable() {
		return null;
	}

	@Override
	public Literal setLiteralValue(Object value) {
		return this;
	}

	@Override
	public String getLiteralType() {
		return "NULL";
	}

	@Override
	public Object getLiteralValue() {
		return null;
	}

}
