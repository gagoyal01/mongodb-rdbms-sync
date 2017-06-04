package com.cisco.app.dbmigrator.migratorapp.logging.dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.logging.connection.ApplicationCollections;
import com.cisco.app.dbmigrator.migratorapp.logging.connection.MongoConnection;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncConnectionInfo;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncConnectionInfo.ConnectionInfoAttributes;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;

public class SyncConnectionDao {
	MongoCollection<SyncConnectionInfo> connectionInfo = MongoConnection.INSTANCE.getMongoDataBase()
			.getCollection(String.valueOf(ApplicationCollections.SyncConnectionInfo), SyncConnectionInfo.class);

	public SyncConnectionInfo getConnection(String dbName, String dbUserName) {
		return connectionInfo.find(
				Filters.and(Filters.eq(String.valueOf(ConnectionInfoAttributes.dbName), dbName),
						Filters.eq(String.valueOf(ConnectionInfoAttributes.userName), dbUserName)),
				SyncConnectionInfo.class).first();
	}

	public SyncConnectionInfo updateConnection(SyncConnectionInfo connInfo) {
		if(connInfo.getConnectionId() == null){
			connInfo.setConnectionId(new ObjectId());
		}
		return connectionInfo.findOneAndReplace(
				Filters.eq(String.valueOf(ConnectionInfoAttributes._id), connInfo.getConnectionId()), connInfo,
				new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(true));
	}

	public List<SyncConnectionInfo> getConnectionDetails() {
		return connectionInfo.find().projection(Projections.include(String.valueOf(ConnectionInfoAttributes.dbName),String.valueOf(ConnectionInfoAttributes.userName),String.valueOf(ConnectionInfoAttributes.dbType)))
				.into(new ArrayList<SyncConnectionInfo>());
	}
	
	public void getConnetionById(ObjectId id){
		connectionInfo.find(Filters.eq(String.valueOf(ConnectionInfoAttributes._id), id)).first();
	}
	
	public void deleteConnetion(ObjectId id){
		connectionInfo.deleteOne(Filters.eq(String.valueOf(ConnectionInfoAttributes._id), id));
	}
	
	public List<SyncConnectionInfo> getPassword() {
		return connectionInfo.find().projection(Projections.include(String.valueOf(ConnectionInfoAttributes._id),String.valueOf(ConnectionInfoAttributes.password)))
				.into(new ArrayList<SyncConnectionInfo>());
	}
	
	public void setEncryptedPassword(ObjectId id , byte[] pass) {
		 connectionInfo.findOneAndUpdate(
				Filters.and(Filters.eq(String.valueOf(ConnectionInfoAttributes._id), id)),
				Updates.set(String.valueOf(ConnectionInfoAttributes.password), pass),
				new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
	}
	
	public SyncConnectionInfo searchConnection(String dbName,String userName,String dbType){
		return connectionInfo.find(Filters.and(Filters.eq(String.valueOf(ConnectionInfoAttributes.dbName), dbName),
				Filters.eq(String.valueOf(ConnectionInfoAttributes.userName), userName),Filters.eq(String.valueOf(ConnectionInfoAttributes.dbType), dbType)),SyncConnectionInfo.class
				).first();
	}
}
