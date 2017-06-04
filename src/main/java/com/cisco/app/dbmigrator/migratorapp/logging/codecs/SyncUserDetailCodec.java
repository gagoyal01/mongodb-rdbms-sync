package com.cisco.app.dbmigrator.migratorapp.logging.codecs;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncUser;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncUser.UserDetailAttributes;

public class SyncUserDetailCodec implements CollectibleCodec<SyncUser> {
	private Codec<Document> documentCodec;
	Logger logger =Logger.getLogger(this.getClass().getSimpleName());
	public SyncUserDetailCodec(Codec<Document> documentCodec) {
		super();
		this.documentCodec = documentCodec;
	}
	@Override
	public void encode(BsonWriter writer, SyncUser user, EncoderContext encoderContext) {
		logger.info("Start of Encode method");
		Document document = new Document();
		if (user.getUserid()!=null && !user.getUserid().isEmpty()){
			document.append(String.valueOf(UserDetailAttributes._id), user.getUserid());
		}		
		if(user.getSourceDbMap()!=null && !user.getSourceDbMap().isEmpty()){
			document.append(String.valueOf(UserDetailAttributes.sourceDbMap), user.getSourceDbMap());
		}
		if(user.getTargetDbMap()!=null && !user.getTargetDbMap().isEmpty()){
			document.append(String.valueOf(UserDetailAttributes.targetDbMap), user.getTargetDbMap());
		}
		if(user.getUserRoles()!=null && !user.getUserRoles().isEmpty()){
			document.append("roles", user.getUserRoles());
		}
		documentCodec.encode(writer, document, encoderContext);
		logger.info("Encoder completed. Document formed is \n"+document);
	}
	@Override
	public Class<SyncUser> getEncoderClass() {
		return SyncUser.class;
	}
	@SuppressWarnings("unchecked")
	@Override
	public SyncUser decode(BsonReader reader, DecoderContext decoderContext) {
		logger.info("Start of decode method");
		SyncUser user = new SyncUser();
		Document document = documentCodec.decode(reader, decoderContext);
		user.setUserid(document.getString(String.valueOf(UserDetailAttributes._id)));
		user.setSourceDbMap((Map<String, Set<String>>) document.get(String.valueOf(UserDetailAttributes.sourceDbMap)));
		user.setTargetDbMap((Map<String, Set<String>>) document.get(String.valueOf(UserDetailAttributes.targetDbMap)));
		user.setUserRoles((List<String>) document.get("roles"));
		logger.info("Decode completed. Object formed is "+user.toString());
		return user;
	}
	@Override
	public boolean documentHasId(SyncUser user) {
		return user.getUserid()==null;
	}
	@Override
	public SyncUser generateIdIfAbsentFromDocument(SyncUser user) {
		if (!documentHasId(user)) {
			user.setUserid((UUID.randomUUID().toString()));
		}
		return user;
	}
	@Override
	public BsonValue getDocumentId(SyncUser user) {
		return new BsonString(user.getUserid());
	}
}
