package com.cisco.app.dbmigrator.migratorapp.utilities.cache;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import com.cisco.app.dbmigrator.migratorapp.config.ApplicationContextProvider;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncConnectionDao;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncConnectionInfo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * Class to create caches for Connection Pools.<br>
 * While the application is UP and running, there will be just one instance of
 * Connection pool for any given combination of Database and Schema.<br>
 * Synchronized methods are available to get cached instances.
 * 
 * @author pnilayam
 *
 */
public enum DBCacheManager {
	INSTANCE;
	private CacheManager manager;
	private final String DOT = ".";
	private final String ORACLE_POOL = "oraclePool";
	private final String MONGO_POOL = "mongoPool";
	private Logger logger = Logger.getLogger(DBCacheManager.class);
	private final SyncConnectionDao connectionDao;
	
	/**
	 * Synchronized method to get cached instance of OracleOCIConnectionPool.
	 * Pools are initialized with below settings<br>
	 * CONNPOOL_MAX_LIMIT : 100<br>
	 * CONNPOOL_MIN_LIMIT : 10<br>
	 * CONNPOOL_INCREMENT : 10<br>
	 * Statement Level Caching Enabled
	 * 
	 * @param dbName
	 * @param dbUserName
	 * @return OracleOCIConnectionPool
	 * @throws SQLException
	 */
	public synchronized Connection getCachedOracleConnection(String dbName, String dbUserName) throws SQLException {
		logger.debug("Start of method : getCachedOracleConnectionPool");
		JdbcTemplate template = null;
		Cache cache = manager.getCache(ORACLE_POOL);
		logger.debug("Entered Key is : " + dbName);
		SyncConnectionInfo oracleConnectionInfo = connectionDao.getConnection(dbName, dbUserName);
		Element element = cache.get(dbName);
		logger.debug("Current cache size: " + cache.getSize());
		if (element != null) {
			logger.info("Pool found for entered Key: " + dbName);
			template = (JdbcTemplate) element.getObjectValue();
		} else {
			logger.info("Pool Not found for entered Key: " + dbName);
			template = getDbTemplate(dbName);
			Element newElement = new Element(dbName, template);
			cache.put(newElement);
			logger.info("New Pool created and added to Cache for key : " + dbName);
		}
		logger.debug("End of method : getCachedOracleConnectionPool");
		String decryptedPwd = null;
		try {
			decryptedPwd = oracleConnectionInfo.getPassword();
		} catch (Exception e) {
			logger.error("Error in decrypting password for Username : " + oracleConnectionInfo.getUserName(), e);
		}
		return template.getDataSource().getConnection(oracleConnectionInfo.getUserName(), decryptedPwd);
	}

	//
	@SuppressWarnings("resource")
	private JdbcTemplate getDbTemplate(String dbName) {
		JdbcTemplate template = null;
		if(new File(System.getenv("SYNC_HOME") + "\\applicationContext.xml").exists()){
			try{
			template = new FileSystemXmlApplicationContext(System.getenv("SYNC_HOME") + "\\applicationContext.xml")
					.getBean(dbName, org.springframework.jdbc.core.JdbcTemplate.class);
			}catch (NoSuchBeanDefinitionException e) {
				template = ApplicationContextProvider.getApplicationContext().getBean(dbName,
						org.springframework.jdbc.core.JdbcTemplate.class);
			}
		}else{
			template = ApplicationContextProvider.getApplicationContext().getBean(dbName,
					org.springframework.jdbc.core.JdbcTemplate.class);
		}
		
		return template;
	}

	/**
	 * Synchronized method to get cached instance of MongoClient.
	 * 
	 * @param dbName
	 * @param dbUserName
	 * @return Cached instance of MongoDB Connection Pool
	 * 
	 * 
	 */
	public synchronized MongoClient getCachedMongoPool(String dbName, String userName) {
		logger.debug("Start of method : getOracleConnectionPool");
		Cache cache = manager.getCache(MONGO_POOL);
		if (userName == null || userName.isEmpty()) {
			userName = "Default";
		}
		String key = dbName + DOT + userName;
		logger.debug("Entered Key is : " + key);
		MongoClient client;
		Element element = cache.get(key);
		logger.debug("Current cache size: " + cache.getSize());
		if (element != null) {
			logger.debug("Pool found for entered Key: " + key);
			client = (MongoClient) element.getObjectValue();
		} else {
			logger.debug("Pool Not found for entered Key: " + key);
			client = getMongoClient(dbName, userName);
			if(client!=null){
				Element newElement = new Element(key, client);
				cache.put(newElement);
				logger.debug("New Pool created and added to Cache for key : " + key);	
			}			
		}
		return client;
	}

