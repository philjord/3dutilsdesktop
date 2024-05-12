package esm.analyzer;

import java.util.ArrayList;

public class RecordData
{
	public String type = "";
	
	public int formId = -1;

	// sub records in order
	public ArrayList<SubrecordData> subrecordStatsList = new ArrayList<SubrecordData>();

	public RecordData(String t, int id)
	{
		this.type = t;
		this.formId = id;		 
	}
}
