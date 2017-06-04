package com.cisco.app.dbmigrator.migratorapp.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import com.cisco.app.dbmigrator.migratorapp.core.map.ColumnAttrMapper;
import com.cisco.app.dbmigrator.migratorapp.core.map.MapType;
import com.cisco.app.dbmigrator.migratorapp.core.map.MongoToOracleMap;
import com.cisco.app.dbmigrator.migratorapp.core.map.OracleToMongoMap;
import com.cisco.app.dbmigrator.migratorapp.core.map.SyncMap;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoAttribute;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoEntity;
import com.cisco.app.dbmigrator.migratorapp.core.meta.mongo.MongoObject;
import com.cisco.app.dbmigrator.migratorapp.core.meta.oracle.NodeGroup;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncAttrs;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncMapAndEventDecoder;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncMapDao;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.JoinedTable;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleColumn;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.OracleTable;
import com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities.QueryConstants;

public class SyncMapServiceImpl implements SyncMapService {
	private SyncMapDao mapDao;
	private SyncMapAndEventDecoder decoder;

	public SyncMapDao getMapDao() {
		return mapDao;
	}

	public void setMapDao(SyncMapDao mapDao) {
		this.mapDao = mapDao;
	}

	public SyncMapAndEventDecoder getDecoder() {
		return decoder;
	}

	public void setDecoder(SyncMapAndEventDecoder decoder) {
		this.decoder = decoder;
	}

	@Override
	public void deleteMapping(String mappingId) {
		if (mappingId == null) {
			return;
		}
		mapDao.deleteMapping(new ObjectId(mappingId));
	}

	@Override
	public void saveMappings(String jsonString) {
		SyncMap map = decoder.decodeSyncMap(jsonString);
		mapDao.saveMapping(map);
	}

	@Override
	public SyncMap loadMapping(String mappingId) {
		return mapDao.getMapping(new ObjectId(mappingId));
	}

	@Override
	public List<SyncMap> getAllMappings() {
		return mapDao.getAllMapping();
	}

	@Override
	public String getMappingTables(String mappingId) {
		String mappingTablesStr = "";
		Set<OracleTable> tableSet = new HashSet<OracleTable>();
		JSONObject jsonObject = new JSONObject();
		OracleToMongoMap map = (OracleToMongoMap) loadMapping(mappingId);
		String sourceDb = map.getSourceDbName();
		String sourceShema = map.getSourceUserName();
		jsonObject.put(SyncAttrs.SOURCE_DB_NAME, sourceDb);
		jsonObject.put(SyncAttrs.SOURCE_USER_NAME, sourceShema);
		jsonObject.put(SyncAttrs.COLLECTION_NAME, map.getMapObject().getCollectionName());
		List<OracleTable> oracleTablesList = map.getMapObject().getSourceTables();
		tableSet = getAllTableList(oracleTablesList.get(0));
		JSONArray tableJsonArray = new JSONArray();
		for (OracleTable table : tableSet) {
			JSONObject tableObj = new JSONObject();
			tableObj.put(SyncAttrs.TABLE_NAME, table.getTableName());
			tableObj.put(SyncAttrs.TABLE_ALIAS, table.getTableAlias());
			tableJsonArray.put(tableObj);
		}
		jsonObject.put(SyncAttrs.TABLE_LIST, tableJsonArray);
		JSONArray attributeArray = new JSONArray();
		List<MongoEntity> attrList = map.getMapObject().getAttributes();
		if (map.getMapObject().getIdentifierList() != null && !map.getMapObject().getIdentifierList().isEmpty())
			attrList.addAll(map.getMapObject().getIdentifierList());
		for (MongoEntity entity : attrList) {
			if (entity instanceof MongoAttribute) {
				JSONObject attr = new JSONObject(entity);
				attributeArray.put(attr);
			}
		}
		jsonObject.put(SyncAttrs.ATTRIBUTES, attributeArray);
		mappingTablesStr = jsonObject.toString();
		return mappingTablesStr;
	}

	private Set<OracleTable> getAllTableList(OracleTable table) {
		Set<OracleTable> tableList = new HashSet<OracleTable>();
		tableList.add(table);
		if (table.getJoinedTables() != null && !table.getJoinedTables().isEmpty()) {
			for (JoinedTable joinedTable : table.getJoinedTables()) {
				tableList.addAll(getAllTableList(joinedTable.getTable()));
			}
		}
		return tableList;
	}

	@Override
	public List<SyncMap> getMappingByMapType(String mapType) {
		return mapDao.getMappingByMapType(mapType);
	}

