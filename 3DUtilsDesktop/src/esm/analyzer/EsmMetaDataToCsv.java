package esm.analyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.zip.DataFormatException;

import esfilemanager.common.PluginException;
import esfilemanager.common.data.display.Plugin;
import esfilemanager.common.data.display.PluginFile;
import esfilemanager.common.data.plugin.FormInfo;
import esfilemanager.common.data.plugin.PluginGroup;
import esfilemanager.common.data.plugin.PluginRecord;
import esfilemanager.common.data.record.Record;
import esfilemanager.common.data.record.Subrecord;
import esm.EsmFileLocations;
import tools.io.ESMByteConvert;
import tools.swing.TitledJFileChooser;

/**
 * Ok so I need to export to a csv for excel analysis
 * 
 * Always by esm name, as I need to run through all esms, or is this just a new csv each time?
 * 
 * I need, Record names that exists, and a count of them
 * 
 * RECO order of exisitance
 * 
 * Records that have a modl, and what bits are attached
 * 
 * REcords that have and don't have EDIDs
 * 
 * Sub records in each record, in order, and with optional, and with fixed occuarnce after XXXX sub record data range
 * size
 * 
 * sub record that is 8 bytes and so could be a formID identiifer, and the types of record pointed
 * 
 * 
 * Perhaps all data just goes to excel and I use pivots to build the picture?
 * 
 * 
 * look at the skyrim formatty thing on the internet
 * 
 * I need to make a definitive format file that lives in each game root
 * 
 */
public class EsmMetaDataToCsv {
	public static String										OUTPUT_FILE_KEY		= "outputFile";

	public static boolean										ANALYZE_CELLS		= false;

	public static boolean										WRITE_CSV			= false;

	public static boolean										WRITE_MD			= true;

	public static boolean										WRITE_JAVA			= true;

	public static File											outputFolder;

	public static BufferedWriter								csvOut;

	public static Plugin										pluginToAnalyze;

	public static Preferences									prefs;

	public static int											currentRowOrderNum	= 0;

	public static String										esmFileName			= "unknown";

	// just the meta data of records and subs to be turned into overall stats after full collection
	public static LinkedHashMap<String, ArrayList<RecordData>>	recordDataLists		= new LinkedHashMap<String, ArrayList<RecordData>>();

