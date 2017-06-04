/**
 * 
 */
package com.cisco.app.dbmigrator.migratorapp.core.job;

/**
 * @author pnilayam
 *
 */
public enum SyncStatus {
	STATUS;
	public static final String PENDING = "PENDING";
	public static final String FAILED = "FAILED";
	public static final String APPROVED = "APPROVED";
	public static final String IN_PROGRESS = "INPROGRESS";
	public static final String COMPLETE = "COMPLETE";
	public static final String PREPARING = "PREPARING";
	public static final String CANCELLED ="CANCELLED";
}
