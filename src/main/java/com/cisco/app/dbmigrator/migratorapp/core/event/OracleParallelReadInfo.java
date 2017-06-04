package com.cisco.app.dbmigrator.migratorapp.core.event;

import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;

/**
 * Bean to hold Parallel Read info while reading data from OracleDB.
 * 
 * @author pnilayam
 *
 */
public class OracleParallelReadInfo {
	private boolean processParallel;
	private OracleColumn rangeColumn;
	private int numOfBuckets;
	public boolean isProcessParallel() {
		return processParallel;
	}
	public void setProcessParallel(boolean processParallel) {
		this.processParallel = processParallel;
	}
	public OracleColumn getRangeColumn() {
		return rangeColumn;
	}
	public void setRangeColumn(OracleColumn rangeColumn) {
		this.rangeColumn = rangeColumn;
	}
	public int getNumOfBuckets() {
		return numOfBuckets;
	}
	public void setNumOfBuckets(int numOfBuckets) {
		this.numOfBuckets = numOfBuckets;
	}
}
