package com.cisco.app.dbmigrator.migratorapp.core.event.range;

import java.util.ArrayList;
import java.util.List;

public class IntRangeDivisor extends RangeDivisor<Integer> {
	
	public IntRangeDivisor(int numOfBuckets, Integer minValue, Integer maxValue) {
		super(numOfBuckets, minValue, maxValue);
	}

	@Override
	public List<DataRange> generateRangeList() {
		List<DataRange> rangeList= new ArrayList<DataRange>();
		//int diff = maxValue-minValue;
		//int factor = diff/numOfBuckets;
		int currentMin=minValue;
		/*for(int i=0;i<numOfBuckets-1;i++){
			DataRange range = new DataRange();
			range.setLowerLimit(currentMin);
			range.setUpperLimit(currentMin+factor);
			currentMin=currentMin+factor;
			range.setLowerLimitInclusive(true);
			rangeList.add(range);
		}
		DataRange range = new DataRange();
		range.setLowerLimit(currentMin);
		range.setUpperLimit(maxValue);
		range.setLowerLimitInclusive(true);
		range.setUpperLimitInclusive(true);
		rangeList.add(range);
		*/
		int offset = 20000;
		while(currentMin<maxValue){
			DataRange range = new DataRange();
			range.setLowerLimit(currentMin);
			range.setUpperLimit(currentMin+offset);
			currentMin=currentMin+offset;
			range.setLowerLimitInclusive(true);
			rangeList.add(range);
		}
		
		return rangeList;
	}
}
