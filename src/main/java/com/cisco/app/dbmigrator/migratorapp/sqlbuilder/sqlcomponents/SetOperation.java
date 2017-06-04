package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.sqlcomponents;

public abstract class SetOperation {
	protected enum SetOperators{
		UNION,UNIONALL,MINUS,INTERSECT;
	}
}