	public static void main(String args[]) {
		String generalEsmFile = EsmFileLocations.getGeneralEsmFile();

		// this is for csv full details output 
		if (generalEsmFile != null) {
			esmFileName = generalEsmFile.substring(generalEsmFile.lastIndexOf("\\") + 1);
			prefs = Preferences.userNodeForPackage(EsmMetaDataToCsv.class);
			outputFolder = TitledJFileChooser
					.requestFolderName("Select Output Folder (" + esmFileName.substring(0, esmFileName.lastIndexOf("."))
										+ " will be created as a sub folder)",
							prefs.get(OUTPUT_FILE_KEY, ""), null);

			if (outputFolder != null) {
				prefs.put(OUTPUT_FILE_KEY, outputFolder.getAbsolutePath());

				try {

					System.out.println("Loading file " + generalEsmFile);
					long startTime = System.currentTimeMillis();
					Plugin plugin = loadPlugin(new File(generalEsmFile));
					System.out.println("Finished loading in " + (System.currentTimeMillis() - startTime));

					System.out.println("Analyzing...");
					startTime = System.currentTimeMillis();
					analzePlugin(plugin);
					System.out.println("Finished analyzing in " + (System.currentTimeMillis() - startTime));

					System.out.println("Writing...");
					startTime = System.currentTimeMillis();
					writeOutMdFile();
					System.out.println("Finished writing in " + (System.currentTimeMillis() - startTime));

				} catch (PluginException e) {
					e.printStackTrace();
				} catch (DataFormatException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private static Plugin loadPlugin(File pluginFile) throws PluginException, DataFormatException, IOException {
		Plugin plugin = new PluginFile(pluginFile);

		plugin.load(false);

		//oddly form map isn't populated, not sure why, I'll do it now
		List<FormInfo> allForms = plugin.getFormList();
		for (FormInfo fi : allForms) {
			plugin.getFormMap().put(fi.getFormID(), fi);
		}
		return plugin;
	}

	private static void analzePlugin(Plugin plugin) {
		pluginToAnalyze = plugin;
		try {
			if (WRITE_CSV) {

				File csv = new File(outputFolder, esmFileName + ".csv");
				if (!csv.exists())
					csv.createNewFile();
				csvOut = new BufferedWriter(new FileWriter(csv));

				// output header row
				csvOut.append("num,");
				//out.append("EsmFileName,");
				csvOut.append("RecordType,");
				csvOut.append("FormID,");
				csvOut.append("RecordFlags1,");
				csvOut.append("RecordFlags2,");
				csvOut.append("EditorID,");
				csvOut.append("SubrecordType,");
				csvOut.append("SubOrder,");
				csvOut.append("DataLen,");
				csvOut.append("CouldBeFormID,");
				csvOut.append("CouldBeString,");

				csvOut.newLine();
			}
			for (PluginGroup group : pluginToAnalyze.getGroupList()) {
				analzeGroupChildren(group);
			}
			if (WRITE_CSV) {
				csvOut.close();
			}
		} catch (DataFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (PluginException e) {
			e.printStackTrace();
		}

	}

	private static void analzeGroupChildren(PluginGroup group)
			throws DataFormatException, IOException, PluginException {
		for (Record r : group.getRecordList()) {
			PluginRecord record = (PluginRecord)r;

			if (record instanceof PluginGroup) {
				analzeGroupChildren((PluginGroup)record);
			} else {
				if (ANALYZE_CELLS || group.getGroupType() == 0) {
					//String groupRecordType = group.getGroupRecordType();					

					String editorID = record.getEditorID();
					int formID = record.getFormID();
					int recordFlags1 = record.getRecordFlags1();
					int recordFlags2 = record.getRecordFlags2();

					//for md building
					RecordData recordData = new RecordData(record.getRecordType(), formID);
					ArrayList<RecordData> recordDataList = recordDataLists.get(record.getRecordType());
					if (recordDataList == null) {
						recordDataList = new ArrayList<RecordData>();
						recordDataLists.put(record.getRecordType(), recordDataList);
					}
					recordDataList.add(recordData);

					if (WRITE_CSV) {
						// to ease the file size slightly skip some trivial ones, negate this to get them decoded
						if (!skipitForCSV(record.getRecordType())) {

							int currentSubOrderNum = 0;
							List<Subrecord> subs = record.getSubrecords();
							for (int i = 0; i < subs.size(); i++) {
								Subrecord sub = subs.get(i);

								// skip the editorid we know about that guy
								if (sub.getSubrecordType().equals("EDID"))
									continue;

								//RECO header info, repeated everytime
								csvOut.append("" + currentRowOrderNum++);
								csvOut.append(",");
								//out.append(esmFileName);
								//out.append(",");
								csvOut.append(record.getRecordType());
								csvOut.append(",");
								csvOut.append("" + formID);
								csvOut.append(",");
								csvOut.append("" + recordFlags1);
								csvOut.append(",");
								csvOut.append("" + recordFlags2);
								csvOut.append(",");
								csvOut.append(editorID);// this has no spaces 
								csvOut.append(",");
								csvOut.append(sub.getSubrecordType());
								csvOut.append(",");
								csvOut.append("" + currentSubOrderNum++);
								csvOut.append(",");
								csvOut.append("" + sub.getSubrecordData().length);
								csvOut.append(",");
								String couldBeFormID = couldBeFormID(sub.getSubrecordData());
								csvOut.append(couldBeFormID == null ? "" : couldBeFormID);
								csvOut.append(",");
								String couldBeString = couldBeString(record.getRecordType(), sub.getSubrecordType(),
										sub.getSubrecordData());
								csvOut.append(couldBeString == null ? "" : escape(couldBeString));

								csvOut.newLine();
								csvOut.flush();

								optionalDecodeOutput(record.getRecordType(), sub.getSubrecordType(),
										sub.getSubrecordData());
							}
						}
					}

					/// stats for md builder
					int currentSubOrderNum = 0;
					List<Subrecord> subs = record.getSubrecords();
					for (int i = 0; i < subs.size(); i++) {
						Subrecord sub = subs.get(i);

						// skip the editorid we know about that guy
						//if (sub.getSubrecordType().equals("EDID"))
						//	continue;

						String subTypeBefore = null;
						String subTypeAfter = null;
						if (i > 0)
							subTypeBefore = subs.get(i - 1).getSubrecordType();
						if (i < subs.size() - 1)
							subTypeAfter = subs.get(i + 1).getSubrecordType();
						String couldBeFormID = couldBeFormID(sub.getSubrecordData());
						String couldBeString = couldBeString(record.getRecordType(), sub.getSubrecordType(),
								sub.getSubrecordData());
						SubrecordData srs = new SubrecordData(sub, record.getRecordType(), currentSubOrderNum,
								subTypeBefore, subTypeAfter, couldBeFormID, couldBeString);
						recordData.subrecordStatsList.add(srs);

					}

				}
			}

		}

	}

	private static boolean skipitForCSV(String recType) {
		if (esmFileName.equals("Skyrim.esm")
			&& (recType.equals("GMST")	|| recType.equals("KYWD")
				|| recType.equals("LCRT") || recType.equals("GLOB") || recType.equals("TXST")
				|| recType.equals("STAT")))
			return true;

		//PACK is a huge one as well
		//DIAL//QUST		

		return false;
	}

	//https://stackoverflow.com/questions/6377454/escaping-tricky-string-to-csv-format
	private static String escape(String str) {

		boolean mustQuote = (str.contains(",") || str.contains("\"") || str.contains("\r") || str.contains("\n"));
		if (mustQuote) {
			str = str.replace("\"", "\"\"");
			str = "\"" + str + "\"";
		}

		return str;
	}

	/**
	 * return 4 char type of Record pointed at, if formId
	 * @param bs
	 * @return
	 */

	public static String couldBeFormID(byte[] bs) {
		if (bs.length == 4) {
			String ret = "";
			int asInt = ESMByteConvert.extractInt3(bs, 0);
			if (asInt >= 1000 && asInt < 1000000) {
				FormInfo fi = pluginToAnalyze.getFormMap().get(asInt);
				if (fi != null)
					ret += fi.getRecordType() + ":";
			}

			if (asInt > -1000 && asInt < 100000)
				ret += asInt + " ";

			// odd floats are a waste of space
			try {
				float f = ESMByteConvert.extractFloat(bs, 0);
				int scale = floatScale(f);
				if (scale < 4 && scale > -4)
					ret += f + "f";
			} catch (Exception e) {
			}

			return ret;
		}

		return null;
	}

	// Taken straight from BigDecimal
	public static int floatScale(float val) {
		// Translate the double into sign, exponent and significand, according
		// to the formulae in JLS, Section 20.10.22.
		int valBits = Float.floatToIntBits(val);
		//int sign = ((valBits >> 63) == 0 ? 1 : -1);
		int exponent = (int)((valBits >> 52) & 0x7ffL);
		int significand = (exponent == 0 ? (valBits & ((1 << 52) - 1)) << 1 : (valBits & ((1 << 52) - 1)) | (1 << 52));
		exponent -= 1075;
		// At this point, val == sign * significand * 2**exponent.

		/*
		 * Special case zero to supress nonterminating normalization and bogus
		 * scale calculation.
		 */
		if (significand == 0) {
			return 0;
		}
		// Normalize
		while ((significand & 1) == 0) { // i.e., significand is even
			significand >>= 1;
			exponent++;
		}

		return exponent;
	}

	public static String couldBeFormID(int formId) {

		if (formId >= 0 || formId < 1000000) {
			FormInfo fi = pluginToAnalyze.getFormMap().get(formId);
			//GRUP is low number probably just an int
			if (fi != null && !fi.getRecordType().equals("GRUP"))
				return fi.getRecordType();
		}

		return null;
	}

	/**
	 * null if not a string
	 * @param bs
	 * @return
	 */
	public static String couldBeString(String recType, String subtype, byte[] bs) {
		// some matches to pattern that shouldn't 
		if (recType.equals("NPC_") && subtype.equals("HCLR") && bs.length == 4 || // color4 of bytes
			recType.equals("REGN") && subtype.equals("RCLR") && bs.length == 4 || // color4 of bytes
			recType.equals("ACTI") && subtype.equals("FULL") && bs.length == 2
			|| recType.equals("TERM") && subtype.equals("ITXT") && bs.length == 3
			|| recType.equals("QUST") && subtype.equals("INDX") && bs.length == 2)
			return null;

		//TODO: are they always zero terminated ? 
		if (bs.length > 0 && bs[bs.length - 1] == 0) {

			int length = bs.length;
			for (int i = 0; i < length - 1; i++)
				if (bs[i] < 0x20 || bs[i] >= 0x80)
					return null;

			return new String(bs, 0, bs.length - 1);

		}

		return null;
	}

	private static void optionalDecodeOutput(String recType, String subtype, byte[] bs) {
		if (recType.equals("IDLE") && subtype.equals("CTDA") && bs.length == 24) {
			//System.out.println("PACK CTDA ");
			//System.out.println( "0:" + ESMByteConvert.extractInt(bs, 4));
			//System.out.println( "4:" + ESMByteConvert.extractInt(bs, 12));
			//System.out.println( "8:" + ESMByteConvert.extractInt(bs, 16));
			/*System.out.println( "0:" + couldBeFormID(ESMByteConvert.extractInt(bs, 0)));
			System.out.println( "4:" + couldBeFormID(ESMByteConvert.extractInt(bs, 4)));
			System.out.println( "8:" + couldBeFormID(ESMByteConvert.extractInt(bs, 8)));
			System.out.println( "12:" + couldBeFormID(ESMByteConvert.extractInt(bs, 12)));
			System.out.println( "16:" + couldBeFormID(ESMByteConvert.extractInt(bs, 16)));
			System.out.println( "20:" + couldBeFormID(ESMByteConvert.extractInt(bs, 20)));*/

		}

	}

	private static void writeOutMdFile() throws IOException {
		BufferedWriter mdOut = null;
		if (WRITE_MD) {
			File mdFile = new File(outputFolder, esmFileName + ".md");
			if (!mdFile.exists())
				mdFile.createNewFile();
			mdOut = new BufferedWriter(new FileWriter(mdFile));

			//TODO: customize per esm File!
			String header = "Esm Record Formats \n\n"	+ "=== \n\n" + "## URL \n\n"
							+ "<p><a href=\"https://en.uesp.net/wiki/Oblivion:Oblivion\">https://en.uesp.net/wiki/Oblivion:Oblivion</a></p> \n\n"
							+ "<p><a href=\"https://en.uesp.net/wiki/Oblivion_Mod:Modding#Toolmaker_Info\">https://en.uesp.net/wiki/Oblivion_Mod:Modding#Toolmaker_Info</a></p> \n\n"
							+ "<p><a href=\"https://en.uesp.net/wiki/Oblivion_Mod:Mod_File_Format\">https://en.uesp.net/wiki/Oblivion_Mod:Mod_File_Format</a></p> \n\n"
							+ "## My cut of it   \n\n";

			mdOut.append(header);
		}

		//Note we want RECO in alpha order but not subs!
		Map<String, ArrayList<RecordData>> sortedRecsMap = getSortedRecsMap(recordDataLists);
		for (ArrayList<RecordData> rds : sortedRecsMap.values()) {
			String desc = PluginGroup.typeMap.get(rds.get(0).type);
			if (WRITE_MD) {
				String h2 = "<p><b>"	+ rds.get(0).type + "</b> " + desc + " count=" + rds.size() + "</p> \n\n"
							+ "<table class=\"wikitable\" width=\"100%\"> \n\n" + "<tbody><tr> \n\n"
							+ "<th width=\"5%\">C________</th> \n\n" + "<th width=\"5%\">Subrecord</th> \n\n"
							+ "<th width=\"10%\"><a href=\"https://en.uesp.net/wiki/Oblivion_Mod:File_Format_Conventions\" title=\"Oblivion Mod:File Format Conventions\">Type______</a></th> \n\n"
							+ "<th>Info</th> \n\n" + "</tr> \n\n" + "";//+ "<tr> \n\n" + "<td>1</td> \n\n"	+ "<td>EDID</td> \n\n" + "<td>ZString</td> \n\n"
				//+ "<td>Editor ID, used only by consturction kit, not loaded at runtime</td> \n\n"//TODO: some don't have this!! must just chuck it in the pile like others
				//+ "</tr> \n\n";
				mdOut.append(h2);

				//System.out.println("Writing RECO to md file : " + rds.get(0).type);
			}

			//I have all records and their list of subs now
			/// so I need to build a list of subreco type and for each one work out the cardinality, type and likely data (formid pointer or string style)
			int maxSubCount = 0;
			LinkedHashSet<String> allsubTypes = new LinkedHashSet<String>();
			for (RecordData rd : rds) {
				List<SubrecordData> subrecordStatsList = rd.subrecordStatsList;
				for (SubrecordData srd : subrecordStatsList) {
					allsubTypes.add(srd.subrecordType);

					//TODO: delete this debug out
					//if (rds.get(0).type.equals("ALCH"))// note I re order the records to alpha perhaps I should do that earlier to make debg easier?
					//	System.out.println(rd.formId + " " + "ALCH putting " + srd.subrecordType);
				}

				if (maxSubCount < subrecordStatsList.size())
					maxSubCount = subrecordStatsList.size();
			}

			// a place to put the rows data
			LinkedHashMap<String, SubrecordStats> allSubTypesData = new LinkedHashMap<String, SubrecordStats>();

			// for each one build a cardinality and type
			for (String subType : allsubTypes) {

				SubrecordStats subrecordStats = new SubrecordStats(subType);
				allSubTypesData.put(subType, subrecordStats);

				for (RecordData rd : rds) {
					int countForRecord = 0;

					List<SubrecordData> subrecordStatsList = rd.subrecordStatsList;
					for (SubrecordData srd : subrecordStatsList) {
						if (srd.subrecordType.equals(subType)) {
							subrecordStats.totalCount++;
							countForRecord++;// cardinality		

							subrecordStats.applySub(srd);
						}
					}

					if (countForRecord != 1)
						subrecordStats.cardexact1 = false;// all must be 1!

					if (countForRecord == 0)
						subrecordStats.cardZero = true;// any must be 0

					if (countForRecord > 1)
						subrecordStats.cardMult = true;// any must be multi		

					subrecordStats.cardMax = subrecordStats.cardMax < countForRecord ? countForRecord : subrecordStats.cardMax;
					subrecordStats.cardMin = subrecordStats.cardMin > countForRecord ? countForRecord : subrecordStats.cardMin;

				}

				subrecordStats.organiseDerivedData();

			}

			///////////////////////////SORT THE SUBS!
			// now in order roll through the subs stats with the determined C and type and desc
			ArrayList<SubrecordStats> sortedAllSubTypesData = getSortedSubRecs(allSubTypesData);

			//NOTCIE BIG TIME, WITNESS ME!!!! ALCH has the FULL type used twice! so it has 2 positions and is not good for testing
			//  but that might just be a real system, in which case at the stats gathering stage I have to do something weird! super weird.

			ArrayList<SubRecTuple> tups = getSortedSubRecTuples(allSubTypesData);

			System.out.println("debug for " + rds.get(0).type);
			//debug just print them
			for (SubRecTuple s : tups) {
				System.out.println(" tuple = " + s);
			}

			if (WRITE_MD) {
				// for each one build a table
				for (SubrecordStats subrecordStats : sortedAllSubTypesData) {
					String sr = "<tr> \n\n" + "<td>" + subrecordStats.C + "</td> \n\n" + "<td>"
								+ subrecordStats.subrecordType + "</td> \n\n" + "<td>" + subrecordStats.dataType
								+ "</td> \n\n" + "<td>" + subrecordStats.locationDesc + ". " + subrecordStats.desc
								+ "</td> \n\n" + "</tr> \n\n";

					mdOut.append(sr);
				}

				mdOut.append("</table></tbody>");

				//System.out.println("End writing RECO to md file : " + rds.get(0).type);
				mdOut.flush();
			}

			//////////NOW to build java classes of the same!!!
			if (WRITE_JAVA) {
				createJavaClass(rds.get(0), desc, sortedAllSubTypesData);
			}

		}

	}

	public static Map<String, ArrayList<RecordData>> getSortedRecsMap(Map<String, ArrayList<RecordData>> recordStatsList) {
		List<Map.Entry<String, ArrayList<RecordData>>> entries = new ArrayList<Map.Entry<String, ArrayList<RecordData>>>(
				recordStatsList.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<String, ArrayList<RecordData>>>() {
			@Override
			public int compare(Map.Entry<String, ArrayList<RecordData>> a, Map.Entry<String, ArrayList<RecordData>> b) {
				return a.getKey().compareTo(b.getKey());
			}
		});

		Map<String, ArrayList<RecordData>> sortedMap = new LinkedHashMap<String, ArrayList<RecordData>>();
		for (Map.Entry<String, ArrayList<RecordData>> entry : entries) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}

		return sortedMap;
	}

	public static ArrayList<SubrecordStats> getSortedSubRecs(LinkedHashMap<String, SubrecordStats> allSubTypesData) {
		ArrayList<SubrecordStats> sorted = new ArrayList<SubrecordStats>(allSubTypesData.values());

		///TODO: can't have transitive comparision, it doesn't work like that'
		///
		///so I have to say if after.contains, or if any of the afters.after contains etc
		///gonna be a tricky one, probably not able to us ehte .sort method
		/*		Collections.sort(sorted, new Comparator<SubrecordStats>() {
					@Override
					public int compare(SubrecordStats a, SubrecordStats b) {
						if (a.subTypesAfter.contains(b.subrecordType))//|| b.subTypesBefore.contains(a.subrecordType))
							return -1;
						else if (b.subTypesAfter.contains(a.subrecordType))//|| a.subTypesBefore.contains(b.subrecordType))
							return 1;
						else
							return 0;
		
					}
				});*/

		return sorted;
	}

	/**
	 * Note the groups and optionally after etc is actually going to need ot be encoded so I can build editors and
	 * loaders, so my current cardinality is only for display to a user but the groupsings are going to need their own
	 * data strucuture proper. That wa I can Also write out a good description too - data of SubRecoTuples each one in
	 * order with optionality to and a descripton writer the SubRecTuples then get in order (they have a front info and
	 * back info) then the write out comes in fact from them
	 * 
	 * 
	 * So I see somethigns taht have "always followed by" which makes them pairs, if the followed by has always after
	 * too
	 * 
	 * In these case the cardinalit should be idnetical too, so I've got to get pairs or triplets or more into a single
	 * fixed unti then I've got the optional but always follows tpe of one.
	 * 
	 */
	public static ArrayList<SubRecTuple> getSortedSubRecTuples(LinkedHashMap<String, SubrecordStats> allSubTypesData) {
		ArrayList<SubRecTuple> tuples = new ArrayList<SubRecTuple>();
		ArrayList<SubRecTuple> sortedTuples = new ArrayList<SubRecTuple>();

		// start with a budle of singel stat tuple to be merged into groups (or not)
		for (SubrecordStats s : allSubTypesData.values()) {
			tuples.add(new SubRecTuple(s));
		}

		// go through the stats, build whatever chains we can, remove the stat if it's in a tuple (except header)
		// and repeat until no more stats get added to a tuple on a pass
		boolean changesMade = true;
		while (changesMade) {
			changesMade = false;
			for (int i = 0; i < tuples.size(); i++) {
				SubRecTuple t = tuples.get(i);
				for (int j = i + 1; j < tuples.size(); j++) {
					SubRecTuple tuple2 = tuples.get(j);
					if (t.test(tuple2)) {
						// this tuple2 gets removed from the over all list as it's now found a home inside this tuple
						tuples.remove(j);
						// move us backward by 1 so the new item at the pos i is tested
						j = j - 1;
						changesMade = true;
						// don't break as we might find more homes
					}
				}
			}
		}

		// now take the head and tail of each tuple and compare to put them in sorted tuples
		for (SubRecTuple t : tuples) {
			//if(t.)
			//sortedTuples
		}

		return tuples;
	}

	private static class SubRecTuple {
		public static class Item {
			public SubrecordStats	stat		= null;
			public boolean			optional	= false;

			public Item(SubrecordStats stat) {
				this.stat = stat;
			}

			public Item(SubrecordStats stat, boolean optional) {
				this.stat = stat;
				this.optional = optional;
			}

		}

		public ArrayList<Item> items = new ArrayList<Item>();

		/**
		 * Kick off with a single header and see if any others get added, if we end at size 1 we discard this structure
		 * @param header
		 */
		public SubRecTuple(SubrecordStats header) {
			items.add(new Item(header, header.cardZero));

		}

		public SubrecordStats getHeadStat() {
			return items.get(0).stat;
		}

		// might be interesting if there is multiple optional at the end of a tuple?? can that exist?
		public SubrecordStats getTailStat() {
			return items.get(items.size() - 1).stat;
		}

		/**
		 * This will test the tuple for being part of this tuple (after the current parts) and also merge it into teh
		 * back
		 * @param potentialNextItem
		 * @return if a match has made
		 */
		public boolean test(SubRecTuple potentialNextTuple) {

			SubrecordStats tail = getTailStat();
			SubrecordStats potHead = potentialNextTuple.getHeadStat();
			// is the potential always and only after one of our elements
			// if so we'd better see it in the sub types after!
			if (potHead.alwaysAfter() != null && potHead.alwaysAfter().equals(tail.subrecordType)) {
				// now to discover the optionality of this guy
				boolean optional = true;

				// if it's the only guy that appears after they are fo so mandatory (cardinality will match too)
				if (tail.alwaysBefore() != null && tail.alwaysBefore().equals(potHead.subrecordType))
					optional = false;

				// all at the tail
				for (Item item : potentialNextTuple.items) {
					item.optional = optional; //override it (whatever it was) 
					//TODO: confirm there isn't some crazy optional set of 2 sub but they are always a pair
					//so the second is in fact mandatory thou the set is optional
					items.add(item);
				}

				//output any oddities
				if (!tail.subTypesAfter.contains(potHead.subrecordType))
					System.out.println("PROBLEM after but not in after list!! " + potHead.subrecordType + " "
										+ tail.subrecordType);

				// can't be anywhere else I wager (false FULL in ALCH in Oblivion for example)
				return true;
			} else {
				// TODO: what are the other options here?
			}

			return false;
		}

		@Override
		public String toString() {
			String s = "SubRecTuple ";// make each one unique cos I can't get teh REc type here
			for (Item item : items) {
				s += item.stat.subrecordType + " opt " + item.optional + ", ";
			}
			return s;
		}

	}

	private static void createJavaClass(RecordData recordData, String desc,
										ArrayList<SubrecordStats> sortedAllSubTypesData)
			throws IOException {

		String betterJavaName = PluginGroup.typeMap.get(recordData.type);

		if (betterJavaName == null)
			System.out.println("Not recordData.type " + recordData.type);

		// did we have no lookup, or a questionable lookup
		if (betterJavaName == null || betterJavaName.indexOf("?") != -1) {
			betterJavaName = recordData.type;
		} else {
			int bracketOpen = betterJavaName.indexOf("(");
			if (bracketOpen != -1)
				betterJavaName = betterJavaName.substring(0, bracketOpen);
			betterJavaName = betterJavaName.replaceAll(" ", "");
		}

		String esmClean = esmFileName.substring(0, esmFileName.lastIndexOf("."));

		File javaRECOFolder = new File(outputFolder, esmClean);
		javaRECOFolder.mkdirs();

		File javaClassOut = new File(javaRECOFolder, betterJavaName + ".java");
		if (!javaClassOut.exists())
			javaClassOut.createNewFile();

		BufferedWriter javaBW = new BufferedWriter(new FileWriter(javaClassOut));

		/**
		 * Concept Create a compilable java class, that I can open a project with and load an esm of the appropriate
		 * version each game version will have a package space in this test project load the esm into java classes with
		 * members and zero erros, then fire every esm at it.
		 * 
		 * after all esm incl 76 are working perfectly, then integration with current code can happen (member names
		 * getting modded etc)
		 * 
		 * Possibly I could make a custom editor for each version to make the editor a bit sexier to make them all
		 * build?
		 * 
		 * I don't seem to be analyzing the cell and REFR data I'm going to have to
		 * 
		 */

		javaBW.append("package esmj3d." + esmClean + ".records; \n");
		javaBW.newLine();
		javaBW.append("import esfilemanager.common.data.record.Record;\n");
		javaBW.append("import esfilemanager.common.data.record.Subrecord;\n");
		javaBW.append("import esmj3d.data.shared.records.RECO;\n");
		javaBW.append("import esmj3d.data.shared.subrecords.FormID;\n");
		javaBW.append("import esmj3d.data.shared.subrecords.LString;\n");
		javaBW.append("import esmj3d.data.shared.subrecords.MODL;\n");
		javaBW.newLine();
		javaBW.append("public class " + betterJavaName + " extends InstRECO { \n");
		javaBW.newLine();

		//for each one build a table
		for (SubrecordStats subrecordStats : sortedAllSubTypesData) {
			javaBW.append("\t"	+ subrecordStats.subrecordType + " " + subrecordStats.dataType + ";// with a C of "
							+ subrecordStats.C + "\n");
			javaBW.newLine();
		}
		javaBW.newLine();
		javaBW.append("\tpublic " + betterJavaName + "(Record recordData) { \n");
		javaBW.append("\t} \n");
		javaBW.append("} \n");
		javaBW.flush();
		javaBW.close();

	}

}