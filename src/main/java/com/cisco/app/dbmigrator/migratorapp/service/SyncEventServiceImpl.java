package com.cisco.app.dbmigrator.migratorapp.service;

import java.util.List;

import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.core.event.SyncError;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncEvent;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncMarker;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncMapAndEventDecoder;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.O2MEventLogDao;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncEventDao;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.O2MSyncEventLog;

@SuppressWarnings("rawtypes")
public class SyncEventServiceImpl implements SyncEventService {
	private SyncEventDao eventDao;
	private SyncMapAndEventDecoder decoder;
	private O2MEventLogDao eventLogDao;
	public SyncEventDao getEventDao() {
		return eventDao;
	}

	public void setEventDao(SyncEventDao eventDao) {
		this.eventDao = eventDao;
	}

	public SyncMapAndEventDecoder getDecoder() {
		return decoder;
	}

	public void setDecoder(SyncMapAndEventDecoder decoder) {
		this.decoder = decoder;
	}

	@Override
	public void deleteEvent(String eventId) {
		eventDao.deleteEvent(new ObjectId(eventId));
	}

	@Override
	public SyncMarker getEventStatus(String eventId) {
		if(eventId!=null && !eventId.isEmpty()){
			return eventDao.getEventStats(new ObjectId(eventId));
		}
		return null;
	}

	@Override
	public List<SyncError> getEventErrors(String eventId) {
		if(eventId!=null && !eventId.isEmpty()){
			return eventDao.getEventErrors(new ObjectId(eventId));
		}
		return null;
	}

	@Override
	public SyncEvent saveEvent(String mappingJsonStr) {
		SyncEvent event = decoder.decodeSyncEvent(mappingJsonStr);
		return eventDao.saveEvent(event);
	}

	@Override
	public List<SyncEvent> getEventsForUser(String userId) {
		return eventDao.getAllEventsForUser(userId);
	}
	
	@Override
	public SyncEvent retryEvent(String eventId,boolean retryFailed,boolean retryEntire,boolean dropCollection) {
		return eventDao.retryEvent(new ObjectId(eventId),retryFailed,retryEntire,dropCollection);
	}

	@Override
	public SyncEvent cancelEvents(String eventId) {
		return eventDao.cancelEvent(new ObjectId(eventId));
	}

	@Override
	public void logEvent(O2MSyncEventLog eventInfo) {
		eventLogDao.logEvent(eventInfo);
	}

}
