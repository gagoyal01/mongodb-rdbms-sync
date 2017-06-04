package com.cisco.app.dbmigrator.migratorapp.logging.codecs;

import java.util.UUID;

import org.apache.log4j.Logger;
import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncUserSession;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncUserSession.SessionAttributes;

public class SyncUserSessionCodec implements CollectibleCodec<SyncUserSession> {
	private Codec<Document> documentCodec;
	Logger logger =Logger.getLogger(this.getClass());
	public SyncUserSessionCodec(Codec<Document> documentCodec) {
		super();
		this.documentCodec = documentCodec;
	}
	@Override
	public void encode(BsonWriter writer, SyncUserSession session, EncoderContext encoderContext) {
		Document document = new Document();
		if(session.getSessionId()!=null && !session.getSessionId().isEmpty() ){
			document.append(String.valueOf(SessionAttributes._id), session.getSessionId());	
		}
		if(session.getUserid()!=null && !session.getUserid().isEmpty()){
			document.append(String.valueOf(SessionAttributes.userid), session.getUserid());
		}
		if(session.getLoginTime()!=null){
			document.append(String.valueOf(SessionAttributes.loginTime), session.getLoginTime());
		}
		if(session.getClientIPAddress()!=null && !session.getClientIPAddress().isEmpty()){
			document.append(String.valueOf(SessionAttributes.clientIPAddress), session.getClientIPAddress());
		}
		if(session.getClientHostName()!=null && !session.getClientHostName().isEmpty()){
			document.append(String.valueOf(SessionAttributes.clientHostName), session.getClientHostName());
		}
		if(session.getClientAgent()!=null && !session.getClientAgent().isEmpty()){
			document.append(String.valueOf(SessionAttributes.clientAgent), session.getClientAgent());
		}
		documentCodec.encode(writer, document, encoderContext);
	}

	@Override
	public Class<SyncUserSession> getEncoderClass() {
		return SyncUserSession.class;
	}

	@Override
	public SyncUserSession decode(BsonReader reader, DecoderContext decoderContext) {
		SyncUserSession sessionData= new SyncUserSession();
		Document document = documentCodec.decode(reader, decoderContext);
		sessionData.setSessionId(document.getString(String.valueOf(SessionAttributes._id)));
		sessionData.setLoginTime(document.getDate(String.valueOf(SessionAttributes.loginTime)));
		sessionData.setUserid(document.getString(String.valueOf(SessionAttributes.userid)));
		sessionData.setClientIPAddress(document.getString(String.valueOf(SessionAttributes.clientIPAddress)));
		sessionData.setClientAgent(document.getString(String.valueOf(SessionAttributes.clientAgent)));
		sessionData.setClientHostName(document.getString(String.valueOf(SessionAttributes.clientHostName)));
		return sessionData;
	}

	@Override
	public boolean documentHasId(SyncUserSession session) {
		return session.getSessionId()==null;
	}
	@Override
	public SyncUserSession generateIdIfAbsentFromDocument(SyncUserSession session) {
		if (!documentHasId(session)) {
			session.setSessionId((UUID.randomUUID().toString()));
		}
		return session;
	}
	@Override
	public BsonValue getDocumentId(SyncUserSession session) {
		return new BsonString(session.getSessionId());
	}
}
