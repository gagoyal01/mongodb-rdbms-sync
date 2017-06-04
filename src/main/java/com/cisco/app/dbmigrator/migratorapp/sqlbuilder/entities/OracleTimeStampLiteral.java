package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import oracle.sql.TIMESTAMP;

public class OracleTimeStampLiteral implements Literal<TIMESTAMP> {
	TIMESTAMP ts ;
	@Override
	public TIMESTAMP getSqlExpressionForMatchable() {
		return ts;
	}

	@Override
	public Literal<TIMESTAMP> setLiteralValue(TIMESTAMP value) {
		ts=value;
		return null;
	}
	
	public Literal<TIMESTAMP> setLiteralValue(Date value) {
		ts = new TIMESTAMP(new Timestamp(value.getTime()), Calendar.getInstance(TimeZone.getTimeZone("PST")));
		return this;
	}

	@Override
	public String getLiteralType() {
		return SqlColumnType.TIMESTAMP;
	}

	@Override
	public TIMESTAMP getLiteralValue() {
		return ts;
	}
}
