package esm.analyzer.old;

import java.util.HashSet;
import java.util.regex.Pattern;

import esfilemanager.common.data.record.Subrecord;
import tools.io.ESMByteConvert;

public class SubrecordStats
{
	public String subrecordType;

	public int count = 0;

	public int minLength = Integer.MAX_VALUE;

	public int maxLength = Integer.MIN_VALUE;

	public boolean isString = true; // any false leaves it as false only all trues leave it

	public boolean couldBeFormId = true;
	
	public HashSet<Integer> hasOrderOf = new HashSet<Integer>();	

	public HashSet<String> appearsIn = new HashSet<String>();

	public HashSet<Integer> fewSizes = new HashSet<Integer>();	
	
	public HashSet<String> subTypesBefore = new HashSet<String>();
	
	public HashSet<String> subTypesAfter = new HashSet<String>();

	public SubrecordStats(String st)
	{
		this.subrecordType = st;
	}

	public void applySub(Subrecord sub, String inRec, int orderNo, String subTypeBefore, String subTypeAfter)
	{
		appearsIn.add(inRec);
		hasOrderOf.add(orderNo);
		subTypesBefore.add(subTypeBefore);
		subTypesAfter.add(subTypeAfter);
		
		
		
		count++;
		byte[] bs = sub.getSubrecordData();
		
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

		if (bs.length == 4)
		{
			int possFormId = ESMByteConvert.extractInt(bs, 0);
			if (possFormId < 0 || possFormId > EsmFormatAnalyzer.maxFormId)
			{
				couldBeFormId = false;
			}
		}
		else
		{
			couldBeFormId = false;
		}

		//oblivion has lots of massive DESC recos and FULLs don't show either
		if (bs.length > 0 && bs[bs.length - 1] == 0 )//&& bs.length < 2048)
		{
			// only update is string if it is not yet false
			if (isString)
			{
				String s = new String(bs, 0, bs.length - 1);
				isString = Pattern.matches("[^\\p{C}[\\s]]*", s);
				//if (!Pattern.matches("[\\p{Graph}\\p{Space}]+.", str))
			}
		}
		else
		{
			isString = false;
		}

	}
}
