package com.cisco.app.dbmigrator.migratorapp.service;

import java.util.List;
import com.cisco.app.dbmigrator.migratorapp.logging.entity.SyncNodeDetails;

public interface SyncNodeDetailsService {

	public List<SyncNodeDetails> getNodeDetails();
}
