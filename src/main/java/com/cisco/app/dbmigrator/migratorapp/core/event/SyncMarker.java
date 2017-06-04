package com.cisco.app.dbmigrator.migratorapp.core.event;

import java.util.Date;

import org.bson.BsonDocument;
import org.bson.BsonDocumentWrapper;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

/**
 * Bean to hold Sync Event status and details
 * 
 * @author pnilayam
 *
 */
public class SyncMarker implements Bson {
	private volatile boolean allRowsFetchedFromDb;
	private int rowsRead;
	private int rowsDumped;
	private int totalRows;
	private Date startTime;
	private Date endTime;
	private volatile boolean failed;

	public boolean isFailed() {
		return failed;
	}

	public void setFailed(boolean failed) {
		this.failed = failed;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public SyncMarker() {
		failed=false;
		totalRows = -1;
	}

	public synchronized boolean isAllRowsFetchedFromDb() {
		return allRowsFetchedFromDb;
	}

	public synchronized void setAllRowsFetchedFromDb(boolean allRowsFetchedFromDb) {
		this.allRowsFetchedFromDb = allRowsFetchedFromDb;
	}

	public synchronized int getRowsRead() {
		return rowsRead;
	}

	public synchronized void setRowsRead(int rowsRead) {
		this.rowsRead = rowsRead;
	}

	public synchronized int getRowsDumped() {
		return rowsDumped;
	}

	public synchronized void setRowsDumped(int rowsDumped) {
		this.rowsDumped = rowsDumped;
	}

	public int getTotalRows() {
		return totalRows;
	}

	public void setTotalRows(int totalRows) {
		this.totalRows = totalRows;
	}

	public synchronized void addToRowsDumped(int rowsDumped) {
		this.rowsDumped += rowsDumped;
	}

	@Override
	public <TDocument> BsonDocument toBsonDocument(Class<TDocument> arg0, CodecRegistry codecRegistry) {
		return new BsonDocumentWrapper<SyncMarker>(this, codecRegistry.get(SyncMarker.class));
	}
}