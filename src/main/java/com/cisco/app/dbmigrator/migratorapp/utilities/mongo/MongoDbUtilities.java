package com.cisco.app.dbmigrator.migratorapp.utilities.mongo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.core.event.SyncError;
import com.cisco.app.dbmigrator.migratorapp.logging.connection.MongoConnection;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncEventDao;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncConnectionInfo;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.MatchOperator;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.client.model.Filters;

public final class MongoDbUtilities {
	private MongoDbUtilities(){}
	public List<String> getAllCollections(){
		List<String> collectionList = new ArrayList<String>();
		return MongoConnection.INSTANCE.getMongoDataBase().listCollectionNames().into(collectionList);		
	}
	
	public static Bson getFilterBson(MatchOperator operator , String attributeName , Object value){
		Bson filter = null;
		switch(operator){
		case GT : 
			filter=Filters.gt(attributeName, value);
			break;
		case LT : 
			filter=Filters.lt(attributeName, value);
			break;
		case GTE : 
			filter=Filters.gte(attributeName, value);
			break;
		case LTE : 
			filter=Filters.lte(attributeName, value);
			break;
		case EQ :
			filter=Filters.eq(attributeName, value);
			break;
		case NE : 
			filter=Filters.ne(attributeName, value);
			break;
		default :
			filter=Filters.eq(attributeName, value);
			break;
		}
		return filter;
	}
	
	public static void logErrors(Exception e , ObjectId eventId){
		SyncError error = new SyncError(e);
		error.setThreadName(Thread.currentThread().getName());
		new SyncEventDao().pushError(eventId, error);
	}
	public static boolean getMongoClient(SyncConnectionInfo connectionInfo) {
		boolean flag = false;
		List<ServerAddress> addressList = null;
		List<MongoCredential> credList = null;
		MongoClient client = null;
		MongoClientOptions.Builder optionsBuilder = new MongoClientOptions.Builder();
        optionsBuilder.readPreference(ReadPreference.secondary());
		if (connectionInfo.getPassword() != null && !connectionInfo.getPassword().isEmpty()) {
			MongoCredential credential = MongoCredential.createCredential(connectionInfo.getUserName(), connectionInfo.getDbName(),
					connectionInfo.getPassword().toCharArray());
			credList = new ArrayList<MongoCredential>();
			credList.add(credential);
		}
		if (connectionInfo.getHostToPortMap() != null && !connectionInfo.getHostToPortMap().isEmpty()) {
			addressList = new ArrayList<ServerAddress>();
			ServerAddress address = null;
			for (Map.Entry<String, Double> hostPort : connectionInfo.getHostToPortMap().entrySet()) {
				address = new ServerAddress(hostPort.getKey(), hostPort.getValue().intValue());
				addressList.add(address);
			}
		}
		if(credList!=null){
			client = new MongoClient(addressList, credList , optionsBuilder.build());
		}else{
			client = new MongoClient(addressList , optionsBuilder.build());
		}
		if(client != null){
			flag = true;
			client.close();
		}
		return flag;
	}
}
