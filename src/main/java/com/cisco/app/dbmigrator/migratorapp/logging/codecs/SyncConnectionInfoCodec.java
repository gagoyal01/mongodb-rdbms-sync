package com.cisco.app.dbmigrator.migratorapp.logging.codecs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncConnectionInfo;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncConnectionInfo.ConnectionInfoAttributes;

public class SyncConnectionInfoCodec implements CollectibleCodec<SyncConnectionInfo> {

	private Codec<Document> documentCodec;
	Logger logger =Logger.getLogger(this.getClass());
	public SyncConnectionInfoCodec(Codec<Document> documentCodec) {
		super();
		this.documentCodec = documentCodec;
	}
	
	@Override
	public void encode(BsonWriter writer, SyncConnectionInfo connectionInfo, EncoderContext encoderContext) {

		Document document = new Document();
		if(null!=connectionInfo.getConnectionId()){
			document.append(String.valueOf(ConnectionInfoAttributes._id), connectionInfo.getConnectionId());
		}
		if(null!=connectionInfo.getConnectionName() &&!connectionInfo.getConnectionName().isEmpty()){
			document.append(String.valueOf(ConnectionInfoAttributes.connectionName), connectionInfo.getConnectionName());
		}
		if(null!=connectionInfo.getDbType()){
			document.append(String.valueOf(ConnectionInfoAttributes.dbType), connectionInfo.getDbType());
		}
		if(null!=connectionInfo.getDbName()){
			document.append(String.valueOf(ConnectionInfoAttributes.dbName), connectionInfo.getDbName());
		}
		if(null!=connectionInfo.getUserName()){
			document.append(String.valueOf(ConnectionInfoAttributes.userName), connectionInfo.getUserName());
		}
		if(null!=connectionInfo.getUrl()){
			document.append(String.valueOf(ConnectionInfoAttributes.url), connectionInfo.getUrl());
		}
		/*if(null!=connectionInfo.getPassword()){
			try {
				document.append(String.valueOf(ConnectionInfoAttributes.password), EncryptorDecryptor.encrypt(connectionInfo.getPassword()));
			} catch (Exception e) {
				e.printStackTrace();
			}
			//document.append(String.valueOf(ConnectionInfoAttributes.password), connectionInfo.getPassword());
		}*/
		document.append(String.valueOf(ConnectionInfoAttributes.password), connectionInfo.getPassword());
		if (connectionInfo.getHostToPortMap()!=null && connectionInfo.getHostToPortMap().size()>0){
			List<Document> hostToPortDocList= new ArrayList<Document>();
			for(Map.Entry<String,Double> hostToPort : connectionInfo.getHostToPortMap().entrySet()){
				Document hostToPortDoc = new Document();
				hostToPortDoc.append(String.valueOf(ConnectionInfoAttributes.host), hostToPort.getKey());
				hostToPortDoc.append(String.valueOf(ConnectionInfoAttributes.port), hostToPort.getValue());
				hostToPortDocList.add(hostToPortDoc);
			}
			document.append(String.valueOf(ConnectionInfoAttributes.hostToPortMap), hostToPortDocList);
		}
		logger.info("Encoder finished. Created document is "+document);
		documentCodec.encode(writer, document, encoderContext);
	}

	@Override
	public Class<SyncConnectionInfo> getEncoderClass() {
		return SyncConnectionInfo.class;
	}

	@Override
	public SyncConnectionInfo decode(BsonReader reader, DecoderContext decoderContext) {
        SyncConnectionInfo con = new SyncConnectionInfo();
        Map<String, Double> hostToPortMap=null;
        Document document = documentCodec.decode(reader, decoderContext);
        con.setConnectionId(document.getObjectId(String.valueOf(ConnectionInfoAttributes._id)));
        con.setConnectionName(document.getString(String.valueOf(ConnectionInfoAttributes.connectionName)));
        con.setDbName(document.getString(String.valueOf(ConnectionInfoAttributes.dbName)));
        con.setDbType(document.getString(String.valueOf(ConnectionInfoAttributes.dbType)));
       /* try {
        	if(document.get(String.valueOf(ConnectionInfoAttributes.password))!=null){
        		//if(!(document.get(String.valueOf(ConnectionInfoAttributes.password)) instanceof String))
            		con.setPassword(EncryptorDecryptor.decrypt(((Binary)document.get(String.valueOf(ConnectionInfoAttributes.password))).getData()));
            	else 
            		con.setPassword(document.getString(String.valueOf(ConnectionInfoAttributes.password)));
        	}
		} catch (Exception e) {
			e.printStackTrace();
		}*/
        con.setPassword(document.getString(String.valueOf(ConnectionInfoAttributes.password)));
        con.setUserName(document.getString(String.valueOf(ConnectionInfoAttributes.userName)));
        con.setUrl(document.getString(String.valueOf(ConnectionInfoAttributes.url)));
		@SuppressWarnings("unchecked")
		List<Document> hostToPortMapDoc = (List<Document>) document.get(String.valueOf(ConnectionInfoAttributes.hostToPortMap));
		if( hostToPortMapDoc !=null && !hostToPortMapDoc.isEmpty()){
			hostToPortMap = new HashMap<String, Double>();
			for(Document doc : hostToPortMapDoc){
				hostToPortMap.put(doc.getString(String.valueOf(ConnectionInfoAttributes.host)), doc.getDouble(String.valueOf(ConnectionInfoAttributes.port)));
			}
			con.setHostToPortMap(hostToPortMap);
		}
		logger.info("Decoder finished. Created Object is : "+con);
		return con;
	}

	@Override
	public boolean documentHasId(SyncConnectionInfo connectionInfo) {
		return connectionInfo.getConnectionId()==null;
	}

	@Override
	public SyncConnectionInfo generateIdIfAbsentFromDocument(SyncConnectionInfo connectionInfo) {
		if (!documentHasId(connectionInfo)) {
			connectionInfo.setConnectionId(new ObjectId(UUID.randomUUID().toString()));
		}
		return connectionInfo;
	}

	@Override
	public BsonValue getDocumentId(SyncConnectionInfo connectionInfo) {
		return new BsonString(String.valueOf(connectionInfo.getConnectionId()));
	}

}
