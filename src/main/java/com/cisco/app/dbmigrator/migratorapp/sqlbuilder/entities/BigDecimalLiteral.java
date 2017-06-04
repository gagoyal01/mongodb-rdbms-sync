package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities;

import java.math.BigDecimal;

public class BigDecimalLiteral implements Literal<BigDecimal> {
	BigDecimal value;
	@Override
	public BigDecimal getSqlExpressionForMatchable() {
		return value;
	}

	@Override
	public Literal<BigDecimal> setLiteralValue(BigDecimal value) {
		this.value=value;
		return this;
	}

	@Override
	public String getLiteralType() {
		return SqlColumnType.NUMBER;
	}

	@Override
	public BigDecimal getLiteralValue() {
		return value;
	}

}
