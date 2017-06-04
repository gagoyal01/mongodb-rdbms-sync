package com.cisco.app.dbmigrator.migratorapp.core.meta.mongo;

import java.io.InputStream;

import org.bson.Document;

public class MongoGridData {
	private Document gridMetaData;
	private String fileName;
	private InputStream is;
	private byte [] binData;
	public byte[] getBinData() {
		return binData;
	}
	public void setBinData(byte[] binData) {
		this.binData = binData;
	}
	public Document getGridMetaData() {
		return gridMetaData;
	}
	public void setGridMetaData(Document gridMetaData) {
		this.gridMetaData = gridMetaData;
	}
	public InputStream getIs() {
		return is;
	}
	public void setIs(InputStream is) {
		this.is = is;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	@Override
	public String toString() {
		return "MongoGridData [gridMetaData=" + String.valueOf(gridMetaData) + ", fileName=" + fileName + "]";
	}
}
