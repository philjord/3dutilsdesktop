package esm.analyzer;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;

import esfilemanager.common.data.plugin.FormInfo;
import esfilemanager.common.data.record.Subrecord;
import tools.io.ESMByteConvert;

public class SubrecordData
{
	public String subrecordType;

	public int count = 0;

	public int minLength = Integer.MAX_VALUE;

	public int maxLength = Integer.MIN_VALUE;
	
	public HashSet<Integer> hasOrderOf = new HashSet<Integer>();	

	public HashSet<Integer> fewSizes = new HashSet<Integer>();	
	
	public HashSet<String> subTypesBefore = new HashSet<String>();
	
	public HashSet<String> subTypesAfter = new HashSet<String>();
	
	// count of formId pointer types
	public HashMap<String, Integer> formTypeCounts = new HashMap<String, Integer>();
	
	public int countOfString = 0;
	public int countOfNif = 0;
	public int countOfDds = 0;
	
	public int countOfInts = 0;
	public int countOfFloats = 0;

	public SubrecordData(Subrecord sub, String inRec, int orderNo, String subTypeBefore, String subTypeAfter, String couldBeFormID, String couldBeString)
	{		
		subrecordType = sub.getSubrecordType();
		hasOrderOf.add(orderNo);
		subTypesBefore.add(subTypeBefore);
		subTypesAfter.add(subTypeAfter);
		
		count++;
		
		if(couldBeFormID != null) {
			String formIdType = couldBeFormID.substring(0,couldBeFormID.indexOf(":"));
	 		if(formIdType.length() == 4) {
	 			if(formTypeCounts.get(formIdType) == null) {
	 				formTypeCounts.put(formIdType, 0);
	 			} 	
	 			formTypeCounts.put(formIdType, formTypeCounts.get(formIdType) + 1);
	 		}
		}
		if (couldBeString != null) {
			countOfString += couldBeString.length() > 0 ? 1 : 0;
			countOfNif += couldBeString.toLowerCase().endsWith(".nif") ? 1 : 0;
			countOfDds += couldBeString.toLowerCase().endsWith(".dds") ? 1 : 0;
		}
		
		byte[] bs = sub.getSubrecordData();
		
		checkSimpleType(bs);
		
		// after 5 size variations its not worth trying to pin it down, presumably strings and randos
		if(fewSizes.size() < 6)
			fewSizes.add(bs.length);

		
		if (minLength > bs.length)
		{
			minLength = bs.length;
		}

		if (maxLength < bs.length)
		{
			maxLength = bs.length;
		}
	}
	
	public void checkSimpleType(byte[] bs) {
		if (bs.length == 4) {

			int possI = ESMByteConvert.extractInt(bs, 0);
			BigDecimal possF = new BigDecimal(ESMByteConvert.extractFloat(bs, 0));
			if(possI>-1000&& possI<10000)
				countOfInts++;
			if(possF.scale()<4&&possF.scale()>-4)
				countOfFloats++;
		}  
		
	  
	}
	@Override
	public boolean equals(Object o) {
		if(o!=null && o instanceof SubrecordData && ((SubrecordData)o).subrecordType.equals(this.subrecordType))
			return true;
		return false;
	}
}
