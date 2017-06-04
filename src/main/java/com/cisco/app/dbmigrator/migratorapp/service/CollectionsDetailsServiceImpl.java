package com.cisco.app.dbmigrator.migratorapp.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoAttribute;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoEntity;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoObject;
import com.cisco.app.dbmigrator.migratorapp.utilities.cache.DBCacheManager;
import com.mongodb.MongoClient;

public class CollectionsDetailsServiceImpl implements CollectionsDetailsService{
	@Override
	public List<String> getAllCollections(String sourceDbName, String sourceSchemaName) {
		List<String> collectionNameList = new ArrayList<String>();
		MongoClient mongoClient = DBCacheManager.INSTANCE.getCachedMongoPool(sourceDbName, sourceSchemaName);
		return mongoClient.getDatabase(sourceDbName).listCollectionNames().into(collectionNameList);
	}

	@Override
	public List<String> getAttributesForCollection(String sourceDbName, String sourceSchemaName,
			String collectionName) {
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private MongoObject processDocument(Document document,String collectionName,String collectionType) {
		boolean type = false;
		MongoObject mongoObject = new MongoObject();
		Document attDoc = new Document();
		try{
			if(!document.entrySet().isEmpty()){
				MongoEntity entity = null;
				List<Entry> entryList = new ArrayList<Entry>(document.entrySet());
				mongoObject.setCollectionName(collectionName);
				mongoObject.setCollectionType(collectionType);
				for(Entry entry : entryList){
					type = getAttributeType(entry.getValue().getClass());
					if(!type && entry.getValue() instanceof List){
						List<Document> docList = (List<Document>) document.get(entry.getKey());
						if(!docList.isEmpty() && docList.size()>0){
							for(int i=0;i<docList.size();i++){
								Document doc = docList.get(i);
								Set<Map.Entry<String, Object>> arrayEntry = doc.entrySet();
								Iterator<Map.Entry<String, Object>> it = arrayEntry.iterator();
								while (it.hasNext()) {
									Map.Entry<String, Object> details =  it.next();
									if(!attDoc.containsKey(details.getKey())){
										if(details.getValue() instanceof Document || details.getValue() instanceof List){
											attDoc.put(details.getKey(), details.getValue());
										}else{
											attDoc.put(details.getKey(), details.getValue());
										}
									}
									
								}
							}
							entity = processDocument(attDoc, entry.getKey().toString(), entry.getValue().getClass().getSimpleName());
							attDoc = new Document();
						}else{
							// TODO for Empty array List
							
						}
					}else if(!type && entry.getValue() instanceof Document){
						Document doc = (Document) document.get(entry.getKey());
						entity = processDocument(doc,entry.getKey().toString(),entry.getValue().getClass().getSimpleName());
					}else{
						entity= processMongoAttribute(entry.getKey().toString(), entry.getValue().getClass().getSimpleName().toUpperCase());
					}
					mongoObject.addEntity(entity);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return mongoObject;
	}

	public static boolean getAttributeType(Class<?> type) {
		return (type.isPrimitive() && type != void.class) ||
	        type == Double.class || type == Float.class || type == Long.class ||
	        type == Integer.class || type == Short.class || type == Character.class ||
	        type == Byte.class || type == Boolean.class || type == String.class 
	        || type == ObjectId.class || type == Date.class;
	}
	
	private MongoAttribute processMongoAttribute(String name, String type) {
		MongoAttribute attribute = new MongoAttribute();
		attribute.setAttributeName(name);
		attribute.setAttributeType(type);
		return attribute;
	}

	@Override
	public MongoObject processCollection(String sourceDbName, String sourceSchemaName, String collectionName) {
		MongoClient mongoClient = DBCacheManager.INSTANCE.getCachedMongoPool(sourceDbName, sourceSchemaName);
		Document document = mongoClient.getDatabase(sourceDbName).getCollection(collectionName).find().first();
		MongoObject mongoObject = processDocument(document,collectionName,"Collection"/*,mongoObjects*/);
		return mongoObject;
	}
}
