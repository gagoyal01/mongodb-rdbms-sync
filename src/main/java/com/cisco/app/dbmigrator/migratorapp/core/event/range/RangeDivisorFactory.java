package com.cisco.app.dbmigrator.migratorapp.core.event.range;

import java.util.Date;

public class RangeDivisorFactory {
	@SuppressWarnings("rawtypes")
	public static RangeDivisor getRangeDivisor(Object minValue, Object maxValue , int numOfBuckets, String columnType) {
		RangeDivisor rangeDivisor=null;
		if("NUMBER".equalsIgnoreCase(columnType)){
			rangeDivisor=new IntRangeDivisor(numOfBuckets, (Integer)minValue, (Integer)maxValue);
		}else if ("DATE".equalsIgnoreCase(columnType)) {
			rangeDivisor=new DateRangeDivisor(numOfBuckets, (Date)minValue, (Date)maxValue);
		}
		return rangeDivisor;
	}
}
