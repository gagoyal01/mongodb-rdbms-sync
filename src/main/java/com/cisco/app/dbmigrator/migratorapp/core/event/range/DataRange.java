package com.cisco.app.dbmigrator.migratorapp.core.event.range;

public class DataRange {
	private String rangeAttribute;
	private Object upperLimit;
	private Object lowerLimit;
	private boolean isUpperLimitInclusive;
	private boolean isLowerLimitInclusive;
	public String getRangeAttribute() {
		return rangeAttribute;
	}
	public void setRangeAttribute(String rangeAttribute) {
		this.rangeAttribute = rangeAttribute;
	}
	public Object getUpperLimit() {
		return upperLimit;
	}
	public void setUpperLimit(Object upperLimit) {
		this.upperLimit = upperLimit;
	}
	public Object getLowerLimit() {
		return lowerLimit;
	}
	public void setLowerLimit(Object lowerLimit) {
		this.lowerLimit = lowerLimit;
	}
	public boolean isUpperLimitInclusive() {
		return isUpperLimitInclusive;
	}
	public void setUpperLimitInclusive(boolean isUpperLimitInclusive) {
		this.isUpperLimitInclusive = isUpperLimitInclusive;
	}
	public boolean isLowerLimitInclusive() {
		return isLowerLimitInclusive;
	}
	public void setLowerLimitInclusive(boolean isLowerLimitInclusive) {
		this.isLowerLimitInclusive = isLowerLimitInclusive;
	}
}
