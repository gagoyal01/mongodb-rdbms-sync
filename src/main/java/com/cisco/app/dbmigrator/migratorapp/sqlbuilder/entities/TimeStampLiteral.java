package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities;

import java.sql.Timestamp;
import java.util.Date;

public class TimeStampLiteral implements Literal<Date> {
	private Timestamp timestamp;
	@Override
	public Timestamp getSqlExpressionForMatchable() {
		return timestamp;
	}

	@Override
	public Literal<Date> setLiteralValue(Date value) {
		timestamp = new Timestamp(value.getTime());
		return this;
	}

	@Override
	public String getLiteralType() {
		return "TIMESTAMP";
	}

	@Override
	public Timestamp getLiteralValue() {
		return timestamp;
	}

}
