package com.cisco.app.dbmigrator.migratorapp.logging.codecs;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

import com.cisco.app.dbmigrator.migratorapp.logging.entities.O2MSyncEventLog;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.O2MSyncEventLog.O2MSyncEventInfo;

public class O2MSyncEventLogCodec implements CollectibleCodec<O2MSyncEventLog> {

	public static final String _ID = "_id";
	public static final String EVENT_ID = "eventId";
	public static final String OPERATION = "op";
	public static final String CREATED_ON = "crOn";
	public static final String SYNCED_ON = "syOn";
	public static final String STATUS = "status";
	public static final String EVENT_FILTERS = "filters";
	public static final String ERRORS = "errors";
	public static final String TABLE_NAME = "tname";
	public static final String COLUMN_NAME = "cname";
	public static final String COLUMN_VALUE = "cval";
	public static final String PENDING = "P";
	public static final String RUNNING = "R";
	public static final String COMPLETE = "C";
	private Codec<Document> documentCodec;

	public O2MSyncEventLogCodec(Codec<Document> documentCodec) {
		super();
		this.documentCodec = documentCodec;
	}
	
	@Override
	public void encode(BsonWriter writer, O2MSyncEventLog log, EncoderContext encoderContext) {
		Document document = new Document();
		if(log.getLogId()!=null){
			document.append(_ID, log.getLogId());
		}
		document.append(EVENT_ID, log.getEventId());
		document.append(CREATED_ON, log.getCrOn()==null?new Date():log.getCrOn());
		document.append(SYNCED_ON, log.getSyOn());
		document.append(STATUS, log.getStatus()==null?PENDING:log.getStatus());
		document.append(OPERATION, log.getOperation());
		if(log.getEventFilters()!=null){
			List<Document> filterDocList = new ArrayList<Document>(log.getEventFilters().size());
			Document filterDoc=null;
			for(O2MSyncEventInfo filter : log.getEventFilters()){
				filterDoc= new Document();
				filterDoc.append(TABLE_NAME, filter.getTableName());
				filterDoc.append(COLUMN_NAME, filter.getColumnName());
				filterDoc.append(COLUMN_VALUE, filter.getColumnValue());
				filterDocList.add(filterDoc);
			}
			document.append(EVENT_FILTERS, filterDocList);
		}
		documentCodec.encode(writer, document, encoderContext);
	}

	@Override
	public Class<O2MSyncEventLog> getEncoderClass() {
		return O2MSyncEventLog.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public O2MSyncEventLog decode(BsonReader arg0, DecoderContext arg1) {
		Document document = documentCodec.decode(arg0, arg1);
		O2MSyncEventLog log = new O2MSyncEventLog();
		log.setLogId(document.getObjectId(_ID));
		log.setEventId(document.getString(EVENT_ID));
		log.setCrOn(document.getDate(CREATED_ON));
		log.setSyOn(document.getDate(SYNCED_ON));
		log.setStatus(document.getString(STATUS));
		log.setOperation(document.getString(OPERATION));
		List<Document> filterDocList = (List<Document>) document.get(EVENT_FILTERS);
		if(filterDocList!=null){
			List<O2MSyncEventInfo> filters = new ArrayList<O2MSyncEventInfo>(filterDocList.size());
			O2MSyncEventInfo filter = null;
			for(Document filterDoc : filterDocList){
				filter= new O2MSyncEventInfo();
				filter.setTableName(filterDoc.getString(TABLE_NAME));
				filter.setColumnName(filterDoc.getString(COLUMN_NAME));
				filter.setColumnValue(filterDoc.get(COLUMN_VALUE));
				filters.add(filter);
			}
			log.setEventFilters(filters);
		}
		return log;
	}

	@Override
	public boolean documentHasId(O2MSyncEventLog arg0) {
		if(arg0.getLogId()!=null){
			return true;
		}
		return false;
	}

	@Override
	public O2MSyncEventLog generateIdIfAbsentFromDocument(O2MSyncEventLog arg0) {
		if(!documentHasId(arg0)){
			arg0.setLogId(new ObjectId());
		}
		return arg0;
	}

	@Override
	public BsonValue getDocumentId(O2MSyncEventLog arg0) {
		return new BsonObjectId(arg0.getLogId());
	}

}
