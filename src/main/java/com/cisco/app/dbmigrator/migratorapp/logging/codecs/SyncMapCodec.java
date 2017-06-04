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

import com.cisco.app.dbmigrator.migratorapp.core.map.SyncMap;

public class SyncMapCodec implements CollectibleCodec<SyncMap> {
	private Codec<Document> documentCodec;

	public SyncMapCodec(Codec<Document> documentCodec) {
		super();
		this.documentCodec = documentCodec;
	}

	@Override
	public void encode(BsonWriter writer, SyncMap map, EncoderContext encoderContext) {
		SyncMapAndEventEncoder encoder = new SyncMapAndEventEncoder();
		Document document= encoder.encodeSyncMap(map);
		documentCodec.encode(writer, document, encoderContext);
	}

	@Override
	public Class<SyncMap> getEncoderClass() {
		return SyncMap.class;
	}

	@Override
	public SyncMap decode(BsonReader reader, DecoderContext decoderContext) {
		Document document = documentCodec.decode(reader, decoderContext);
		SyncMapAndEventDecoder decoder = new SyncMapAndEventDecoder();
		return decoder.decodeSyncMap(document);
	}

	@Override
	public boolean documentHasId(SyncMap map) {
		return map.getMapId() != null;
	}

	@Override
	public SyncMap generateIdIfAbsentFromDocument(SyncMap map) {
		if (!documentHasId(map)) {
			map.setMapId(new ObjectId());
		}
		return map;
	}

	@Override
	public BsonValue getDocumentId(SyncMap map) {
		return new BsonObjectId(map.getMapId());
	}

}
