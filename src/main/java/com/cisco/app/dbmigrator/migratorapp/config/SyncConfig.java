package com.cisco.app.dbmigrator.migratorapp.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.cisco.app.dbmigrator.migratorapp.constants.SyncConstants;

/**
 * @author pnilayam
 * 
 *         Enum to hold configuration and properties
 *
 */
public enum SyncConfig {
	INSTANCE;
	private Logger logger = Logger.getLogger(getClass());
	private final Properties syncPropMap = new Properties();
	private final Properties dbPropMap = new Properties();
	private final Properties mailerPropMap = new Properties();
	private static final String SYNC_HOME = "SYNC_HOME";
	private boolean readFromClasspath = false;

	private void setLogLevel() {
		String logLevel = syncPropMap.getProperty(SyncConstants.LOGGING_LEVEL, "INFO");
		logger.debug("Logger level configured is : " + logLevel);
		Level level = Level.toLevel(logLevel, Level.DEBUG);
		LogManager.getRootLogger().setLevel(level);
		logger.info("Logging level set to " + level);
	}

	private void loadConfig(String syncHome) {
		/**
		 * Load sync configuration file.
		 */
		logger.info("SYNC_HOME is pointing to " + syncHome);
		if(readFromClasspath){
			logger.info("Loading Configuration file sync.conf from classpath");
			try (InputStream confFileIs = SyncConfig.class.getResourceAsStream("/sync/sync.conf")) {
				Properties confProp = new Properties();
				confProp.load(confFileIs);
				for (Map.Entry<Object, Object> entry : confProp.entrySet()) {
					syncPropMap.setProperty(String.valueOf(entry.getKey()).toUpperCase(), String.valueOf(entry.getValue()));
				}
			} catch (Exception e) {
				logger.error("Error while loading configuration file from classpath", e);
				System.exit(0);
			}
		}else{
			File confFile = new File(syncHome + "\\sync.conf");
			if (!confFile.exists()) {
				logger.warn("Configuration file sync.conf not found at SYNC_HOME. Exiting process");
				System.exit(0);
			}
			try (InputStream confFileIs = new FileInputStream(confFile)) {
				Properties confProp = new Properties();
				confProp.load(confFileIs);
				for (Map.Entry<Object, Object> entry : confProp.entrySet()) {
					syncPropMap.setProperty(String.valueOf(entry.getKey()).toUpperCase(), String.valueOf(entry.getValue()));
				}
			} catch (Exception e) {
				logger.error("Error while loading configuration file", e);
				System.exit(0);
			}
		}		
		logger.debug("Config Properties " + syncPropMap);
	}
	
	private void loadMailerConfig(String syncHome) {
		/**
		 * Load sync configuration file.
		 */
		logger.info("SYNC_HOME is pointing to " + syncHome);
		if(readFromClasspath){
			logger.info("Loading Configuration file sync.conf from classpath");
			try (InputStream confFileIs = SyncConfig.class.getResourceAsStream("/sync/mailer.properties")) {
				Properties mailerProp = new Properties();
				mailerProp.load(confFileIs);
				for (Map.Entry<Object, Object> entry : mailerProp.entrySet()) {
					mailerPropMap.setProperty(String.valueOf(entry.getKey()).toUpperCase(), String.valueOf(entry.getValue()));
				}
			} catch (Exception e) {
				logger.error("Error while loading configuration file from classpath", e);
				System.exit(0);
			}
		}else{
			File confFile = new File(syncHome + "\\mailer.properties");
			if (!confFile.exists()) {
				logger.warn("Configuration file sync.conf not found at SYNC_HOME. Exiting process");
				System.exit(0);
			}
			try (InputStream confFileIs = new FileInputStream(confFile)) {
				Properties mailerProp = new Properties();
				mailerProp.load(confFileIs);
				for (Map.Entry<Object, Object> entry : mailerProp.entrySet()) {
					mailerPropMap.setProperty(String.valueOf(entry.getKey()).toUpperCase(), String.valueOf(entry.getValue()));
				}
			} catch (Exception e) {
				logger.error("Error while loading configuration file", e);
				System.exit(0);
			}
		}		
		logger.debug("Mailer Properties " + mailerPropMap);
	}

