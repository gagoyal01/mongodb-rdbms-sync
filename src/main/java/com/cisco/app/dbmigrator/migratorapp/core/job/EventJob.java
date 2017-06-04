package com.cisco.app.dbmigrator.migratorapp.core.job;

import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

public class EventJob extends QuartzJobBean{
	private static Logger logger = Logger.getLogger(EventJob.class);
	@Override
	protected void executeInternal(JobExecutionContext arg0) throws JobExecutionException {
		logger.debug("Migration Job triggered");
		NodeBalancer.INSTANCE.process();
	}
}
