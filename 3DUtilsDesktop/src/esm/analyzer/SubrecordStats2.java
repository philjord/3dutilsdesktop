package esm.analyzer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import esfilemanager.common.data.record.Subrecord;

public class SubrecordStats2
{
	public String subrecordType;
	
	public String C;
	
	public String dataType;
	
	public String desc = "";
	
	

	public int count = 0;

	public int minLength = Integer.MAX_VALUE;

	public int maxLength = Integer.MIN_VALUE;
	
	public int minSubPos = Integer.MAX_VALUE;

	public int maxSubPos = Integer.MIN_VALUE;
	
	
	public HashSet<Integer> hasOrderOf = new HashSet<Integer>();	

	public HashSet<String> appearsIn = new HashSet<String>();

	public HashSet<Integer> fewSizes = new HashSet<Integer>();	
	
	public HashSet<String> subTypesBefore = new HashSet<String>();
	
	public HashSet<String> subTypesAfter = new HashSet<String>();
	
	// count of formId pointer types
	public HashMap<String, Integer> formTypeCounts = new HashMap<String, Integer>();
	
	public int countOfString = 0;
	public int countOfNif = 0;
	public int countOfDds = 0;
	public int countOfKf = 0;
	
	public int countOfInts = 0;
	public int countOfFloats = 0;
	public int countOfVec3 = 0;
	public int countOfVec4 = 0;

	public SubrecordStats2(String st)
	{
		this.subrecordType = st;
	}
	public void applySub(SubrecordData srd) {
 
		hasOrderOf.addAll(srd.hasOrderOf);
		subTypesBefore.addAll(srd.subTypesBefore);
		subTypesAfter.addAll(srd.subTypesAfter);
		
		count += srd.count;
		
		// need to add up the formtype counts
		for (Entry<String, Integer> entry : srd.formTypeCounts.entrySet()) {
			Integer current = formTypeCounts.get(entry.getKey());
			current = current == null ? 0 : current;
			formTypeCounts.put(entry.getKey(), current + entry.getValue());
		}
		
		for(int ord : srd.hasOrderOf) {
			if(ord < minSubPos)
				minSubPos = ord;
			
			if(ord > maxSubPos)
				maxSubPos = ord;
		}
		
		fewSizes.addAll(srd.fewSizes);

		countOfString += srd.countOfString;
		countOfNif += srd.countOfNif;
		countOfDds += srd.countOfDds;
		
		countOfInts += srd.countOfInts;
		countOfFloats += srd.countOfFloats;
		countOfVec3 += srd.countOfVec3;
		countOfVec4 += srd.countOfVec4;
		if (minLength > srd.minLength)
		{
			minLength = srd.minLength;
		}

		if (maxLength < srd.maxLength)
		{
			maxLength = srd.maxLength;
		}
		
	}
	public void applySub(Subrecord sub, String inRec, int orderNo, String subTypeBefore, String subTypeAfter, String couldBeFormID, String couldBeString)
	{
	
	}
	
	@Override
	public boolean equals(Object o) {
		if(o!=null && o instanceof SubrecordStats2 && ((SubrecordStats2)o).subrecordType.equals(this.subrecordType))
			return true;
		return false;
	}

	
}
