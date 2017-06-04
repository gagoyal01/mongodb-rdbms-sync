package com.cisco.app.dbmigrator.migratorapp.logging.entity;

import java.util.List;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncEvent;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncNode;

@SuppressWarnings("rawtypes")
public class SyncNodeDetails {
	private SyncNode node;
	private List<SyncEvent> event;
	
	public SyncNode getNode() {
		return node;
	}
	public void setNode(SyncNode node) {
		this.node = node;
	}
	public List<SyncEvent> getEvent() {
		return event;
	}
	public void setEvent(List<SyncEvent> event) {
		this.event = event;
	}
}
