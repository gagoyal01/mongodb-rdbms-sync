/**
 * 
 */
package com.cisco.app.dbmigrator.migratorapp.logging.entities;

/**
 * @author pnilayam
 *
 */
public class UserActivity {
	private String userId;
	private String sourceDbName;
	private String sourceSchemaName;
	private String targetDbName;
	private String targetSchemaName;
	
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getSourceDbName() {
		return sourceDbName;
	}
	public void setSourceDbName(String sourceDbName) {
		this.sourceDbName = sourceDbName;
	}
	public String getSourceSchemaName() {
		return sourceSchemaName;
	}
	public void setSourceSchemaName(String sourceSchemaName) {
		this.sourceSchemaName = sourceSchemaName;
	}
	public String getTargetDbName() {
		return targetDbName;
	}
	public void setTargetDbName(String targetDbName) {
		this.targetDbName = targetDbName;
	}
	public String getTargetSchemaName() {
		return targetSchemaName;
	}
	public void setTargetSchemaName(String targetSchemaName) {
		this.targetSchemaName = targetSchemaName;
	}
}
