package com.cisco.app.dbmigrator.migratorapp.core.event;

import org.bson.BsonDocument;
import org.bson.BsonDocumentWrapper;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

/**
 * Bean to hold process Errors
 * 
 * @author pnilayam
 *
 */
public class SyncError extends Exception implements Bson{
	private static final long serialVersionUID = 7690440940108580397L;
	private String threadName;
	private String fullStackTrace;
	public SyncError() {
		super();
	}
	public SyncError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
	public SyncError(String message, Throwable cause) {
		super(message, cause);
	}
	public SyncError(String message) {
		super(message);
	}
	public SyncError(Throwable cause) {
		super(cause);
	}
	public String getFullStackTrace() {
		return fullStackTrace;
	}
	public void setFullStackTrace(String fullStackTrace) {
		this.fullStackTrace = fullStackTrace;
	}
	public String getThreadName() {
		return threadName;
	}
	public void setThreadName(String threadName) {
		this.threadName = threadName;
	}
	@Override
	public <TDocument> BsonDocument toBsonDocument(Class<TDocument> arg0, CodecRegistry codecRegistry) {
		return new BsonDocumentWrapper<SyncError>(this, codecRegistry.get(SyncError.class));
	}
}
