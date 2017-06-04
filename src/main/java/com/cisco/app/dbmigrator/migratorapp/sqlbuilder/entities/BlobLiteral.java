package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities;

import java.sql.Blob;

public class BlobLiteral implements Literal<Blob> {

	private Blob value;
	
	public BlobLiteral() {}

	@Override
	public String getLiteralType() {
		return SqlColumnType.BLOB;
	}

	@Override
	public Blob getSqlExpressionForMatchable() {
		return value;
	}

	@Override
	public Literal<Blob> setLiteralValue(Blob value) {
		this.value=value;
		return this;
	}

	@Override
	public Blob getLiteralValue() {
		return value;
	}
}
