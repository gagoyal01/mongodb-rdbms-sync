package com.cisco.app.dbmigrator.migratorapp.logging.codecs;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import com.cisco.app.dbmigrator.migratorapp.core.event.SyncMarker;

public class SyncMarkerCodec implements Codec<SyncMarker> {
	private Codec<Document> documentCodec;
	public SyncMarkerCodec(Codec<Document> documentCodec) {
		super();
		this.documentCodec = documentCodec;
	}
	@Override
	public void encode(BsonWriter writer, SyncMarker marker, EncoderContext encoderContext) {
		Document document = new Document();
		if(marker.getStartTime()!=null){
			document.append(SyncAttrs.START_TIME, marker.getStartTime());
		}
		document.append(SyncAttrs.ROWS_READ, marker.getRowsRead());
		if(marker.isAllRowsFetchedFromDb()){
			document.append(SyncAttrs.ALL_ROWS_FETCHED, marker.isAllRowsFetchedFromDb());
		}
		document.append(SyncAttrs.ROWS_DUMPED, marker.getRowsDumped());
		document.append(SyncAttrs.TOTAL_ROWS, marker.getTotalRows());
		if(marker.getEndTime()!=null){
			document.append(SyncAttrs.END_TIME, marker.getEndTime());
		}	
		documentCodec.encode(writer, document, encoderContext);
	}

	@Override
	public Class<SyncMarker> getEncoderClass() {
		return SyncMarker.class;
	}

	@Override
	public SyncMarker decode(BsonReader reader, DecoderContext decoderContext) {
		Document document = documentCodec.decode(reader, decoderContext);
		SyncMarker marker = new SyncMarker();
		marker.setAllRowsFetchedFromDb(document.getBoolean(SyncAttrs.ALL_ROWS_FETCHED, false));
		marker.setTotalRows(document.getInteger(SyncAttrs.TOTAL_ROWS, -1));
		marker.setRowsDumped(document.getInteger(SyncAttrs.ROWS_DUMPED, 0));
		marker.setRowsRead(document.getInteger(SyncAttrs.ROWS_READ, 0));
		marker.setStartTime(document.getDate(SyncAttrs.START_TIME));
		marker.setEndTime(document.getDate(SyncAttrs.END_TIME));
		return marker;
	}
}
