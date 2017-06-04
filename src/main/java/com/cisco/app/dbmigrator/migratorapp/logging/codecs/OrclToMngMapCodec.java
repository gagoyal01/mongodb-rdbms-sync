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

import com.cisco.app.dbmigrator.migratorapp.core.map.OracleToMongoMap;

public class OrclToMngMapCodec implements CollectibleCodec<OracleToMongoMap> {

	private Codec<Document> documentCodec;

	public OrclToMngMapCodec(Codec<Document> documentCodec) {
		super();
		this.documentCodec = documentCodec;
	}

	@Override
	public void encode(BsonWriter writer, OracleToMongoMap map, EncoderContext encoderContext) {
		SyncMapAndEventEncoder encoder = new SyncMapAndEventEncoder();
		Document document = encoder.encodeSyncMap(map);
		documentCodec.encode(writer, document, encoderContext);
	}

	@Override
	public Class<OracleToMongoMap> getEncoderClass() {
		return OracleToMongoMap.class;
	}

	@Override
	public OracleToMongoMap decode(BsonReader reader, DecoderContext decoderContext) {
		Document document = documentCodec.decode(reader, decoderContext);
		SyncMapAndEventDecoder decoder = new SyncMapAndEventDecoder();
		return (OracleToMongoMap) decoder.decodeSyncMap(document);
	}

	@Override
	public boolean documentHasId(OracleToMongoMap map) {
		return map.getMapId() != null;
	}

	@Override
	public OracleToMongoMap generateIdIfAbsentFromDocument(OracleToMongoMap map) {
		if (!documentHasId(map)) {
			map.setMapId(new ObjectId());
		}
		return map;
	}

	@Override
	public BsonValue getDocumentId(OracleToMongoMap map) {
		return new BsonObjectId(map.getMapId());
	}

}
