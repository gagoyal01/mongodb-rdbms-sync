package com.cisco.app.dbmigrator.migratorapp.core.job;

import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncNodeDao;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncNode;
import com.cisco.app.dbmigrator.migratorapp.mail.Mailer;

public class NodeMonitorJob extends QuartzJobBean{
	//private final Logger logger = Logger.getLogger(NodeMonitorJob.class);
	
	@Override
	protected void executeInternal(JobExecutionContext arg0) throws JobExecutionException {
		//logger.info("Node Monitor Thread Triggered");
		NodeMonitorUtil.INSTANCE.getFailedNode();
	}
	
	private enum NodeMonitorUtil{
		INSTANCE;
		private static final long timeDuration = 300000;
		private static final Logger logger = Logger.getLogger(NodeMonitorUtil.class);
		private final SyncNodeDao nodeDao;
		
		private NodeMonitorUtil(){
			nodeDao = new SyncNodeDao();
		}
		
		public void getFailedNode(){
			SyncNode failedNode = nodeDao.getFailedNode(System.currentTimeMillis()-timeDuration);
			if(failedNode==null){
				logger.info("All nodes are fine");
			}else{
				logger.info("Failed Host name "+ failedNode.getHostName());	
				logger.info("Failed AppId "+ failedNode.getUUID());
				if(failedNode.getFailureTime()==0){
					failedNode.setFailureTime(failedNode.getLastPingTime());
				}
				Mailer.sendMailForFailedNode(failedNode);
			}
			
		}
	}
}