	private DBCacheManager() {
		logger.debug("Loading DBCacheManager");
		// orclJdbcTemplate =
		// ApplicationContextProvider.getApplicationContext().getBean("orclJdbcTemplate"
		// , org.springframework.jdbc.core.JdbcTemplate.class);
		System.setProperty("net.sf.ehcache.skipUpdateCheck", "true");
		connectionDao = new SyncConnectionDao();
		manager = CacheManager.create();
		Cache oraclePool = new Cache(ORACLE_POOL, 20, false, false, 3000, 1000);
		manager.addCache(oraclePool);
		logger.debug("ORACLE_POOL cache added to manager");
		Cache mongoPool = new Cache(MONGO_POOL, 20, false, false, 3000, 1000);
		manager.addCache(mongoPool);
		logger.debug("MONGO_POOL cache added to manager");
		logger.debug("DBCacheManager loaded");
	}

/*	private OracleOCIConnectionPool getOracleConnectionPool(String dbName, String dbUserName,
			SyncConnectionInfo oracleConnectionInfo) {
		logger.debug("Started building Oracle Connection Pool");
		OracleOCIConnectionPool connectionPool = null;
		try {
			connectionPool = new OracleOCIConnectionPool();
			connectionPool.setUser(oracleConnectionInfo.getUserName());
			connectionPool.setPassword(oracleConnectionInfo.getPassword());
			
			 * if(oracleConnectionInfo.getUrl()!=null &&
			 * !oracleConnectionInfo.getUrl().isEmpty()){
			 * connectionPool.setURL("jdbc:oracle:oci:@" +
			 * oracleConnectionInfo.getUrl()); }else{
			 * connectionPool.setURL("jdbc:oracle:oci:@" + dbName); }
			 
			logger.debug("Pool Created for schema : " + dbName);
			Properties configProperties = new Properties();
			configProperties.setProperty(OracleOCIConnectionPool.CONNPOOL_MAX_LIMIT, String.valueOf(10));
			configProperties.setProperty(OracleOCIConnectionPool.CONNPOOL_MIN_LIMIT, String.valueOf(2));
			configProperties.setProperty(OracleOCIConnectionPool.CONNPOOL_INCREMENT, String.valueOf(1));
			// configProperties.setProperty(OracleOCIConnectionPool.CONNPOOL_TIMEOUT,
			// String.valueOf(20));
			logger.debug("Pool setting :: CONNPOOL_MAX_LIMIT 100 , CONNPOOL_MIN_LIMIT 10, Statement Caching Enabled");
			connectionPool.setPoolConfig(configProperties);

			connectionPool.setImplicitCachingEnabled(true);
			logger.debug("Completed building Oracle Connection Pool");
			return connectionPool;
		} catch (SQLException e) {
			logger.error("Error while building Oracle connection pool", e);
		} catch (Exception e) {
			logger.error("Unexpected Error ", e);
		}
		return connectionPool;
	}*/

	private MongoClient getMongoClient(String dbName, String userName) {
		SyncConnectionInfo connectionInfo = connectionDao.getConnection(dbName, userName);
		List<ServerAddress> addressList = null;
		List<MongoCredential> credList = null;
		MongoClient client = null;
		MongoClientOptions.Builder optionsBuilder = new MongoClientOptions.Builder();
		String decryptedPassword = null;
		try {
			optionsBuilder.readPreference(ReadPreference.primary());
			if (connectionInfo.getPassword() != null && !connectionInfo.getPassword().isEmpty()) {
				decryptedPassword = connectionInfo.getPassword();
				MongoCredential credential = MongoCredential.createCredential(userName, dbName,
						decryptedPassword.toCharArray());
				credList = new ArrayList<MongoCredential>();
				credList.add(credential);
			}
			if (connectionInfo.getHostToPortMap() != null && !connectionInfo.getHostToPortMap().isEmpty()) {
				addressList = new ArrayList<ServerAddress>();
				ServerAddress address = null;
				for (Map.Entry<String, Double> hostPort : connectionInfo.getHostToPortMap().entrySet()) {
					address = new ServerAddress(hostPort.getKey(), hostPort.getValue().intValue());
					addressList.add(address);
				}
			}
			if (credList != null) {
				client = new MongoClient(addressList, credList, optionsBuilder.build());
			} else {
				client = new MongoClient(addressList, optionsBuilder.build());
			}
		} catch (Exception e) {
			logger.error("Error while creating mongoClient " ,e);
		}
		return client;
	}
}