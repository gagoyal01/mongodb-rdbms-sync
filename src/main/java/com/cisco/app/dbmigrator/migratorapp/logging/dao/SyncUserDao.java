package com.cisco.app.dbmigrator.migratorapp.logging.dao;

import com.cisco.app.dbmigrator.migratorapp.logging.connection.ApplicationCollections;
import com.cisco.app.dbmigrator.migratorapp.logging.connection.MongoConnection;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncUser;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncUser.UserDetailAttributes;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.ReturnDocument;

public class SyncUserDao {
	MongoCollection<SyncUser> userDetailsCollection = MongoConnection.INSTANCE
			.getMongoDataBase().getCollection(String.valueOf(ApplicationCollections.SyncUserDetail),
					SyncUser.class);

	public SyncUser getUser(String userId) {
		return userDetailsCollection.find(
				Filters.eq(String.valueOf(UserDetailAttributes._id), userId),
				SyncUser.class).first();
	}

	public SyncUser updateUser(SyncUser user) {
		return userDetailsCollection.findOneAndReplace(
				Filters.eq(String.valueOf(UserDetailAttributes._id),user.getUserid()),user,
						new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER)
													  .upsert(true));
	}
	
	public void deleteUser(String userId){
		userDetailsCollection.deleteOne(Filters.eq(String.valueOf(UserDetailAttributes._id),userId));
	}

}
