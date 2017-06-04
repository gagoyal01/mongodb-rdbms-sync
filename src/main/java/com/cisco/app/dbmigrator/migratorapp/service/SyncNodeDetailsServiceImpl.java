package com.cisco.app.dbmigrator.migratorapp.service;

import java.util.List;

import com.cisco.app.dbmigrator.migratorapp.config.SyncConfig;
import com.cisco.app.dbmigrator.migratorapp.constants.SyncConstants;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncNodeDetailsDao;
import com.cisco.app.dbmigrator.migratorapp.logging.entity.SyncNodeDetails;

public class SyncNodeDetailsServiceImpl implements SyncNodeDetailsService{
	private SyncNodeDetailsDao syncNodeDetailsDao = null;
	
	
	public SyncNodeDetailsDao getSyncNodeDetailsDao() {
		return syncNodeDetailsDao;
	}

	public void setSyncNodeDetailsDao(SyncNodeDetailsDao syncNodeDetailsDao) {
		this.syncNodeDetailsDao = syncNodeDetailsDao;
	}

	@Override
	public List<SyncNodeDetails> getNodeDetails() {
		return syncNodeDetailsDao.getNodeDetails(getLifeCycle());
	}

	private String getLifeCycle() {
		return SyncConfig.INSTANCE.getDbProperty(SyncConstants.LIFE)!=null?SyncConfig.INSTANCE.getDbProperty(SyncConstants.LIFE):"dev";
	}
}
