package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities;

import java.util.Date;

public class DateLiteral implements Literal<Date>{
	private Date date;
	@Override
	public java.sql.Date getSqlExpressionForMatchable() {
		return new java.sql.Date(date.getTime());//==null?null:date.toString();
	}

	@Override
	public Literal<Date> setLiteralValue(Date value) {
		this.date=value;
		return this;
	}

	@Override
	public String getLiteralType() {
		return "DATE";
	}

	@Override
	public java.sql.Date getLiteralValue() {
		return new java.sql.Date(date.getTime());
	}
}
