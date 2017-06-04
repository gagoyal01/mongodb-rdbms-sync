package com.cisco.app.dbmigrator.migratorapp.core.thread;

import java.util.concurrent.BlockingQueue;

import org.bson.Document;

import com.cisco.app.dbmigrator.migratorapp.utilities.cache.DBCacheManager;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;

public class MngToOrclReader implements Runnable {

	private BlockingQueue<Document> dataBuffer;
	private final String mongoDbName;
	private final String mongoUserName;
	private final String collectionName;
	private final int batchSize;

	public MngToOrclReader(BlockingQueue<Document> dataBuffer, String mongoDbName, String mongoUserName,
			String collectionName, int batchSize) {
		super();
		this.dataBuffer = dataBuffer;
		this.mongoDbName = mongoDbName;
		this.mongoUserName = mongoUserName;
		this.collectionName = collectionName;
		this.batchSize = batchSize;
	}

	@Override
	public void run() {
		System.out.println("Reader started");
		MongoCollection<Document> collection = DBCacheManager.INSTANCE.getCachedMongoPool(mongoDbName, mongoUserName)
				.getDatabase(mongoDbName).getCollection(collectionName);
		FindIterable<Document> it = collection.find().batchSize(batchSize);
		it.forEach(new Block<Document>() {
			@Override
			public void apply(Document t) {
				System.out.println("Document read " + t);
				try {
					dataBuffer.put(t);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}
}