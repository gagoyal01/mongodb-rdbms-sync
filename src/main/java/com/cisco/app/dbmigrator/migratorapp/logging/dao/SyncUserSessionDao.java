package com.cisco.app.dbmigrator.migratorapp.logging.dao;

import java.util.ArrayList;
import java.util.List;

import com.cisco.app.dbmigrator.migratorapp.logging.connection.ApplicationCollections;
import com.cisco.app.dbmigrator.migratorapp.logging.connection.MongoConnection;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncUserSession;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncUserSession.SessionAttributes;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;

public class SyncUserSessionDao {
	MongoCollection<SyncUserSession> userSessionCollection = MongoConnection.INSTANCE.getMongoDataBase()
			.getCollection(String.valueOf(ApplicationCollections.SyncUserSession), SyncUserSession.class);

	public SyncUserSession saveSession(SyncUserSession userSession) {
		return userSessionCollection.findOneAndReplace(
				Filters.eq(String.valueOf(SessionAttributes._id), userSession.getSessionId()), userSession,
				new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(true));
	}

	public List<SyncUserSession> getAllSessions(String userId) {
		List<SyncUserSession> sessionList = new ArrayList<SyncUserSession>();
		return userSessionCollection.find(Filters.eq(String.valueOf(SessionAttributes.userid), userId))
				.into(sessionList);
	}

	public SyncUserSession getLastSession(String userId) {
		return userSessionCollection.find(Filters.eq(String.valueOf(SessionAttributes.userid), userId))
				.sort(Sorts.descending(String.valueOf(SessionAttributes.loginTime))).limit(1).first();
	}
}
