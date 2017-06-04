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

import com.cisco.app.dbmigrator.migratorapp.core.event.MongoToOracleEvent;

public class MngToOrclEventCodec implements CollectibleCodec<MongoToOracleEvent> {
	private Codec<Document> documentCodec;

	public MngToOrclEventCodec(Codec<Document> documentCodec) {
		super();
		this.documentCodec = documentCodec;
	}

	@Override
	public void encode(BsonWriter writer, MongoToOracleEvent event, EncoderContext encoderContext) {
		SyncMapAndEventEncoder encoder = new SyncMapAndEventEncoder();		
		Document document =encoder.encodeSyncEvent(event);		
		documentCodec.encode(writer, document, encoderContext);
	}

	@Override
	public Class<MongoToOracleEvent> getEncoderClass() {
		return MongoToOracleEvent.class;
	}

	@Override
	public MongoToOracleEvent decode(BsonReader reader, DecoderContext decoderContext) {
		Document document = documentCodec.decode(reader, decoderContext);
		SyncMapAndEventDecoder decoder = new SyncMapAndEventDecoder();
		MongoToOracleEvent event = (MongoToOracleEvent) decoder.decodeSyncEvent(document);
		return event;
	}

	@Override
	public boolean documentHasId(MongoToOracleEvent event) {
		return event.getEventId() != null;
	}

	@Override
	public MongoToOracleEvent generateIdIfAbsentFromDocument(MongoToOracleEvent event) {
		if (!documentHasId(event)) {
			event.setEventId(new ObjectId());
		}
		return event;
	}

	@Override
	public BsonValue getDocumentId(MongoToOracleEvent event) {
		return new BsonObjectId(event.getEventId());
	}

}
