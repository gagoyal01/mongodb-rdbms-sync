package com.cisco.app.dbmigrator.migratorapp.core.job;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.config.SyncConfig;
import com.cisco.app.dbmigrator.migratorapp.constants.SyncConstants;
import com.cisco.app.dbmigrator.migratorapp.core.event.EventType;
import com.cisco.app.dbmigrator.migratorapp.core.event.O2MSyncDataLoader;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncEvent;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncMarker;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncEventDao;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncNodeDao;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncNode;
import com.cisco.app.dbmigrator.migratorapp.mail.Mailer;

@SuppressWarnings("rawtypes")
public enum NodeBalancer {
	INSTANCE;
	private final Logger logger = Logger.getLogger(NodeBalancer.class);
	private static final int DEFAULT_CONCURRENCY_LEVEL = 5;
	private static final String DIVISOR = "_Divisor";

	private final SyncNodeDao nodeDao;
	private final SyncEventDao eventDao;

	private SyncNode node;
	private ExecutorService nodeService;
	private AtomicInteger eventCount;
	private final Map<ObjectId, Runnable> eventRunnables;
	private List<ObjectId> incompleteEvents;
	private List<ObjectId> incompleteSystemEvents;

	private NodeBalancer() {
		logger.debug("Classloader" + Thread.currentThread().getContextClassLoader());
		nodeDao = new SyncNodeDao();
		SyncNode filter = new SyncNode();
		filter.setUUID(getGearUid());
		filter.setHostName(getHostName());
		filter.setNodeName(getNodeName());
		filter.setLifeCycle(getLifeCycle());
		node = nodeDao.getNodeDetails(filter);
		if (node == null) {
			node = filter;
			node.setConcurrencyLevel(DEFAULT_CONCURRENCY_LEVEL);
		}
		if (node.getEventList() != null) {
			incompleteEvents = new CopyOnWriteArrayList<ObjectId>(node.getEventList());// Collections.synchronizedList(node.getEventList());
		}
		if (node.getSystemEvents() != null) {
			incompleteSystemEvents = new CopyOnWriteArrayList<ObjectId>(node.getSystemEvents());// Collections.synchronizedList(node.getSystemEvents());
		}
		node.setEventList(new ArrayList<ObjectId>(node.getConcurrencyLevel()));
		node.setSystemEvents(new ArrayList<ObjectId>(node.getConcurrencyLevel()));
		logger.info("Concurrency level : " + node.getConcurrencyLevel());
		nodeService = Executors.newFixedThreadPool(node.getConcurrencyLevel());
		eventDao = new SyncEventDao();
		eventRunnables = new ConcurrentHashMap<ObjectId, Runnable>(node.getConcurrencyLevel());
		eventCount = new AtomicInteger(0);
	}

	private boolean isTokenAvailable() {
		boolean isTokenAvailable = false;
		if (eventCount.intValue() < node.getConcurrencyLevel()) {
			isTokenAvailable = true;
			logger.info("Token available");
		}
		return isTokenAvailable;
	}

	public void addEventToExecutor(SyncEvent event) {
		logger.info("Event picked for processing. EventName : " + event.getEventName());
		eventCount.incrementAndGet();
		node.getEventList().add(event.getEventId());
		nodeService.submit(event);
		if (event.getEventId().equals(event.getParentEventId())) {
			Mailer.sendmail(event, null, null, Mailer.STARTED);
		}
	}

	private void cleanUpCancelledEvents() {
		List<ObjectId> cancelledEvents = eventDao.checkCancelledEvents(eventRunnables.keySet());
		if (cancelledEvents != null && !cancelledEvents.isEmpty()) {
			for (ObjectId id : cancelledEvents) {
				Runnable cancelledEvent = eventRunnables.remove(id);
				if (cancelledEvent instanceof SyncEvent) {
					SyncEvent event = (SyncEvent) cancelledEvent;
					SyncMarker marker = null;
					if (event != null) {
						node.getEventList().remove(id);
						marker = event.getMarker();
						if (marker != null) {
							marker.setFailed(true);
						} else {
							marker = new SyncMarker();
							marker.setFailed(true);
							event.setMarker(marker);
						}
					}
				} else {
					O2MSyncDataLoader loaderEvent = (O2MSyncDataLoader) cancelledEvent;
					node.getSystemEvents().remove(id);
					loaderEvent.setCancelled(true);
				}
				Mailer.sendmail(id, null, null, Mailer.CANCELLED);
			}
		}
	}

