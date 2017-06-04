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

import com.cisco.app.dbmigrator.migratorapp.core.event.OracleToMongoGridFsEvent;

public class SyncEventGridFsCodec implements CollectibleCodec<OracleToMongoGridFsEvent> {

	private Codec<Document> documentCodec;

	public SyncEventGridFsCodec(Codec<Document> documentCodec) {
		super();
		this.documentCodec = documentCodec;
	}
	
	@Override
	public void encode(BsonWriter writer, OracleToMongoGridFsEvent event, EncoderContext encoderContext) {
		SyncMapAndEventGridFsEncoder encoder = new SyncMapAndEventGridFsEncoder();		
		Document document =encoder.encodeSyncEvent(event);		
		documentCodec.encode(writer, document, encoderContext);
	}

	@Override
	public Class<OracleToMongoGridFsEvent> getEncoderClass() {
		return OracleToMongoGridFsEvent.class;
	}

	@Override
	public OracleToMongoGridFsEvent decode(BsonReader reader, DecoderContext decoderContext) {
		Document document = documentCodec.decode(reader, decoderContext);
		SyncMapAndEventGridFsDecoder decoder = new SyncMapAndEventGridFsDecoder();
		OracleToMongoGridFsEvent event = decoder.decodeSyncEvent(document);
		return event;
	}

	@Override
	public boolean documentHasId(OracleToMongoGridFsEvent event) {
		return event.getEventId() != null;
	}

	@Override
	public OracleToMongoGridFsEvent generateIdIfAbsentFromDocument(OracleToMongoGridFsEvent event) {
		if (!documentHasId(event)) {
			event.setEventId(new ObjectId());
		}
		return event;
	}

	@Override
	public BsonValue getDocumentId(OracleToMongoGridFsEvent event) {
		return new BsonObjectId(event.getEventId());
	}

}
