package com.cisco.app.dbmigrator.migratorapp.logging.entities;

import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;
import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.core.event.SyncError;

public class O2MSyncEventLog {
	private ObjectId logId;
	private String eventId;
	private String operation;
	private Date crOn;
	private Date syOn;
	private SyncError error;
	private String status;
	private List<O2MSyncEventInfo> eventFilters;
	
	/**
	 * @return the logId
	 */
	public ObjectId getLogId() {
		return logId;
	}

	/**
	 * @param logId the logId to set
	 */
	public void setLogId(ObjectId logId) {
		this.logId = logId;
	}

	/**
	 * @return the eventId
	 */
	public String getEventId() {
		return eventId;
	}

	/**
	 * @param eventId the eventId to set
	 */
	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	/**
	 * @return the operation
	 */
	public String getOperation() {
		return operation;
	}

	/**
	 * @param operation the operation to set
	 */
	public void setOperation(String operation) {
		this.operation = operation;
	}

	/**
	 * @return the crOn
	 */
	public Date getCrOn() {
		return crOn;
	}

	/**
	 * @param crOn the crOn to set
	 */
	public void setCrOn(Date crOn) {
		this.crOn = crOn;
	}

	/**
	 * @return the syOn
	 */
	public Date getSyOn() {
		return syOn;
	}

	/**
	 * @param syOn the syOn to set
	 */
	public void setSyOn(Date syOn) {
		this.syOn = syOn;
	}

	/**
	 * @return the error
	 */
	public SyncError getError() {
		return error;
	}

	/**
	 * @param error the error to set
	 */
	public void setError(SyncError error) {
		this.error = error;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @return the eventFilters
	 */
	public List<O2MSyncEventInfo> getEventFilters() {
		return eventFilters;
	}

	/**
	 * @param eventFilters the eventFilters to set
	 */
	public void setEventFilters(List<O2MSyncEventInfo> eventFilters) {
		this.eventFilters = eventFilters;
	}

	public static final class O2MSyncEventInfo implements SQLData{
		private String tableName;
		private String columnName;
		private Object columnValue;
		private String typeName;
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "O2MSyncEventInfo [tableName=" + tableName + ", columnName=" + columnName + ", columnValue="
					+ columnValue + ", typeName=" + typeName + "]";
		}
		/**
		 * @return the tableName
		 */
		public String getTableName() {
			return tableName;
		}
		/**
		 * @param tableName the tableName to set
		 */
		public void setTableName(String tableName) {
			this.tableName = tableName;
		}
		/**
		 * @return the columnName
		 */
		public String getColumnName() {
			return columnName;
		}
		/**
		 * @param columnName the columnName to set
		 */
		public void setColumnName(String columnName) {
			this.columnName = columnName;
		}
		/**
		 * @return the columnValue
		 */
		public Object getColumnValue() {
			return columnValue;
		}
		/**
		 * @param columnValue the columnValue to set
		 */
		public void setColumnValue(Object columnValue) {
			this.columnValue = columnValue;
		}
		@Override
		public String getSQLTypeName() throws SQLException {
			return typeName;
		}
		@Override
		public void readSQL(SQLInput stream, String typeName) throws SQLException {
			this.typeName= typeName;
			this.tableName=stream.readString();
			this.columnName= stream.readString();
			this.columnValue=stream.readObject();
		}
		@Override
		public void writeSQL(SQLOutput stream) throws SQLException {
			stream.writeString(this.tableName);
			stream.writeString(this.columnName);
			stream.writeString(String.valueOf(this.columnValue));
		}
		
	}
}
