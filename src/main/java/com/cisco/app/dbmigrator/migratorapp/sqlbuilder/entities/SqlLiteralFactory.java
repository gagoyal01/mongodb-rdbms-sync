package com.cisco.app.dbmigrator.migratorapp.sqlbuilder.entities;

import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.bson.types.Binary;

import com.cisco.app.dbmigrator.migratorapp.constants.SyncConstants;

import oracle.sql.ROWID;
import oracle.sql.TIMESTAMP;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class SqlLiteralFactory {	
	private SqlLiteralFactory(){}
	public static Literal getLiteral(Object value, String dataType) {
		Literal literal = null;
		if (value == null || SyncConstants.EMPTY_STRING.equalsIgnoreCase(String.valueOf(value))) {
			literal = new NullLiteral();

		} else {
			if (SqlColumnType.VARCHAR2.equalsIgnoreCase(dataType)) {
				literal = new VarcharLiteral();
				value = String.valueOf(value);
			} else if (SqlColumnType.NUMBER.equalsIgnoreCase(dataType)) {
				literal = new BigDecimalLiteral();
				value = new BigDecimal(String.valueOf(value),MathContext.DECIMAL128);
			} else if (SqlColumnType.DATE.equalsIgnoreCase(dataType) || SqlColumnType.TIMESTAMP.equals(dataType)) {
				literal = new OracleTimeStampLiteral();
				try {
					if (value instanceof String) {
						value = new SimpleDateFormat("yyyy-MM-dd").parse(String.valueOf(value));
					}
					Date dateVal = (Date) value;
					value = new TIMESTAMP(new Timestamp(dateVal.getTime()), Calendar.getInstance(TimeZone.getTimeZone("PST")));
				} catch (ParseException e) {
					e.printStackTrace();
				}
			} else if (SqlColumnType.ROWID.equalsIgnoreCase(dataType)){
				if(value instanceof String){
					value = new ROWID(String.valueOf(value).getBytes());
				}
				literal = new RowIdLiteral();
			}
			else {
				literal = new VarcharLiteral();
				value = String.valueOf(value);
			}
			literal.setLiteralValue(value);
		}
		return literal;
	}
	
	public static Literal getLobLiteral(Object inputValue, String dataType, Connection connection) throws SQLException{
		Literal literal = null;
		if(connection==null){
			literal = new NullLiteral();
		}else{
			if(SqlColumnType.BLOB.equalsIgnoreCase(dataType)){
				literal= new BlobLiteral();
				Blob blob = connection.createBlob();
				Binary objArray = (Binary) inputValue;
				blob.setBytes(1, objArray.getData());
				literal.setLiteralValue(blob);
			}else if(SqlColumnType.CLOB.equalsIgnoreCase(dataType)){
				literal = new ClobLiteral();
				Clob clob = connection.createClob();
				clob.setString(1, String.valueOf(inputValue));
				literal.setLiteralValue(clob);
			}
		}
		return literal;
	}
}
