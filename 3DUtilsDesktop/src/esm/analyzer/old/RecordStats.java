package esm.analyzer.old;

import java.util.List;

import esfilemanager.common.data.record.Record;
import esfilemanager.common.data.record.Subrecord;

public class RecordStats
{
	public String type = "";

	public boolean appearsInExtCELL = false;

	public boolean appearsInIntCELL = false;

	public int count = 0;

	public SubrecordStatsList subrecordStatsList = new SubrecordStatsList();

	public RecordStats(String t)
	{
		this.type = t;
	}
	// if subs are recorded sepeately
	public void applyRecord(Record rec)
	{
		count++;
	}
	
	public void applyRecord(Record rec, boolean interior, boolean exterior)
	{
		 applyRecord(rec, interior, exterior, null);
	}
	public void applyRecord(Record rec, boolean interior, boolean exterior, SubrecordStatsList allSubrecordStatsList)
	{
		appearsInIntCELL = appearsInIntCELL || interior;
		appearsInExtCELL = appearsInExtCELL || exterior;
		count++;

		List<Subrecord> subs = rec.getSubrecords();
		
		
		
		for (int i = 0; i < subs.size(); i++)
		{
			Subrecord sub = subs.get(i);
			String subTypeBefore = null;
			String subTypeAfter = null;
			if(i> 0)
				subTypeBefore = subs.get(i-1).getSubrecordType();
			
			if(i>subs.size()-1)
				subTypeAfter = subs.get(i+1).getSubrecordType();

			subrecordStatsList.applySub(sub, rec.getRecordType(), i, subTypeBefore, subTypeAfter);
			
 
			
			
			// also put into the global sub stats list
			if(allSubrecordStatsList != null)
				allSubrecordStatsList.applySub(sub, rec.getRecordType(), i, subTypeBefore, subTypeAfter);
		}

		EsmFormatAnalyzer.loadRecord(rec);
	}
}
