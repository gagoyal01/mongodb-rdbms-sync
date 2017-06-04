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

import com.cisco.app.dbmigrator.migratorapp.core.event.SyncEvent;

@SuppressWarnings("rawtypes")
public class SyncEventCodec implements CollectibleCodec<SyncEvent> {
	private Codec<Document> documentCodec;

	public SyncEventCodec(Codec<Document> documentCodec) {
		super();
		this.documentCodec = documentCodec;
	}

	@Override
	public void encode(BsonWriter writer, SyncEvent event, EncoderContext encoderContext) {
		SyncMapAndEventEncoder encoder = new SyncMapAndEventEncoder();		
		Document document =encoder.encodeSyncEvent(event);		
		documentCodec.encode(writer, document, encoderContext);
	}

	@Override
	public Class<SyncEvent> getEncoderClass() {
		return SyncEvent.class;
	}

	@Override
	public SyncEvent decode(BsonReader reader, DecoderContext decoderContext) {
		Document document = documentCodec.decode(reader, decoderContext);
		SyncMapAndEventDecoder decoder = new SyncMapAndEventDecoder();
		SyncEvent event = decoder.decodeSyncEvent(document);
		return event;
	}

	@Override
	public boolean documentHasId(SyncEvent event) {
		return event.getEventId() != null;
	}

	@Override
	public SyncEvent generateIdIfAbsentFromDocument(SyncEvent event) {
		if (!documentHasId(event)) {
			event.setEventId(new ObjectId());
		}
		return event;
	}

	@Override
	public BsonValue getDocumentId(SyncEvent event) {
		return new BsonObjectId(event.getEventId());
	}

}
