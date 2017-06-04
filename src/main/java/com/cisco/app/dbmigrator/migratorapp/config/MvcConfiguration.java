package com.cisco.app.dbmigrator.migratorapp.config;

import java.util.Properties;

import org.quartz.Trigger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.cisco.app.dbmigrator.migratorapp.core.job.EventJob;
import com.cisco.app.dbmigrator.migratorapp.core.job.NodeMonitorJob;
import com.cisco.app.dbmigrator.migratorapp.interceptor.HttpAuthInterceptor;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncMapAndEventDecoder;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncMapAndEventEncoder;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncConnectionDao;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncEventDao;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncMapDao;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncNodeDetailsDao;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncUserDao;
import com.cisco.app.dbmigrator.migratorapp.service.AdminService;
import com.cisco.app.dbmigrator.migratorapp.service.AdminServiceImpl;
import com.cisco.app.dbmigrator.migratorapp.service.SyncEventService;
import com.cisco.app.dbmigrator.migratorapp.service.SyncEventServiceImpl;
import com.cisco.app.dbmigrator.migratorapp.service.SyncMapService;
import com.cisco.app.dbmigrator.migratorapp.service.SyncMapServiceImpl;

@Configuration
@ComponentScan(basePackages = { "com.cisco.app.dbmigrator.migratorapp.config",
		"com.cisco.app.dbmigrator.migratorapp.interceptor", "com.cisco.app.dbmigrator.migratorapp.controller" })
@EnableWebMvc
@ImportResource("classpath:/applicationContext.xml")
public class MvcConfiguration extends WebMvcConfigurerAdapter {
	@Bean
	public ViewResolver getViewResolver() {
		InternalResourceViewResolver resolver = new InternalResourceViewResolver();
		resolver.setPrefix("/WEB-INF/views/");
		resolver.setSuffix(".jsp");
		return resolver;
	}
	
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new HttpAuthInterceptor());
		super.addInterceptors(registry);
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/resources/**").addResourceLocations("/resources/");
	}

	@Bean
	public SyncEventDao getSyncEventDao() {
		return new SyncEventDao();
	}

	@Bean
	public SyncMapDao getSyncMapDao() {
		return new SyncMapDao();
	}

	@Bean
	public SyncMapAndEventDecoder getSyncMapAndEventDecoder() {
		return new SyncMapAndEventDecoder();
	}

	@Bean
	public SyncNodeDetailsDao getSyncNodeDetailsDao() {
		return new SyncNodeDetailsDao();
	}

	@Bean
	public SyncUserDao getSyncUserDao() {
		return new SyncUserDao();
	}

	@Bean
	public SyncConnectionDao getSyncConnectionDao() {
		return new SyncConnectionDao();
	}

	@Bean
	public SyncMapAndEventEncoder getSyncMapAndEventEncoder() {
		return new SyncMapAndEventEncoder();
	}

	@Bean
	public SyncEventService getSyncEventService() {
		SyncEventServiceImpl eventServiceImpl = new SyncEventServiceImpl();
		eventServiceImpl.setEventDao(getSyncEventDao());
		eventServiceImpl.setDecoder(getSyncMapAndEventDecoder());
		return eventServiceImpl;
	}

	@Bean
	public SyncMapService getSyncMapService() {
		SyncMapServiceImpl mapServiceImpl = new SyncMapServiceImpl();
		mapServiceImpl.setMapDao(getSyncMapDao());
		mapServiceImpl.setDecoder(getSyncMapAndEventDecoder());
		return mapServiceImpl;
	}

	@Bean
	public AdminService getAdminService() {
		AdminServiceImpl adminService = new AdminServiceImpl();
		adminService.setSyncNodeDetailsDao(getSyncNodeDetailsDao());
		adminService.setSyncUserDao(getSyncUserDao());
		adminService.setConnectionDao(getSyncConnectionDao());
		return adminService;
	}
	
	@Bean
	@Scope(value = "prototype")
	public JobDetailFactoryBean getMonitorJob(){
		JobDetailFactoryBean bean = new JobDetailFactoryBean();
		bean.setJobClass(NodeMonitorJob.class);
		return bean;
	}
	
	@Bean
	public SimpleTriggerFactoryBean getMonitorJobTrigger(){
		SimpleTriggerFactoryBean bean = new SimpleTriggerFactoryBean();
		bean.setJobDetail(getMonitorJob().getObject());
		bean.setStartDelay(120000);
		bean.setRepeatInterval(60000);
		return bean;
	}
	
	@Bean
	public SchedulerFactoryBean getEventHealthMonitorBean(){
		SchedulerFactoryBean bean = new SchedulerFactoryBean();
		bean.setBeanName("EventHealthMonitorThread");
		bean.setTriggers(new Trigger [] {getMonitorJobTrigger().getObject()});
		Properties prop = new Properties();
		prop.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
		prop.setProperty("org.quartz.threadPool.threadCount", "1");
		bean.setQuartzProperties(prop);
		return bean;
	}
	
	@Bean
	@Scope(value = "prototype")
	public JobDetailFactoryBean getEventJob(){
		JobDetailFactoryBean bean = new JobDetailFactoryBean();
		bean.setJobClass(EventJob.class);
		return bean;
	}
	
	@Bean
	public SimpleTriggerFactoryBean getEventJobTrigger(){
		SimpleTriggerFactoryBean bean = new SimpleTriggerFactoryBean();
		bean.setJobDetail(getEventJob().getObject());
		bean.setStartDelay(40000);
		bean.setRepeatInterval(60000);
		return bean;
	}
	
	@Bean
	public SchedulerFactoryBean getEventJobBean(){
		SchedulerFactoryBean bean = new SchedulerFactoryBean();
		bean.setBeanName("EligibleEventProcessorThread");
		bean.setTriggers(new Trigger [] {getEventJobTrigger().getObject()});
		Properties prop = new Properties();
		prop.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
		prop.setProperty("org.quartz.threadPool.threadCount", "1");
		bean.setQuartzProperties(prop);
		return bean;
	}
}
