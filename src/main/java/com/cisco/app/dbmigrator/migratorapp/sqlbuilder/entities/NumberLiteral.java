package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities;

public class NumberLiteral implements Literal<Double> {
	private double value;
	
	public NumberLiteral(){}
	
	public static NumberLiteral setNumber(int value){
		NumberLiteral number = new NumberLiteral();
		number.value=value;
		return number;
	}
	public static NumberLiteral setNumber(double value){
		NumberLiteral number = new NumberLiteral();
		number.value=value;
		return number;
	}

	@Override
	public Literal<Double> setLiteralValue(Double value) {
		this.value = value;			
		return this;
	}

	@Override
	public String getLiteralType() {
		return "NUMBER";
	}

	@Override
	public Double getLiteralValue() {
		return value;
	}

	@Override
	public Double getSqlExpressionForMatchable() {
		return value;
	}

}