	public void markEventAsCompleted(ObjectId eventId) {
		logger.info("Event completed with eventId " + eventId);
		eventDao.markEventAsCompleted(eventId);
		eventRunnables.remove(eventId);
		node.getEventList().remove(eventId);
		eventCount.decrementAndGet();
		process();
	}

	public void markEventAsFailed(ObjectId eventId) {
		logger.info("Event Failed with eventId " + eventId);
		eventCount.decrementAndGet();
		eventRunnables.remove(eventId);
		node.getEventList().remove(eventId);
		process();
	}

	public void process() {
		cleanUpCancelledEvents();
		if (node.getEventTypes() != null && node.getEventTypes().contains(String.valueOf(EventType.System))) {
			processDataLoaderThreads();
		}
		if (isTokenAvailable()) {
			SyncEvent event = null;
			if (incompleteEvents != null && !incompleteEvents.isEmpty()) {
				event = eventDao.getEvent(incompleteEvents.remove(0));
				if (event == null || SyncStatus.CANCELLED.equals(event.getStatus())
						|| !(EventType.MongoToOrclSync == event.getEventType()
								|| EventType.OrclToMongoSync == event.getEventType())) {
					return;
				}
				event.setRetry(true);
			} else {
				event = eventDao.getPendingEvent(node.getEventTypes());
			}
			if (event != null) {
				eventRunnables.put(event.getEventId(), event);
				if (event.getEventType() == EventType.OrclToMongo || event.getEventType() == EventType.OrclToMongoGridFs) {
					if (event.getParentEventId() == null
							|| event.getParentEventId().equals(event.getEventId())) {
						Thread divisor = new Thread(new O2MEventDistributorNew(event));
						divisor.setName(event.getEventName() + DIVISOR);
						divisor.start();
					} else {
						addEventToExecutor(event);
					}
				} else if (event.getEventType().equals(EventType.MongoToOrcl)) {
					addEventToExecutor(event);
				} else if (event.getEventType().equals(EventType.MongoToOrclSync)) {
					addEventToExecutor(event);
				} else if (event.getEventType().equals(EventType.OrclToMongoSync)) {
					addEventToExecutor(event);
				} else if (event.getEventType().equals(EventType.OrclToMongoGridFs)) {
					addEventToExecutor(event);
				}
			}
		}
		node.setTotalHeapSize(Runtime.getRuntime().maxMemory());
		node.setUsedHeapSize(Runtime.getRuntime().totalMemory());
		node = nodeDao.updateNodeDetails(node);
	}

	private void processDataLoaderThreads() {
		if (incompleteSystemEvents != null && !incompleteSystemEvents.isEmpty()) {
			ObjectId eventId = incompleteSystemEvents.remove(0);
			O2MSyncDataLoader loader = eventDao.getDataLoader(eventId);
			if (loader != null && !SyncStatus.CANCELLED.equals(loader.getStatus())) {
				nodeService.submit(loader);
				eventRunnables.put(loader.getEventId(), loader);
				node.getSystemEvents().add(eventId);
				logger.info("Loader event Resumed ");
			}
		} else {
			O2MSyncDataLoader loader = eventDao.getPendingDataLoader();
			if (loader != null) {
				nodeService.submit(loader);
				node.getSystemEvents().add(loader.getEventId());
				eventRunnables.put(loader.getEventId(), loader);
				logger.info("Loader event picked ");
			}
		}
	}

	private String getNodeName() {
		if (getLifeCycle() != null) {
			String absolutePath = System.getProperty("user.dir");
			return absolutePath.substring(absolutePath.lastIndexOf("/") + 1, absolutePath.length());
		} else {
			return "local";
		}
	}

	private String getLifeCycle() {
		
		return SyncConfig.INSTANCE.getDbProperty(SyncConstants.LIFE);
	}
	
	private String getGearUid() {
		if(getLifeCycle()!=null){
			Map<String, String> env = System.getenv();
			String gearUId = env.get("OPENSHIFT_GEAR_UUID");
			logger.info("Fetched Gear Id : "+gearUId);
			return gearUId;
	
		}else{
			logger.info("Fetched Gear Id : local");
			return "local";
		}
	}

	private String getHostName() {
		InetAddress localHost = null;
		try {
			localHost = InetAddress.getLocalHost();
			logger.info("Host name : " +localHost);
		} catch (UnknownHostException e) {
			logger.error("Error while getting host name", e);
		}
		return localHost != null ? localHost.getHostName() : null;
	}
}