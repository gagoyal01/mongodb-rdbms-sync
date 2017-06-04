package com.cisco.app.dbmigrator.migratorapp.logging.codecs;

import org.bson.Document;

import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncNode;

public class SyncNodeEncoder {

	public static Document encodeNode(SyncNode nodeMapper){
		Document document = new Document();
		if(nodeMapper.getId()!=null){
			document.append(SyncAttrs.ID, nodeMapper.getId());
		}
		if(nodeMapper.getHostName()!=null && !nodeMapper.getHostName().isEmpty()){
			document.append(SyncAttrs.HOST, nodeMapper.getHostName());
		}
		if(nodeMapper.getNodeName()!=null && !nodeMapper.getNodeName().isEmpty()){
			document.append(SyncAttrs.NODE, nodeMapper.getNodeName());
		}
		if(nodeMapper.getJvmName()!=null && !nodeMapper.getJvmName().isEmpty()){
			document.append(SyncAttrs.JVM, nodeMapper.getJvmName());
		}
		if(nodeMapper.getLifeCycle()!=null && !nodeMapper.getLifeCycle().isEmpty()){
			document.append(SyncAttrs.LIFE_CYCLE, nodeMapper.getLifeCycle());
		}
		if(nodeMapper.getState()!=null && !nodeMapper.getState().isEmpty()){
			document.append(SyncAttrs.STATE, nodeMapper.getState());
		}
		if(nodeMapper.getConcurrencyLevel()!=0){
			document.append(SyncAttrs.CON_LEVEL, nodeMapper.getConcurrencyLevel());
		}
		if(nodeMapper.getTotalHeapSize()!=0){
			document.append(SyncAttrs.TOTAL_HEAP_SIZE, nodeMapper.getTotalHeapSize());	
		}
		if(nodeMapper.getUsedHeapSize()!=0){
			document.append(SyncAttrs.USED_HEAP_SIZE, nodeMapper.getUsedHeapSize());	
		}
		if(nodeMapper.getEventList()!=null){
			document.append(SyncAttrs.ACTIVE_EVENTS, nodeMapper.getEventList());	
		}
		if(nodeMapper.getEventTypes()!=null){
			document.append(SyncAttrs.EVENT_TYPES, nodeMapper.getEventTypes());
		}
		if(nodeMapper.getSystemEvents()!=null){
			document.append(SyncAttrs.SYSTEM_EVENTS, nodeMapper.getSystemEvents());
		}
		if(nodeMapper.getUUID()!=null){
			document.append(SyncAttrs.UUID, nodeMapper.getUUID());
		}
		if(nodeMapper.getFailureTime()!=0){
			document.append(SyncAttrs.FAILURE_TIME, nodeMapper.getFailureTime());	
		}		
		document.append(SyncAttrs.LAST_PING_TIME, System.currentTimeMillis());
		return document;
	}
	public static String getNodeJson(SyncNode node) {
		return SyncNodeEncoder.encodeNode(node).toJson();
	}
}
