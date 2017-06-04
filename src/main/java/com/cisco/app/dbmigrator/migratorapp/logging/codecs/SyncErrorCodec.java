package com.cisco.app.dbmigrator.migratorapp.logging.codecs;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import com.cisco.app.dbmigrator.migratorapp.core.event.SyncError;

public class SyncErrorCodec implements Codec<SyncError> {
	private Codec<Document> documentCodec;

	public SyncErrorCodec(Codec<Document> documentCodec) {
		super();
		this.documentCodec = documentCodec;
	}
	@Override
	public void encode(BsonWriter writer, SyncError error, EncoderContext encoderContext) {
		Document document= new Document();
		if(error.getMessage()!=null && !error.getMessage().isEmpty()){
			document.append(SyncAttrs.ERROR_MESSAGE, error.getMessage());
		}
		
		String fullStackTrace = ExceptionUtils.getStackTrace(error);
		if(fullStackTrace!=null && !fullStackTrace.isEmpty()){
			document.append(SyncAttrs.TRACE, fullStackTrace);
		}
		if(error.getThreadName()!=null && !error.getThreadName().isEmpty()){
			document.append(SyncAttrs.THREAD_NAME, error.getThreadName());
		}
		documentCodec.encode(writer, document, encoderContext);
	}

	@Override
	public Class<SyncError> getEncoderClass() {
		return SyncError.class;
	}

	@Override
	public SyncError decode(BsonReader reader, DecoderContext decoderContext) {
		Document document = documentCodec.decode(reader, decoderContext);		
		SyncError error = new SyncError(document.getString(SyncAttrs.ERROR_MESSAGE));	
		error.setThreadName(document.getString(SyncAttrs.THREAD_NAME));
		error.setFullStackTrace(document.getString(SyncAttrs.TRACE));
		return error;
	}

}
