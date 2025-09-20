package esm.analyzer;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

public class SubrecordStats {
	public String							subrecordType;

	public String							C;

	public String							dataType;

	public String							desc			= "";

	public int								count			= 0;

	public int								minLength		= Integer.MAX_VALUE;

	public int								maxLength		= Integer.MIN_VALUE;

	//	public int minSubPos = Integer.MAX_VALUE;

	//	public int maxSubPos = Integer.MIN_VALUE;	

	//	public HashSet<Integer> hasOrderOf = new HashSet<Integer>();		 

	public HashSet<Integer>					fewSizes		= new HashSet<Integer>();

	public HashSet<String>					subTypesBefore	= new HashSet<String>();

	public HashSet<String>					subTypesAfter	= new HashSet<String>();

	// count of formId pointer types
	public LinkedHashMap<String, Integer>	formTypeCounts	= new LinkedHashMap<String, Integer>();

	public int								countOfString	= 0;
	public int								countOfNif		= 0;
	public int								countOfDds		= 0;
	public int								countOfKf		= 0;

	public int								countOfInts		= 0;
	public int								countOfFloats	= 0;
	public int								countOfVec3		= 0;
	public int								countOfVec4		= 0;

	// details filled in after all sub are applied
	private boolean							locked			= false;

	public int								totalCount		= 0;

	public boolean							cardexact1		= true; // is exactly always 1
	public boolean							cardZero		= false;// can be zero
	public boolean							cardMult		= false;// can be greater than 1

	public int								cardMax			= 0;
	public int								cardMin			= Integer.MAX_VALUE;

	public String							locationDesc;

	public SubrecordStats(String st) {
		this.subrecordType = st;
	}

	public void applySub(SubrecordData srd) {

		if (locked)
			System.err.println("Can't apply subs after derived stats calculated!!!");

		//		hasOrderOf.addAll(srd.hasOrderOf);
		subTypesBefore.add(srd.subTypesBefore);
		subTypesAfter.add(srd.subTypesAfter);

		count += srd.count;

		// need to add up the formtype counts
		for (Entry<String, Integer> entry : srd.formTypeCounts.entrySet()) {
			Integer current = formTypeCounts.get(entry.getKey());
			current = current == null ? 0 : current;
			formTypeCounts.put(entry.getKey(), current + entry.getValue());
		}

		/*		for(int ord : srd.hasOrderOf) {
					if(ord < minSubPos)
						minSubPos = ord;
					
					if(ord > maxSubPos)
						maxSubPos = ord;
				}*/

		fewSizes.add(srd.size);

		countOfString += srd.countOfString;
		countOfNif += srd.countOfNif;
		countOfDds += srd.countOfDds;

		countOfInts += srd.countOfInts;
		countOfFloats += srd.countOfFloats;
		countOfVec3 += srd.countOfVec3;
		countOfVec4 += srd.countOfVec4;
		if (minLength > srd.size) {
			minLength = srd.size;
		}

		if (maxLength < srd.size) {
			maxLength = srd.size;
		}

	}

	/**
	 * Call after all subs have been applied so the derived data can be established
	 */
	public void organiseDerivedData() {
		locked = true;

		// can be - + * 
		//regex shorthand don't include exactly 1

		//1 = 1 only
		//1+ = 1 or more  
		//0+ = 0 or more 
		//0-1 = 0 or 1

		// so I might add cardinality of 
		// ^1 always after the RECO before				
		// ^? optionally after the one before

		// now get cardinality sorted out
		C = cardexact1 ? "1" : (!cardZero && cardMult) ? "1+" : cardZero ? (cardMult ? "0+" : "0-1") : "??";

		// just for fun put the whole range
		C += cardMult ? " (" + cardMin + "-" + cardMax + ")" : "";

		dataType = "???";
		//TODO: need to make sure it's formID or blanks to be formId
		//formTypeCounts has a 0 to mean pointer but to 0, so need only 0 or real
		int formTypeCountsLen = formTypeCounts.entrySet().size();
		if (formTypeCountsLen > 0 && formTypeCountsLen < 3) {
			dataType = "FormId";
		} else if (countOfString > totalCount * 0.2)
			dataType = "ZString";
		else if (countOfInts > totalCount * 0.2)
			dataType = "Int";
		else if (countOfFloats > totalCount * 0.2)
			dataType = "Float";
		else if (countOfVec3 > totalCount * 0.2)
			dataType = "Vec3";
		else if (countOfVec4 > totalCount * 0.2)
			dataType = "Vec4";
		else if (formTypeCountsLen > 2) {
			dataType = "FormId?";//suspect cos to many types pointed at
		} else {
			dataType = "byte[]";
			// we give up at 5
			if (fewSizes.size() > 0 && fewSizes.size() < 5) {
				for (int s : fewSizes)
					dataType += " " + s + ",";

				dataType = dataType.substring(0, dataType.length() - 1);
			}
		}

		if (countOfNif > 0)
			desc += "Including names of nif files. ";
		if (countOfDds > 0)
			desc += "Including names to dds files. ";
		if (formTypeCounts.keySet().size() < 5)// too many is suspect
			for (String formRecordType : formTypeCounts.keySet())
				desc += "Pointers to " + formRecordType + ". ";

		if (subrecordType.equals("EDID"))
			desc = "Editor ID, used only by consturction kit, not loaded at runtime";

		// create a wee after type decriptor for fun
		locationDesc = "After ";
		for (String after : subTypesBefore)
			locationDesc = (after == null ? "First Sub " : locationDesc + (after + ", "));
		locationDesc += " Followed by ";
		for (String before : subTypesAfter)
			locationDesc += (before == null ? " Last " : (before + ", "));

	}

	public String alwaysAfter() {
		if (subTypesBefore.size() == 1)
			return subTypesBefore.iterator().next();

		return null;
	}

	public String alwaysBefore() {
		if (subTypesAfter.size() == 1)
			return subTypesAfter.iterator().next();

		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (o != null && o instanceof SubrecordStats && ((SubrecordStats)o).subrecordType.equals(this.subrecordType))
			return true;
		return false;
	}

}
