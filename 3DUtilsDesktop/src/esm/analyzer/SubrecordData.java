package esm.analyzer;

import java.util.HashMap;

import esfilemanager.common.data.record.Subrecord;
import tools.io.ESMByteConvert;

public class SubrecordData {
	//This might be swapped in case of double ups!!
	public String					subrecordType;

	// if multiple version in esm used to analyze separately
	public int						versionId		= 0;

	public int						count			= 0;

	public int						hasOrderOf		= -1;

	public int						size			= 0;

	public String					subTypesBefore	= null;

	public String					subTypesAfter	= null;

	// count of formId pointer types
	public HashMap<String, Integer>	formTypeCounts	= new HashMap<String, Integer>();

	public int						countOfString	= 0;
	public int						countOfNif		= 0;
	public int						countOfDds		= 0;

	public int						countOfInts		= 0;
	public int						countOfFloats	= 0;
	public int						countOfVec3		= 0;
	public int						countOfVec4		= 0;

	public SubrecordData(	Subrecord sub, String inRec, int versionId, int orderNo, String subTypeBefore,
							String subTypeAfter, String couldBeFormID, String couldBeString) {
		subrecordType = sub.getSubrecordType();
		this.versionId = versionId;
		hasOrderOf = orderNo;
		subTypesBefore = subTypeBefore;
		subTypesAfter = subTypeAfter;

		count++;

		if (couldBeFormID != null && couldBeFormID.indexOf(":") != -1) {
			String formIdType = couldBeFormID.substring(0, couldBeFormID.indexOf(":"));
			if (formIdType.length() == 4) {
				if (formTypeCounts.get(formIdType) == null) {
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
		size = bs.length;

	}

	public void checkSimpleType(byte[] bs) {
		if (bs.length == 4) {

			int possI = ESMByteConvert.extractInt(bs, 0);

			if (possI > -1000 && possI < 100000)
				countOfInts++;

			try {
				int possFscale = EsmMetaDataToCsv.floatScale(ESMByteConvert.extractFloat(bs, 0));
				if (possFscale < 4 && possFscale > -4)
					countOfFloats++;
			} catch (Exception e) {
			}
		} else if (bs.length == 12) {
			try {
				int possF1scale = EsmMetaDataToCsv.floatScale(ESMByteConvert.extractFloat(bs, 0));
				int possF2scale = EsmMetaDataToCsv.floatScale(ESMByteConvert.extractFloat(bs, 4));
				int possF3scale = EsmMetaDataToCsv.floatScale(ESMByteConvert.extractFloat(bs, 8));
				if (possF1scale < 4 && possF1scale > -4 && possF2scale < 4 && possF2scale > -4 && possF3scale < 4
					&& possF3scale > -4)
					countOfVec3++;
			} catch (Exception e) {
			}
		} else if (bs.length == 16) {
			try {
				int possF1scale = EsmMetaDataToCsv.floatScale(ESMByteConvert.extractFloat(bs, 0));
				int possF2scale = EsmMetaDataToCsv.floatScale(ESMByteConvert.extractFloat(bs, 4));
				int possF3scale = EsmMetaDataToCsv.floatScale(ESMByteConvert.extractFloat(bs, 8));
				int possF4scale = EsmMetaDataToCsv.floatScale(ESMByteConvert.extractFloat(bs, 12));
				if (possF1scale < 4 && possF1scale > -4 && possF2scale < 4 && possF2scale > -4 && possF3scale < 4
					&& possF3scale > -4 && possF4scale < 4 && possF4scale > -4)
					countOfVec4++;
			} catch (Exception e) {
			}
		}

	}

	@Override
	public boolean equals(Object o) {
		if (o != null && o instanceof SubrecordData && ((SubrecordData)o).subrecordType.equals(this.subrecordType))
			return true;
		return false;
	}
}
