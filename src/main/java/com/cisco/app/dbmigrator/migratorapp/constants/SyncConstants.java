package com.cisco.app.dbmigrator.migratorapp.constants;

/**
 * ENUM to hold Application level constants
 * 
 * @author pnilayam
 *
 */
public enum SyncConstants {
	CONST;
	public static final String USER= "user";
	public static final String CONTENT_TYPE_JSON = "application/json";
	public static final String EMPTY_STRING = "";
	public static final String SUCCESS = "Success";
	public static final int DEFAULT_BATCH_SIZE =10;
	public static final String SYNC_FLAG = "syncFlg";
	public static final String Y = "Y";
	public static final String SRC= "src";
	public static final String ORCL= "orcl";
	public static final String SYNC_TIME = "syncedOn";
	public static final String OPERATION = "o";
	public static final String DELETE = "DELETE";
	
	/**
	 * Constants to load configurations
	 */
	public static final String LIFE = "LIFE";
	public static final String APP_ID = "APP_ID";
	public static final String IS_PWD_ENCRYPTED = "IS_PWD_ENCRYPTED";
	public static final String ENCRYPTION_KEY = "ENCRYPTION_KEY";
	public static final String SSL_ENABLED = "SSL_ENABLED";
	public static final String SSL_ALLOW_INVALID_HOST = "SSL_ALLOW_INVALID_HOST";
	public static final String KEYSTORE_NAME = "KEYSTORE_NAME";
	public static final String KEYSTORE_PASS = "KEYSTORE_PASS";
	public static final String KEYSTORE_PATH = "KEYSTORE_PATH";
	public static final String TRUSTSTORE_NAME = "TRUSTSTORE_NAME";
	public static final String TRUSTSTORE_PASS = "TRUSTSTORE_PASS";
	public static final String TRUSTSTORE_PATH = "TRUSTSTORE_PATH";
	public static final String WRITE_CONCERN = "WRITE_CONCERN";
	public static final String LOGGING_LEVEL = "LOGGING_LEVEL";
	
	/**
	 * DB Property Constants
	 */
	public static final String DBNAME = "DBNAME";
	public static final String USERNAME = "USERNAME";
	public static final String PORT = "PORT";
	public static final String HOST = "HOST";
	public static final String PASSWORD = "PASSWORD";	
}