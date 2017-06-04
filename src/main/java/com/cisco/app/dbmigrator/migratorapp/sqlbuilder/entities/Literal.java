package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities;

public interface Literal<T> extends MatchAble<T> {
	public Literal<T> setLiteralValue(T value);
	public String getLiteralType();
	public T getLiteralValue();
}
