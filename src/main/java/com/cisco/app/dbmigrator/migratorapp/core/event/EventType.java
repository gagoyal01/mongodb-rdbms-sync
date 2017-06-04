/**
 * 
 */
package com.cisco.app.dbmigrator.migratorapp.core.event;

/**
 * Enum to Hold supported typed of Events
 * 
 * @author pnilayam
 *
 */
public enum EventType {
	OrclToMongo,MongoToOrcl,MongoToOrclSync,OrclToMongoSync,System,OrclToMongoGridFs;
}
