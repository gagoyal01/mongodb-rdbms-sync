package com.cisco.app.dbmigrator.migratorapp.core.map;

import java.util.List;

import org.bson.BsonDocument;
import org.bson.BsonDocumentWrapper;
import org.bson.codecs.configuration.CodecRegistry;

import com.cisco.app.dbmigrator.migratorapp.core.meta.oracle.NodeGroup;

public class MongoToOracleMap extends SyncMap{
	//private Map<String , NodeGroup> nodeGroupMap;
	private List<NodeGroup> rootNode;
	private String collectionName;

	/*public Map<String, NodeGroup> getNodeGroupMap() {
		return nodeGroupMap;
	}
	public void setNodeGroupMap(Map<String, NodeGroup> nodeGroupMap) {
		this.nodeGroupMap = nodeGroupMap;
	}*/
	public String getCollectionName() {
		return collectionName;
	}
	/**
	 * @return the rootNode
	 */
	public List<NodeGroup> getRootNode() {
		return rootNode;
	}
	/**
	 * @param rootNode the rootNode to set
	 */
	public void setRootNode(List<NodeGroup> rootNode) {
		this.rootNode = rootNode;
	}
	public void setCollectionName(String collectionName) {		
		this.collectionName = collectionName;
	}
	@Override
	public <TDocument> BsonDocument toBsonDocument(Class<TDocument> arg0, CodecRegistry codecRegistry) {
		return new BsonDocumentWrapper<MongoToOracleMap>(this, codecRegistry.get(MongoToOracleMap.class));
	}
}
