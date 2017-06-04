package com.cisco.app.dbmigrator.migratorapp.service;

import java.util.List;

import com.cisco.app.dbmigrator.migratorapp.core.event.SyncError;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncEvent;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncMarker;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.O2MSyncEventLog;

/**
 * Service Interface with all behaviors for SyncEvent
 * 
 * @author pnilayam
 *
 */
@SuppressWarnings("rawtypes")
public interface SyncEventService {
	/**
	 * Method to delete Event from System based on given eventId
	 * 
	 * @param eventId
	 */
	public void deleteEvent(String eventId);

	/**
	 * Method to fetch list of Events for input user Id
	 * 
	 * @param userId
	 * @return List of SyncEvent, null in case of no event found
	 */
	public List<SyncEvent> getEventsForUser(String userId);

	/**
	 * Method to get Current status of SyncEvent
	 * @param eventId
	 * @return SyncMarker
	 */
	public SyncMarker getEventStatus(String eventId);

	/**
	 * Method to get all errors for input eventId
	 * @param eventId
	 * @return
	 */
	public List<SyncError> getEventErrors(String eventId);

	/**
	 * Method to Save SyncEvent into System from input eventJson
	 * @param mappingJsonStr
	 * @return
	 */
	public SyncEvent saveEvent(String eventJson);
	
	public SyncEvent retryEvent(String eventId,boolean retryFailed,boolean retryEntire,boolean dropCollection);
	
	public SyncEvent cancelEvents(String eventId);
	
	public void logEvent(O2MSyncEventLog eventInfo);
}