	@Override
	public MongoToOracleMap translateMap(String mappingId) {
		MongoToOracleMap map = new MongoToOracleMap();
		SyncMap syncMap = mapDao.getMapping(new ObjectId(mappingId));
		if (syncMap != null) {
			OracleToMongoMap mongoMap = (OracleToMongoMap) syncMap;
			MongoObject mongoObject = mongoMap.getMapObject();
			List<NodeGroup> nodeGrpList = Arrays.asList(translateMongoObject(mongoObject));
			map.setRootNode(nodeGrpList);
			map.setCollectionName(mongoObject.getCollectionName());
			map.setMapType(String.valueOf(MapType.MongoToOrcl));
		}
		return map;
	}

	private NodeGroup translateMongoObject(MongoObject mongoObject) {
		//List<NodeGroup> nodeGrpList = new ArrayList<NodeGroup>();
		List<OracleTable> sourceTables = mongoObject.getSourceTables();
		NodeGroup nodeGroup = new NodeGroup();
		Set<OracleTable> tableList = new HashSet<OracleTable>();
		Map<String, ColumnAttrMapper> attrMapperMap = new HashMap<String, ColumnAttrMapper>();
		ColumnAttrMapper attrMapper = null;
		if (sourceTables != null && !sourceTables.isEmpty()) {
			tableList = getAllTableList(sourceTables.get(0));
			List<OracleTable> tablist = new ArrayList<OracleTable>(tableList);
			nodeGroup.setTableList(tablist);
			for (MongoEntity attribute : mongoObject.getAttributes()) {
				if (attribute instanceof MongoAttribute) {
					MongoAttribute mongoAttribute = (MongoAttribute) attribute;
					attrMapper = translateMongoAttribute(mongoAttribute);
					if(attrMapper.getColumn() !=null)
						attrMapperMap.put(attrMapper.getColumn().getColumnAlias(), attrMapper);
					nodeGroup.setColumnAttrMappers(attrMapperMap);
				} else if (attribute instanceof MongoObject) {
					MongoObject mongoNestedObject = (MongoObject) attribute;
					nodeGroup.addChildGroup(translateMongoObject(mongoNestedObject));
				}
			}
			if (mongoObject.getIdentifierList() != null && !mongoObject.getIdentifierList().isEmpty()) {
				if (mongoObject.getIdentifierList().size() == 1) {
					MongoAttribute mongoAttribute = (MongoAttribute) mongoObject.getIdentifierList().get(0);
					mongoAttribute.setAttributeName(SyncAttrs.ID);
					mongoAttribute.setIdentifier(true);
					attrMapper = translateMongoAttribute(mongoAttribute);
					attrMapperMap.put(attrMapper.getColumn().getColumnAlias(), attrMapper);
					nodeGroup.setColumnAttrMappers(attrMapperMap);
				} else {
					for (MongoEntity attribute : mongoObject.getIdentifierList()) {
						if (attribute instanceof MongoAttribute) {
							MongoAttribute mongoAttribute = (MongoAttribute) attribute;
							mongoAttribute.setAttributeName(
									SyncAttrs.ID + QueryConstants.DOT + mongoAttribute.getAttributeName());
							mongoAttribute.setIdentifier(true);
							attrMapper = translateMongoAttribute(mongoAttribute);
							attrMapperMap.put(attrMapper.getColumn().getColumnAlias(), attrMapper);
							nodeGroup.setColumnAttrMappers(attrMapperMap);
						} else if (attribute instanceof MongoObject) {
							MongoObject mongoNestedObject = (MongoObject) attribute;
							nodeGroup.addChildGroup(translateMongoObject(mongoNestedObject));
						}
					}
				}
			}
		}else{
			for (MongoEntity attribute : mongoObject.getAttributes()) {
				if (attribute instanceof MongoObject) {
					MongoObject mongoNestedObject = (MongoObject) attribute;
					nodeGroup.addChildGroup(translateMongoObject(mongoNestedObject));
				}
			}
		}
		nodeGroup.setNodeName(mongoObject.getCollectionName());
		/*
		 * if(parentNodeName !=null && !parentNodeName.isEmpty()){
		 * nodeGroup.setNodeName(mongoObject.getCollectionName()); }else{
		 * nodeGroup.setNodeName(mongoObject.getCollectionName()); }
		 */

		//nodeGrpList.add(nodeGroup);
		return nodeGroup;

	}

	private ColumnAttrMapper translateMongoAttribute(MongoAttribute mongoAttribute) {
		OracleColumn column = new OracleColumn();
		ColumnAttrMapper attrMapper = new ColumnAttrMapper();
		if (mongoAttribute.getMappedOracleColumn() != null) {
			column = mongoAttribute.getMappedOracleColumn();
			attrMapper.setColumnData(column);
		}
		attrMapper.setAttribute(mongoAttribute);
		return attrMapper;
	}
}
