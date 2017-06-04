package com.cisco.app.dbmigrator.migratorapp.core.meta.mongo;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class MongoLiteralFactory {
	private static final Logger logger = Logger.getLogger(MongoLiteralFactory.class);

	@SuppressWarnings("unused")
	private static enum SqlTypes {
		BIT(-7), TINYINT(-6), SMALLINT(5), INTEGER(4), BIGINT(-5), FLOAT(6), REAL(7), DOUBLE(8), NUMERIC(2), DECIMAL(
				3), CHAR(1), VARCHAR(12), LONGVARCHAR(-1), DATE(91), TIME(92), TIMESTAMP(93), BINARY(-2), VARBINARY(
						-3), LONGVARBINARY(-4), NULL(0), OTHER(1111), JAVA_OBJECT(
								2000), DISTINCT(2001), STRUCT(2002), ARRAY(2003), BLOB(2004), CLOB(2005);
		private int type;

		/**
		 * @return the type
		 */
		public int getType() {
			return type;
		}

		private SqlTypes(int type) {
			this.type = type;
		}
	}

	private MongoLiteralFactory() {
	}

	public static Object getMongoLiteral(ResultSet row, String columnAlias, String attributeType) throws SQLException {
		if (row == null) {
			return null;
		}
		Object value = null;
		if (String.valueOf(MongoAttributeType.STRING).equalsIgnoreCase(attributeType)) {
			value = row.getString(columnAlias);
		} else if (String.valueOf(MongoAttributeType.DOUBLE).equalsIgnoreCase(attributeType)
				|| String.valueOf(MongoAttributeType.NUMBER).equalsIgnoreCase(attributeType)) {
			/**
			 * Retreiving in String to handle special case in which for NUMBER
			 * columns for Oracle , save 0 only when its 0 in oracle else do not
			 * consider for insertion in mongodb
			 */
			String numericString = row.getString(columnAlias);
			if (numericString != null && !numericString.isEmpty()) {
				double numericValue = row.getDouble(columnAlias);
				value = numericValue;
			}
		} else if (String.valueOf(MongoAttributeType.INTEGER).equalsIgnoreCase(attributeType)) {
			String numericString = row.getString(columnAlias);
			if (numericString != null && !numericString.isEmpty()) {
				long numericValue = row.getLong(columnAlias);
				value = numericValue;
			}
		} else if (String.valueOf(MongoAttributeType.DATE).equalsIgnoreCase(attributeType)) {
			java.sql.Timestamp date = row.getTimestamp(columnAlias);
			if (date != null) {
				value = new Date(date.getTime());
			}
		} else if (String.valueOf(MongoAttributeType.B_ARRAY).equalsIgnoreCase(attributeType)) {
			Blob blob = row.getBlob(columnAlias);
			if (blob != null) {
				InputStream stream = blob.getBinaryStream();
				try {
					byte[] bArray = IOUtils.toByteArray(stream);
					value = bArray;
				} catch (IOException e) {
					logger.error("Error while create B_ARRAY for Blob", e);
				}
			}
		} else {
			value = row.getString(columnAlias);
		}
		return value;
	}

	@SuppressWarnings("unused")
	private static Date getDate(Object inputvalue, int columnType) {
		Date date = null;
		switch (columnType) {
		case Types.DATE:
			java.sql.Date sqlDate = (java.sql.Date) inputvalue;
			date = new Date(sqlDate.getTime());
			break;
		case Types.TIMESTAMP:
			Timestamp ts = (Timestamp) inputvalue;
			date = new Date(ts.getTime());
			break;
		case Types.TIME:
			Time time = (Time) inputvalue;
			date = new Date(time.getTime());
			break;
		default:
			return null;
		}
		return date;
	}

	/*
	 * private String getString(Object inputValue, int columnType){ String
	 * string=null; switch (columnType){ case Types.VARCHAR: string=(String)
	 * inputValue; break; case Types.CLOB: CLOB clob = (CLOB) inputValue;
	 * string=clob.stringValue(); clob.f } return string; }
	 */

	public static Object getMongoLiteral(String attributeType, Object inputValue, String columnType) {
		if (inputValue == null) {
			return null;
		}
		Object returnValue = null;
		if (String.valueOf(MongoAttributeType.STRING).equalsIgnoreCase(attributeType)) {
			returnValue = String.valueOf(inputValue);
		} else if (String.valueOf(MongoAttributeType.DOUBLE).equalsIgnoreCase(attributeType)
				|| String.valueOf(MongoAttributeType.NUMBER).equalsIgnoreCase(attributeType)) {
			double numericValue = Double.valueOf(String.valueOf(inputValue));
			if (numericValue != 0) {
				returnValue = numericValue;
			}
		} else if (String.valueOf(MongoAttributeType.INTEGER).equalsIgnoreCase(attributeType)) {
			int numericValue = Integer.valueOf(String.valueOf(inputValue));
			if (numericValue != 0) {
				returnValue = numericValue;
			}
		} else if (String.valueOf(MongoAttributeType.DATE).equalsIgnoreCase(attributeType)) {
			// int sqlType = SqlTypes.valueOf(columnType).getType();
			returnValue = inputValue;
		} else if (String.valueOf(MongoAttributeType.B_ARRAY).equalsIgnoreCase(attributeType)) {
			try {
				byte[] bArray = IOUtils.toByteArray((InputStream) inputValue);
				returnValue = bArray;
			} catch (IOException e) {
				logger.error("Error while create B_ARRAY for Blob", e);
			}
		} else {
			returnValue = String.valueOf(inputValue);
		}
		return returnValue;
	}
}
