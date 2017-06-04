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

import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncNode;


public class SyncNodeCodec implements CollectibleCodec<SyncNode> {
	
	public Codec<Document> documentCodec;
	public SyncNodeCodec(Codec<Document> documentCodec) {
		super();
		this.documentCodec = documentCodec;
	}
	@Override
	public void encode(BsonWriter writer, SyncNode nodeMapper, EncoderContext encoderContext) {
		Document document = SyncNodeEncoder.encodeNode(nodeMapper);  	
		documentCodec.encode(writer, document, encoderContext);		
	}
	@Override
	public Class<SyncNode> getEncoderClass() {
		return SyncNode.class;
	}
	@Override
	public SyncNode decode(BsonReader reader, DecoderContext decoderContext) {
		Document document = documentCodec.decode(reader, decoderContext);
		SyncNode nodeMapper = SyncNodeDecoder.decodeNode(document);
		return nodeMapper;
	}
	@Override
	public boolean documentHasId(SyncNode nodeMapper) {
		return nodeMapper.getId()== null;
	}
	@Override
	public SyncNode generateIdIfAbsentFromDocument(SyncNode nodeMapper) {
		return nodeMapper;
	}
	@Override
	public BsonValue getDocumentId(SyncNode nodeMapper) {
		return new BsonObjectId(nodeMapper.getId());
	}
}