	private void loadDbProperties(String syncHome) {
		/**
		 * Load DB property file.
		 */
		String lifeCycle = syncPropMap.getProperty(SyncConstants.LIFE);
		if (lifeCycle == null || lifeCycle.isEmpty()) {
			logger.info("life property not set in sync.conf file. Defaulting it to local");
			syncPropMap.setProperty(SyncConstants.LIFE, "local");
			lifeCycle = "local";
		}
		if(readFromClasspath){
			String dbFilePath = "/sync/db-props/" + lifeCycle + ".properties";
			logger.info("Looking for DB property file at this path : " + dbFilePath);
			try (InputStream dbPropFileIs = SyncConfig.class.getResourceAsStream(dbFilePath)) {
				Properties dbProp = new Properties();
				dbProp.load(dbPropFileIs);
				for (Map.Entry<Object, Object> entry : dbProp.entrySet()) {
					dbPropMap.setProperty(String.valueOf(entry.getKey()).toUpperCase(), String.valueOf(entry.getValue()));
				}
			} catch (Exception e) {
				logger.error("Error while loading db configuration file", e);
				System.exit(0);
			}
		}else{
			String dbFilePath = syncHome + "\\db-props\\" + lifeCycle + ".properties";
			logger.info("Looking for DB property file at this path : " + dbFilePath);
			File dbFile = new File(dbFilePath);
			if (!dbFile.exists()) {
				logger.error("Configuration file " + dbFilePath + " not found. Exiting process");
				System.exit(0);
			}
			try (InputStream dbPropFileIs = new FileInputStream(dbFile)) {
				Properties dbProp = new Properties();
				dbProp.load(dbPropFileIs);
				for (Map.Entry<Object, Object> entry : dbProp.entrySet()) {
					dbPropMap.setProperty(String.valueOf(entry.getKey()).toUpperCase(), String.valueOf(entry.getValue()));
				}
			} catch (Exception e) {
				logger.error("Error while loading db configuration file", e);
				System.exit(0);
			}
		}		
		logger.debug("Loaded DB Properties : " + dbPropMap);

		String isSslEnabledProp = syncPropMap.getProperty(SyncConstants.SSL_ENABLED);
		if (isSslEnabledProp != null && !isSslEnabledProp.isEmpty()) {
			logger.info(
					"SSL is enabled. Looking for keystore and truststore files in %SYNC_HOME%/certs folder. SSL_ENABLED value in conf is : "
							+ isSslEnabledProp);
			verifyKeystoreFile(syncHome);
			verifyTruststoreFile(syncHome);
		}

	}

	private SyncConfig() {
		logger.info("Initializing Sync Config");

		/**
		 * Get SYNC_HOME property.
		 */
		String syncHome = System.getenv(SYNC_HOME);
		if (syncHome == null || syncHome.isEmpty()) {
			logger.error("SYNC_HOME not set. Exiting process");
			readFromClasspath = true;
		}
		//syncPropMap.setProperty(SYNC_HOME, syncHome);

		loadConfig(syncHome);
		setLogLevel();
		loadDbProperties(syncHome);
		loadMailerConfig(syncHome);
		
		logger.info("Sync configuration loaded");
	}

	private void verifyKeystoreFile(String syncHome) {
		logger.info("Checking for keystore file");
		String keystoreFileName = syncPropMap.getProperty(SyncConstants.KEYSTORE_NAME);
		if(readFromClasspath){
			String keystorePath ="/sync/certs/" + keystoreFileName;
			URL resource = SyncConfig.class.getResource(keystorePath);
			if(resource==null){
				logger.warn("keystore file " + keystoreFileName + " not found. Make sure keystore is added to JRE.");
			}else {
				logger.info("truststore file found. Setting trusstorePath as " + resource.getPath());
				syncPropMap.setProperty(SyncConstants.KEYSTORE_PATH, resource.getPath());
			}
		}else{
			String keystorePath = syncHome + "\\certs\\" + keystoreFileName;
			logger.info("keystore path derived as : " + keystorePath);
			File keyStoreFile = new File(keystorePath);
			if (!keyStoreFile.exists()) {
				logger.warn("keystore file " + keystoreFileName + " not found. Make sure keystore is added to JRE.");
			} else {
				logger.info("truststore file found. Setting trusstorePath as " + keyStoreFile.getAbsolutePath());
				syncPropMap.setProperty(SyncConstants.KEYSTORE_PATH, keyStoreFile.getAbsolutePath());
			}
		}		
	}

	private void verifyTruststoreFile(String syncHome) {
		logger.info("Checking for truststore file");
		String truststoreFileName = syncPropMap.getProperty(SyncConstants.TRUSTSTORE_NAME);
		if(readFromClasspath){
			String truststorePath ="/sync/certs/" + truststoreFileName;
			logger.info("truststore path derived as : " + truststorePath);
			URL resource = SyncConfig.class.getResource(truststorePath);
			if (resource==null) {
				logger.warn("keystore file " + truststoreFileName + " not found. Make sure keystore is added to JRE.");
			} else {
				logger.info("truststore file found. Setting trusstorePath as " + resource.getPath());
				syncPropMap.setProperty(SyncConstants.TRUSTSTORE_PATH, resource.getPath());
			}	
		}
		String truststorePath = syncHome + "\\certs\\" + truststoreFileName;
		logger.info("truststore path derived as : " + truststorePath);
		File truststoreFile = new File(truststorePath);
		if (!truststoreFile.exists()) {
			logger.warn("keystore file " + truststoreFileName + " not found. Make sure keystore is added to JRE.");
		} else {
			logger.info("truststore file found. Setting trusstorePath as " + truststoreFile.getAbsolutePath());
			syncPropMap.setProperty(SyncConstants.TRUSTSTORE_PATH, truststoreFile.getAbsolutePath());
		}
	}

	public String getProperty(String key) {
		return syncPropMap.getProperty(key);
	}

	public String getDbProperty(String key) {
		return dbPropMap.getProperty(key);
	}
	
	public String getMailerProperty(String key) {
		return mailerPropMap.getProperty(key);
	}
}