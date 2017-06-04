package com.cisco.app.dbmigrator.migratorapp.logging.codecs;

import java.util.ArrayList;
import java.util.List;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncEvent;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncNode;
import com.cisco.app.dbmigrator.migratorapp.logging.entity.SyncNodeDetails;

public class SyncNodeDetailsCodec implements Codec<SyncNodeDetails> {

	private Codec<Document> documentCodec;

	public SyncNodeDetailsCodec(Codec<Document> documentCodec) {
		super();
		this.documentCodec = documentCodec;
	}

	@Override
	public void encode(BsonWriter bsonWriter, SyncNodeDetails nodeAndEvent, EncoderContext context) {
		Document document = new Document();
		if (nodeAndEvent.getNode() != null) {
			document.append("node", nodeAndEvent.getNode());
		}
		if (nodeAndEvent.getEvent() != null) {
			document.append("events", nodeAndEvent.getEvent());
		}
		documentCodec.encode(bsonWriter, document, context);
	}

	@Override
	public Class<SyncNodeDetails> getEncoderClass() {
		return SyncNodeDetails.class;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public SyncNodeDetails decode(BsonReader bsonReader, DecoderContext context) {
		Document document = documentCodec.decode(bsonReader, context);
		SyncNodeDetails nodeAndEvent = new SyncNodeDetails();
		SyncNode node = SyncNodeDecoder.decodeNode((Document) document.get("node"));
		nodeAndEvent.setNode(node);
		SyncEvent event = null;
		List<SyncEvent> eventList = new ArrayList<SyncEvent>();
		SyncMapAndEventDecoder eventDecoder = new SyncMapAndEventDecoder();
		@SuppressWarnings("unchecked")
		List<Document> list = (List<Document>) document.get("events");
		for(Document doc : list){ 
			event = eventDecoder.decodeSyncEvent(doc);
			eventList.add(event); 
		} 
		nodeAndEvent.setEvent(eventList);
		return nodeAndEvent;
	}
}
