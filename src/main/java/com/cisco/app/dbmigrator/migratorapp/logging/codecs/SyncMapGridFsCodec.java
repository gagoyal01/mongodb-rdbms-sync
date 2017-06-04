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

import com.cisco.app.dbmigrator.migratorapp.core.map.OracleToMongoGridFsMap;

public class SyncMapGridFsCodec implements CollectibleCodec<OracleToMongoGridFsMap> {

	private Codec<Document> documentCodec;

	public SyncMapGridFsCodec(Codec<Document> documentCodec) {
		super();
		this.documentCodec = documentCodec;
	}
	
	@Override
	public void encode(BsonWriter writer, OracleToMongoGridFsMap map, EncoderContext encoderContext) {
		SyncMapAndEventGridFsEncoder encoder = new SyncMapAndEventGridFsEncoder();
		Document document= encoder.encodeSyncMap(map);
		documentCodec.encode(writer, document, encoderContext);
	}

	@Override
	public Class<OracleToMongoGridFsMap> getEncoderClass() {
		return OracleToMongoGridFsMap.class;
	}

	@Override
	public OracleToMongoGridFsMap decode(BsonReader reader, DecoderContext decoderContext) {
		Document document = documentCodec.decode(reader, decoderContext);
		SyncMapAndEventGridFsDecoder decoder = new SyncMapAndEventGridFsDecoder();
		return decoder.decodeSyncMap(document);
	}

	@Override
	public boolean documentHasId(OracleToMongoGridFsMap map) {
		return map.getMapId() != null;
	}

	@Override
	public OracleToMongoGridFsMap generateIdIfAbsentFromDocument(OracleToMongoGridFsMap map) {
		if (!documentHasId(map)) {
			map.setMapId(new ObjectId());
		}
		return map;
	}

	@Override
	public BsonValue getDocumentId(OracleToMongoGridFsMap map) {
		return new BsonObjectId(map.getMapId());
	}

}
