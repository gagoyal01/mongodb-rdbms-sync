package com.cisco.app.dbmigrator.migratorapp.logging.connection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.slf4j.LoggerFactory;

import com.cisco.app.dbmigrator.migratorapp.config.SyncConfig;
import com.cisco.app.dbmigrator.migratorapp.constants.SyncConstants;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.MngToOrclEventCodec;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.MngToOrclMapCodec;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.MngToOrclSyncEventCodec;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.O2MSyncEventLogCodec;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.OrclToMngEventCodec;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.OrclToMngGridFsEventCodec;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.OrclToMngMapCodec;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.OrclToMngSyncEventCodec;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.OrclToMongoGridFsMapCodec;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncConnectionInfoCodec;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncErrorCodec;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncEventCodec;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncMapCodec;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncMarkerCodec;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncNodeCodec;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncNodeDetailsCodec;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncUserDetailCodec;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncUserSessionCodec;
import com.cisco.app.dbmigrator.migratorapp.utilities.encrypt.EncryptorDecryptor;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoDatabase;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public enum MongoConnection {
	INSTANCE;
	private MongoDatabase mongoDataBase;
	private final Logger logger;
	private final String ERR_MSG = "Error while initializing mongo client";

	public MongoDatabase getMongoDataBase() {
		return mongoDataBase;
	}

	private MongoConnection() {
		logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		logger.info("Start Creating Connection Enum");

		logger.setLevel(getLogLevel());
		if (isSslEnabled()) {
			System.setProperty("javax.net.ssl.trustStore",
					SyncConfig.INSTANCE.getProperty(SyncConstants.TRUSTSTORE_PATH));
			System.setProperty("javax.net.ssl.trustStorePassword",
					SyncConfig.INSTANCE.getProperty(SyncConstants.TRUSTSTORE_PASS));
			System.setProperty("javax.net.ssl.keyStore", SyncConfig.INSTANCE.getProperty(SyncConstants.KEYSTORE_PATH));
			System.setProperty("javax.net.ssl.keyStorePassword",
					SyncConfig.INSTANCE.getProperty(SyncConstants.TRUSTSTORE_PASS));

		}

		Codec<Document> defaultDocumentCodec = MongoClient.getDefaultCodecRegistry().get(Document.class);

		CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry(),
				CodecRegistries.fromCodecs(new SyncMarkerCodec(defaultDocumentCodec),
						new OrclToMngSyncEventCodec(defaultDocumentCodec), new SyncErrorCodec(defaultDocumentCodec),
						new SyncUserDetailCodec(defaultDocumentCodec), new SyncUserSessionCodec(defaultDocumentCodec),
						new SyncConnectionInfoCodec(defaultDocumentCodec), new SyncNodeCodec(defaultDocumentCodec),
						new SyncEventCodec(defaultDocumentCodec), new SyncMapCodec(defaultDocumentCodec),
						new OrclToMngMapCodec(defaultDocumentCodec), new OrclToMngEventCodec(defaultDocumentCodec),
						new MngToOrclMapCodec(defaultDocumentCodec), new MngToOrclEventCodec(defaultDocumentCodec),
						new MngToOrclSyncEventCodec(defaultDocumentCodec),
						new O2MSyncEventLogCodec(defaultDocumentCodec), new SyncNodeDetailsCodec(defaultDocumentCodec),
						new OrclToMongoGridFsMapCodec(defaultDocumentCodec),
						new OrclToMngGridFsEventCodec(defaultDocumentCodec)));

		MongoClientOptions options = MongoClientOptions.builder().writeConcern(getWriteConcern())
				.sslEnabled(isSslEnabled()).sslInvalidHostNameAllowed(allowInvalidHosts()).codecRegistry(codecRegistry)
				.build();
		try {
			setMongoDataBase(options);
			logger.info("Connection Enum created");
		} catch (Exception e) {
			logger.error(ERR_MSG, e);
		}
	}

	private WriteConcern getWriteConcern() {
		WriteConcern writeConcern = null;
		try {
			String writeConcernProp = SyncConfig.INSTANCE.getProperty(SyncConstants.WRITE_CONCERN);
			if (writeConcernProp != null && !writeConcernProp.isEmpty()) {
				logger.info("Write Concern loaded from DB : " + writeConcernProp);
				writeConcern = WriteConcern.valueOf(writeConcernProp);
			}
			if (writeConcern == null) {
				writeConcern = WriteConcern.MAJORITY;
			}
		} catch (Exception e) {
			logger.error("Error while setting WRITE_CONCERN from loaded configuration", e);
			writeConcern = WriteConcern.MAJORITY;
		}
		logger.info("Write Concern set as : " + writeConcern);
		return writeConcern;
	}

	private boolean isSslEnabled() {
		boolean isSslEnabled = false;
		try {
			String isSslEnabledProp = SyncConfig.INSTANCE.getProperty(SyncConstants.SSL_ENABLED);
			if (isSslEnabledProp != null && !isSslEnabledProp.isEmpty()) {
				logger.info("SSL_ENABLED value loaded from config : " + isSslEnabledProp);
				isSslEnabled = Boolean.valueOf(isSslEnabledProp);
			}
		} catch (Exception e) {
			logger.error("Error while retreiving SSL_ENABLED value from loaded configuration", e);
			isSslEnabled = false;
		}
		logger.info("SSL_ENABLED : " + isSslEnabled);
		return isSslEnabled;
	}

	private boolean allowInvalidHosts() {
		boolean allowInvalidHosts = false;
		try {
			String allowInvalidHostsProp = SyncConfig.INSTANCE.getProperty(SyncConstants.SSL_ALLOW_INVALID_HOST);
			if (allowInvalidHostsProp != null && !allowInvalidHostsProp.isEmpty()) {
				allowInvalidHosts = Boolean.valueOf(allowInvalidHostsProp);
			}
		} catch (Exception e) {
			allowInvalidHosts = false;
		}
		logger.info("ALLOW_INVALID_HOSTS : " + allowInvalidHosts);
		return allowInvalidHosts;
	}

	private Level getLogLevel() {
		String logLevel = SyncConfig.INSTANCE.getProperty(SyncConstants.LOGGING_LEVEL);
		logger.debug("Mongo Logger level configured to : " + logLevel);
		Level level = Level.toLevel(logLevel, Level.DEBUG);
		return level;
	}

	private boolean isPwdEncrypted() {
		boolean isPwdEncrypted = false;
		String encryptedProp = SyncConfig.INSTANCE.getDbProperty(SyncConstants.IS_PWD_ENCRYPTED);
		if (encryptedProp != null && !encryptedProp.isEmpty()) {
			isPwdEncrypted = Boolean.valueOf(encryptedProp);
		}
		logger.info("Password encryption flag is : "+isPwdEncrypted);
		return isPwdEncrypted;
	}

	private String getPassword() throws Exception {
		if (isPwdEncrypted()) {
			return EncryptorDecryptor.decrypt(SyncConfig.INSTANCE.getDbProperty(SyncConstants.PASSWORD),
					SyncConfig.INSTANCE.getDbProperty(SyncConstants.ENCRYPTION_KEY));
		} else {
			return SyncConfig.INSTANCE.getDbProperty(SyncConstants.PASSWORD);
		}
	}

	private void setMongoDataBase(MongoClientOptions options) throws Exception {
		List<ServerAddress> addressList = null;
		List<MongoCredential> credList = null;
		MongoCredential credential = null;
		String life = SyncConfig.INSTANCE.getProperty(SyncConstants.LIFE);
		logger.info("Loading DB Properties for " + life);
		SyncConfig config = SyncConfig.INSTANCE;
		String dbname = config.getDbProperty(SyncConstants.DBNAME);
		String username = config.getDbProperty(SyncConstants.USERNAME);
		int port = Integer.valueOf(config.getDbProperty(SyncConstants.PORT));
		String[] hostList = config.getDbProperty(SyncConstants.HOST).split(",");
		String pwd = getPassword();
		if (pwd != null && !pwd.isEmpty()) {
			credential = MongoCredential.createCredential(username, dbname, pwd.toCharArray());
			credList = Arrays.asList(credential);
		}

		if (hostList != null && hostList.length > 0) {
			addressList = new ArrayList<ServerAddress>();
			for (String hostName : hostList) {
				addressList.add(new ServerAddress(hostName, port));
			}
		}

		if (credList != null) {
			mongoDataBase = new MongoClient(addressList, credList, options).getDatabase(dbname);
		} else {
			mongoDataBase = new MongoClient(addressList, options).getDatabase(dbname);
		}
	}
}
