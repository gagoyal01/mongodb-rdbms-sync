package com.cisco.app.dbmigrator.migratorapp.interceptor;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.cisco.app.dbmigrator.migratorapp.config.SyncConfig;
import com.cisco.app.dbmigrator.migratorapp.constants.SyncConstants;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncUserDao;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncUser;

public class HttpAuthInterceptor extends HandlerInterceptorAdapter {
	Logger logger = Logger.getLogger("HttpAuthInterceptor");

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
			HttpSession httpSession=request.getSession(false);
			if(httpSession==null){
				logger.info("Creating a new Http session");
				httpSession=request.getSession(true);
			}
			String userId= SyncConfig.INSTANCE.getProperty("SYNC_USER");//request.getRemoteUser();
			SyncUser user = (SyncUser) httpSession.getAttribute(SyncConstants.USER);
			if(user==null){
				SyncUserDao userDao = new SyncUserDao();
				user = userDao.getUser(userId);
				httpSession.setAttribute(SyncConstants.USER, user);
			}
			return true;
	}
}