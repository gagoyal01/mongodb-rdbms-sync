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

import com.cisco.app.dbmigrator.migratorapp.core.event.MongoToOracleSyncEvent;

public class MngToOrclSyncEventCodec implements CollectibleCodec<MongoToOracleSyncEvent> {
	private Codec<Document> documentCodec;

	public MngToOrclSyncEventCodec(Codec<Document> documentCodec) {
		super();
		this.documentCodec = documentCodec;
	}

	@Override
	public void encode(BsonWriter writer, MongoToOracleSyncEvent event, EncoderContext encoderContext) {
		SyncMapAndEventEncoder encoder = new SyncMapAndEventEncoder();		
		Document document =encoder.encodeSyncEvent(event);		
		documentCodec.encode(writer, document, encoderContext);
	}

	@Override
	public Class<MongoToOracleSyncEvent> getEncoderClass() {
		return MongoToOracleSyncEvent.class;
	}

	@Override
	public MongoToOracleSyncEvent decode(BsonReader reader, DecoderContext decoderContext) {
		Document document = documentCodec.decode(reader, decoderContext);
		SyncMapAndEventDecoder decoder = new SyncMapAndEventDecoder();
		MongoToOracleSyncEvent event = (MongoToOracleSyncEvent) decoder.decodeSyncEvent(document);
		return event;
	}

	@Override
	public boolean documentHasId(MongoToOracleSyncEvent event) {
		return event.getEventId() != null;
	}

	@Override
	public MongoToOracleSyncEvent generateIdIfAbsentFromDocument(MongoToOracleSyncEvent event) {
		if (!documentHasId(event)) {
			event.setEventId(new ObjectId());
		}
		return event;
	}

	@Override
	public BsonValue getDocumentId(MongoToOracleSyncEvent event) {
		return new BsonObjectId(event.getEventId());
	}

}
