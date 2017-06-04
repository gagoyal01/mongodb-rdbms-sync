package com.cisco.app.dbmigrator.migratorapp.logging.dao;

import java.util.List;

import com.cisco.app.dbmigrator.migratorapp.logging.connection.ApplicationCollections;
import com.cisco.app.dbmigrator.migratorapp.logging.connection.MongoConnection;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.O2MSyncEventLog;
import com.mongodb.client.MongoCollection;

public class O2MEventLogDao {
	MongoCollection<O2MSyncEventLog> eventLog = MongoConnection.INSTANCE.getMongoDataBase()
			.getCollection(String.valueOf(ApplicationCollections.O2MSyncEventLog), O2MSyncEventLog.class);
	
	public void writeData(List<O2MSyncEventLog> eventLogList){
		if(eventLogList==null || eventLogList.isEmpty()){
			return;
		}
		eventLog.insertMany(eventLogList);
	}
	
	public void logEvent(O2MSyncEventLog eventInfo){
		eventLog.insertOne(eventInfo);
	}
}
