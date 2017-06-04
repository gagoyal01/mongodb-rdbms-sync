package com.cisco.app.dbmigrator.migratorapp.logging.codecs;

import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncNode;

public class SyncNodeDecoder {

	@SuppressWarnings("unchecked")
	public static SyncNode decodeNode(Document document) {
		SyncNode nodeMapper=new SyncNode();
		nodeMapper.setId(document.getObjectId(SyncAttrs.ID));
		nodeMapper.setConcurrencyLevel(document.getInteger(SyncAttrs.CON_LEVEL,10));
		nodeMapper.setHostName(document.getString(SyncAttrs.HOST));
		nodeMapper.setJvmName(document.getString(SyncAttrs.JVM));
		nodeMapper.setNodeName(document.getString(SyncAttrs.NODE));
		nodeMapper.setState(document.getString(SyncAttrs.STATE));
		nodeMapper.setLifeCycle(document.getString(SyncAttrs.LIFE_CYCLE));
		if(document.get(SyncAttrs.TOTAL_HEAP_SIZE)!=null){
			nodeMapper.setTotalHeapSize(document.getLong(SyncAttrs.TOTAL_HEAP_SIZE));	
		}
		if(document.get(SyncAttrs.USED_HEAP_SIZE)!=null){
			nodeMapper.setUsedHeapSize(document.getLong(SyncAttrs.USED_HEAP_SIZE));	
		}		
		nodeMapper.setEventList((List<ObjectId>) document.get(SyncAttrs.ACTIVE_EVENTS));
		List<String> eventTypes = (List<String>) document.get(SyncAttrs.EVENT_TYPES);
		nodeMapper.setEventTypes(eventTypes!=null?eventTypes :new ArrayList<String>(1));
		nodeMapper.setSystemEvents((List<ObjectId>) document.get(SyncAttrs.SYSTEM_EVENTS));
		nodeMapper.setUUID(document.getString(SyncAttrs.UUID));
		if(document.get(SyncAttrs.FAILURE_TIME)!=null){
			nodeMapper.setFailureTime(document.getLong(SyncAttrs.FAILURE_TIME));
		}else{
			nodeMapper.setFailureTime(0L);
		}	
		if(document.get(SyncAttrs.LAST_PING_TIME)!=null){
			nodeMapper.setLastPingTime(document.getLong(SyncAttrs.LAST_PING_TIME));
		}else{
			nodeMapper.setLastPingTime(0L);
		}	
		return nodeMapper;
	}
}
