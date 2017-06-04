package com.cisco.app.dbmigrator.migratorapp.logging.codecs;

import org.bson.BsonObjectId;
import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.core.map.MongoToOracleMap;

public class MngToOrclMapCodec implements CollectibleCodec<MongoToOracleMap> {

	private Codec<Document> documentCodec;
	
	
	public MngToOrclMapCodec(Codec<Document> documentCodec) {
		super();
		this.documentCodec = documentCodec;
	}

	@Override
	public void encode(BsonWriter writer, MongoToOracleMap map, EncoderContext encoderContext) {
		SyncMapAndEventEncoder encoder = new SyncMapAndEventEncoder();
		Document document = encoder.encodeSyncMap(map);
		documentCodec.encode(writer, document, encoderContext);
	}

	@Override
	public Class<MongoToOracleMap> getEncoderClass() {
		return MongoToOracleMap.class;
	}

	@Override
	public MongoToOracleMap decode(BsonReader reader, DecoderContext decoderContext) {
		Document document = documentCodec.decode(reader, decoderContext);
		SyncMapAndEventDecoder decoder = new SyncMapAndEventDecoder();
		return (MongoToOracleMap) decoder.decodeSyncMap(document);
	}

	@Override
	public boolean documentHasId(MongoToOracleMap map) {
		return map.getMapId() != null;
	}

	@Override
	public MongoToOracleMap generateIdIfAbsentFromDocument(MongoToOracleMap map) {
		if (!documentHasId(map)) {
			map.setMapId(new ObjectId());
		}
		return map;
	}

	@Override
	public BsonValue getDocumentId(MongoToOracleMap map) {
		return new BsonObjectId(map.getMapId());
	}

}
