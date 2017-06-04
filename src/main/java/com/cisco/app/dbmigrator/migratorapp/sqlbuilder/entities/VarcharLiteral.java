package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities;

public class VarcharLiteral implements Literal<String> {
	private String value;
	
	public VarcharLiteral(){}
	
	public static VarcharLiteral setVarchar(String value){
		VarcharLiteral literal = new VarcharLiteral();
		literal.value=value;
		return literal;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VarcharLiteral other = (VarcharLiteral) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "VarcharLiteral [value=" + value + "]";
	}
	//@Override
	public Literal<String> setLiteralValue(String value) {
		this.value = value;			
		return this;
	}

	@Override
	public String getLiteralType() {
		return "VARCHAR2";
	}

	@Override
	public String getLiteralValue() {
		return value;
	}

	@Override
	public String getSqlExpressionForMatchable() {
		return value;
	}	
}
