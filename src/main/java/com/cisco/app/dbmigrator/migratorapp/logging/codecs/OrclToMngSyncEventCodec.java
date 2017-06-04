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

import com.cisco.app.dbmigrator.migratorapp.core.event.OracleToMongoSyncEvent;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncEvent;

public class OrclToMngSyncEventCodec implements CollectibleCodec<OracleToMongoSyncEvent> {
	private Codec<Document> documentCodec;

	public OrclToMngSyncEventCodec(Codec<Document> documentCodec) {
		super();
		this.documentCodec = documentCodec;
	}

	@Override
	public void encode(BsonWriter writer, OracleToMongoSyncEvent event, EncoderContext encoderContext) {
		SyncMapAndEventEncoder encoder = new SyncMapAndEventEncoder();		
		Document document =encoder.encodeSyncEvent(event);		
		documentCodec.encode(writer, document, encoderContext);
	}

	@Override
	public Class<OracleToMongoSyncEvent> getEncoderClass() {
		return OracleToMongoSyncEvent.class;
	}

	@Override
	public OracleToMongoSyncEvent decode(BsonReader reader, DecoderContext decoderContext) {
		Document document = documentCodec.decode(reader, decoderContext);
		SyncMapAndEventDecoder decoder = new SyncMapAndEventDecoder();
		@SuppressWarnings("rawtypes")
		SyncEvent event = decoder.decodeSyncEvent(document);
		return (OracleToMongoSyncEvent) event;
	}

	@Override
	public boolean documentHasId(OracleToMongoSyncEvent event) {
		return event.getEventId() != null;
	}

	@Override
	public OracleToMongoSyncEvent generateIdIfAbsentFromDocument(OracleToMongoSyncEvent event) {
		if (!documentHasId(event)) {
			event.setEventId(new ObjectId());
		}
		return event;
	}

	@Override
	public BsonValue getDocumentId(OracleToMongoSyncEvent event) {
		return new BsonObjectId(event.getEventId());
	}

}
