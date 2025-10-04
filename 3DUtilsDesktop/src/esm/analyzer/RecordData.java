package esm.analyzer;

import java.util.ArrayList;

public class RecordData {
	public String					type				= "";

	public int						formId				= -1;

	public boolean					instance			= false;

	// sub records in order
	public ArrayList<SubrecordData>	subrecordStatsList	= new ArrayList<SubrecordData>();

	public RecordData(String t, int id, boolean instance) {
		this.type = t;
		this.formId = id;
		this.instance = instance;
	}
}
