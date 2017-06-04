package com.cisco.app.dbmigrator.migratorapp.core.event.range;

import java.util.List;

public abstract class RangeDivisor<T>{
	protected int numOfBuckets;
	protected T minValue;
	protected T maxValue;
	public int getNumOfBuckets() {
		return numOfBuckets;
	}
	public RangeDivisor(int numOfBuckets, T minValue, T maxValue) {
		super();
		this.numOfBuckets = numOfBuckets;
		this.minValue = minValue;
		this.maxValue = maxValue;
	}
	public void setNumOfBuckets(int numOfBuckets) {
		this.numOfBuckets = numOfBuckets;
	}
	public T getMinValue() {
		return minValue;
	}
	public void setMinValue(T minValue) {
		this.minValue = minValue;
	}
	public T getMaxValue() {
		return maxValue;
	}
	public void setMaxValue(T maxValue) {
		this.maxValue = maxValue;
	}
	public abstract List<DataRange> generateRangeList();
}
