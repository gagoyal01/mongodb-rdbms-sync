package com.cisco.app.dbmigrator.migratorapp.core.event.range;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DateRangeDivisor extends RangeDivisor<Date> {

	public DateRangeDivisor(int numOfBuckets, Date minValue, Date maxValue) {
		super(numOfBuckets, minValue, maxValue);
	}

	@Override
	public List<DataRange> generateRangeList() {
		List<DataRange> rangeList= new ArrayList<DataRange>();
		long diff= maxValue.getTime()-minValue.getTime();
		long factor = diff/numOfBuckets;
		long currentMin=maxValue.getTime();
		for(int i=0;i<numOfBuckets-1;i++){
			DataRange range = new DataRange();
			range.setLowerLimit(new Date(currentMin));
			range.setUpperLimit(new Date(currentMin+factor));
			currentMin=currentMin+factor;
			range.setLowerLimitInclusive(true);
			rangeList.add(range);
		}
		DataRange range = new DataRange();
		range.setLowerLimit(new Date(currentMin));
		range.setUpperLimit(new Date(maxValue.getTime()));
		range.setLowerLimitInclusive(true);
		range.setUpperLimitInclusive(true);
		rangeList.add(range);
		return rangeList;
	}

}
