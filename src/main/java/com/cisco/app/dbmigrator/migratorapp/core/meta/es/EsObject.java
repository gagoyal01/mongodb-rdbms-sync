package com.cisco.app.dbmigrator.migratorapp.core.meta.es;

import java.util.List;

public class EsObject implements EsEntity {
	private String esObjectName;
	private List<EsMngAttrMapper> esMngAttrMap;
	private List<MappedCollectionInfo> mappedCollectionInfoList;
	public String getEsObjectName() {
		return esObjectName;
	}

	public void setEsObjectName(String esObjectName) {
		this.esObjectName = esObjectName;
	}

	public List<MappedCollectionInfo> getMappedCollectionInfoList() {
		return mappedCollectionInfoList;
	}

	public void setMappedCollectionInfoList(List<MappedCollectionInfo> mappedCollectionInfoList) {
		this.mappedCollectionInfoList = mappedCollectionInfoList;
	}

	public List<EsMngAttrMapper> getEsMngAttrMap() {
		return esMngAttrMap;
	}

	public void setEsMngAttrMap(List<EsMngAttrMapper> esMngAttrMap) {
		this.esMngAttrMap = esMngAttrMap;
	}

	@Override
	public String getEntityType() {
		return String.valueOf(EsEntityTypes.EsObject);
	}
}