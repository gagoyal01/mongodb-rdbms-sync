package com.cisco.app.dbmigrator.migratorapp.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.cisco.app.dbmigrator.migratorapp.constants.SyncConstants;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncError;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncEvent;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncMarker;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncMapAndEventEncoder;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.O2MSyncEventLog;
import com.cisco.app.dbmigrator.migratorapp.service.SyncEventService;

@SuppressWarnings("rawtypes")
@Controller
public class SyncEventController {
	private static final Logger logger = Logger.getLogger(SyncEventController.class);
	private static final String USER_ID = "userId";
	private static final String EVENT_ID = "eventId";
	private static final String EVENT_JSON = "eventJson";
	private static final String RETRY_FAILED = "retryFailed";
	private static final String RETRY_ENTIRE = "retryEntire";
	private static final String DROP_COLLECTION = "dropCollection";
	@Autowired
	private SyncEventService eventService;

	public SyncEventService getEventService() {
		return eventService;
	}

	public void setEventService(SyncEventService eventService) {
		this.eventService = eventService;
	}

	@RequestMapping(value = "/getAllEvents")
	public void getEventsForUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.info("Inside getEventsForUser");
		String userId = request.getParameter(USER_ID).trim().equalsIgnoreCase(SyncConstants.EMPTY_STRING)
				? SyncConstants.EMPTY_STRING : request.getParameter(USER_ID).trim();
		logger.debug("Getting All events for User : " + userId);
		List<SyncEvent> eventList = eventService.getEventsForUser(userId);
		JSONArray array = new JSONArray();
		String eventArrStr = null;

		if (eventList != null && !eventList.isEmpty()) {
			String eventStr = null;
			for (SyncEvent event : eventList) {
				eventStr = SyncMapAndEventEncoder.getEventJson(event);
				JSONObject jsonObject = new JSONObject(eventStr);
				array.put(jsonObject);
			}
		}
		eventArrStr = array.toString();
		logger.debug("Event list Json : " + eventArrStr);
		response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
		response.getWriter().println(eventArrStr);
		logger.info("getEventsForUser Completed");
	}

	@RequestMapping(value = "/saveEvents")
	public void saveEvents(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.info("Inside saveEvents");
		SyncEvent event = null;
		String syncEventStr = SyncConstants.EMPTY_STRING;
		String eventStr = request.getParameter(EVENT_JSON).trim().equalsIgnoreCase(SyncConstants.EMPTY_STRING)
				? SyncConstants.EMPTY_STRING : request.getParameter(EVENT_JSON).trim();
		logger.debug("Processing saveEvent request for eventJson :" + eventStr);
		event = eventService.saveEvent(eventStr);
		if (event != null) {
			syncEventStr = SyncMapAndEventEncoder.getEventJson(event);
		}
		logger.debug("Returning saved eventJson : " + eventStr);
		response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
		response.getWriter().println(syncEventStr);
		logger.info("saveEvents Completed");
	}

	@RequestMapping(value = "/deleteEvent")
	public void deleteEventMapping(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.info("Inside deleteEventMapping");
		String eventId = request.getParameter(EVENT_ID);
		eventService.deleteEvent(eventId);
		response.getWriter().println(SyncConstants.SUCCESS);
		logger.info("deleteEventMapping Completed");
	}

	@RequestMapping(value = "/fetchEventStatus")
	public void fetchEventStatus(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.info("Inside fetchStatusOfMapping");
		String eventId = request.getParameter(EVENT_ID).trim().equalsIgnoreCase(SyncConstants.EMPTY_STRING)
				? SyncConstants.EMPTY_STRING : request.getParameter(EVENT_ID).trim();
		SyncMarker marker = eventService.getEventStatus(eventId);
		String statusJson = SyncConstants.EMPTY_STRING;
		if (marker != null) {
			statusJson = new JSONObject(marker).toString();
			logger.info("Event Status Json" + statusJson);
		}
		response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
		response.getWriter().println(statusJson);
		logger.info("fetchStatusOfMapping Completed");
	}

	@RequestMapping(value = "/fetchEventErrors")
	public void fetchEventErrors(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.info("Inside fetchEventErrors");
		String eventId = request.getParameter(EVENT_ID).trim().equalsIgnoreCase(SyncConstants.EMPTY_STRING)
				? SyncConstants.EMPTY_STRING : request.getParameter(EVENT_ID).trim();
		List<SyncError> errorList = eventService.getEventErrors(eventId);
		String errorStr = SyncConstants.EMPTY_STRING;
		if (errorList != null && !errorList.isEmpty()) {
			errorStr = new JSONArray(errorList).toString();
			logger.debug("Event Json" + errorStr);
		} else {
			errorStr = new JSONArray().toString();
		}
		response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
		response.getWriter().println(errorStr);
		logger.info("fetchEventErrors Completed");
	}

	@RequestMapping(value = "/retryEvent")
	public void retryEvent(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.info("Inside retryEvent");
		String syncEventStr = SyncConstants.EMPTY_STRING;
		String eventId = request.getParameter(EVENT_ID).trim().equalsIgnoreCase(SyncConstants.EMPTY_STRING)
				? SyncConstants.EMPTY_STRING : request.getParameter(EVENT_ID).trim();
		boolean retryFailed = Boolean.valueOf(request.getParameter(RETRY_FAILED));
		boolean retryEntire = Boolean.valueOf(request.getParameter(RETRY_ENTIRE));
		boolean dropCollection = Boolean.valueOf(request.getParameter(DROP_COLLECTION));
		SyncEvent event = eventService.retryEvent(eventId, retryFailed, retryEntire, dropCollection);
		if (event != null) {
			syncEventStr = SyncMapAndEventEncoder.getEventJson(event);
		}
		response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
		response.getWriter().println(syncEventStr);
		logger.info("retryEvent Completed");
	}

	@RequestMapping(value = "/cancelEvent")
	public void cancelEvent(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.info("Inside cancelEvent");
		String syncEventStr = SyncConstants.EMPTY_STRING;
		String eventId = request.getParameter(EVENT_ID).trim().equalsIgnoreCase(SyncConstants.EMPTY_STRING)
				? SyncConstants.EMPTY_STRING : request.getParameter(EVENT_ID).trim();
		SyncEvent event = eventService.cancelEvents(eventId);
		if (event != null) {
			syncEventStr = SyncMapAndEventEncoder.getEventJson(event);
		}
		response.setContentType(SyncConstants.CONTENT_TYPE_JSON);
		response.getWriter().println(syncEventStr);
		logger.info("cancelEvent Completed");
	}

	@RequestMapping(value = "/log/event", method = RequestMethod.POST, headers = "Accept=application/json")
	@ResponseBody
	public ResponseEntity<Void> logEvent(@RequestBody O2MSyncEventLog eventInfo) {
		try {
			if (eventInfo.getEventId() == null || eventInfo.getEventId().isEmpty()
					|| eventInfo.getEventFilters() == null || eventInfo.getEventFilters().isEmpty()) {
				return new ResponseEntity<Void>(HttpStatus.PARTIAL_CONTENT);
			}
			eventService.logEvent(eventInfo);
		} catch (Exception e) {
			return new ResponseEntity<Void>(HttpStatus.SERVICE_UNAVAILABLE);
		}
		return new ResponseEntity<Void>(HttpStatus.OK);
	}
}
