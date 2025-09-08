package esm.analyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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

	public static boolean										ANALYZE_CELLS		= true;

	public static boolean										WRITE_CSV			= true;

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
						if (sub.getSubrecordType().equals("EDID"))
							continue;

						String subTypeBefore = null;
						String subTypeAfter = null;
						if (i > 0)
							subTypeBefore = subs.get(i - 1).getSubrecordType();
						if (i > subs.size() - 1)
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
					ret += fi.getRecordType() + ": ";
			}

			if (asInt > -1000 && asInt < 100000)
				ret += asInt + " ";

			// odd floats are a waste of space
			try {
				float f = ESMByteConvert.extractFloat(bs, 0);
				BigDecimal possF = new BigDecimal(f);
				if (possF.scale() < 4 && possF.scale() > -4)
					ret += f + "f";
			} catch (Exception e) {
			}

			return ret;
		}

		return null;
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
				String h2 = "<p><b>"	+ rds.get(0).type + "</b> " + desc + "</p> \n\n"
							+ "<table class=\"wikitable\" width=\"100%\"> \n\n" + "<tbody><tr> \n\n"
							+ "<th width=\"5%\">C____</th> \n\n" + "<th width=\"5%\">Subrecord</th> \n\n"
							+ "<th width=\"10%\"><a href=\"https://en.uesp.net/wiki/Oblivion_Mod:File_Format_Conventions\" title=\"Oblivion Mod:File Format Conventions\">Type______</a></th> \n\n"
							+ "<th>Info</th> \n\n" + "</tr> \n\n" + "<tr> \n\n" + "<td>1</td> \n\n"
							+ "<td>EDID</td> \n\n" + "<td>ZString</td> \n\n"
							+ "<td>Editor ID, used only by consturction kit, not loaded at runtime</td> \n\n"
							+ "</tr> \n\n";
				mdOut.append(h2);

				//System.out.println("Writing RECO to md file : " + rds.get(0).type);
			}

			//I have all records and their list of subs now
			/// so I need to build a list of subreco type and for each one work out the cardinality, type and likely data (formid pointer or string style)
			int maxSubCount = 0;
			HashSet<String> allsubTypes = new HashSet<String>();
			for (RecordData rd : rds) {
				List<SubrecordData> subrecordStatsList = rd.subrecordStatsList;
				for (SubrecordData srd : subrecordStatsList) {
					allsubTypes.add(srd.subrecordType);
				}

				if (maxSubCount < subrecordStatsList.size())
					maxSubCount = subrecordStatsList.size();
			}

			// a place to put the rows data
			HashMap<String, SubrecordStats2> allSubTypesData = new HashMap<String, SubrecordStats2>();

			// for each one build a card and type
			for (String subType : allsubTypes) {
				SubrecordStats2 subrecordStats = new SubrecordStats2(subType);
				allSubTypesData.put(subType, subrecordStats);

				int totalCount = 0;

				boolean cardexact1 = true;
				boolean cardZero = false;
				boolean cardMult = false;

				for (RecordData rd : rds) {
					int countForRecord = 0;

					List<SubrecordData> subrecordStatsList = rd.subrecordStatsList;
					for (SubrecordData srd : subrecordStatsList) {
						if (srd.subrecordType.equals(subType)) {
							totalCount++;
							countForRecord++;// cardinality		

							subrecordStats.applySub(srd);
						}
					}

					if (countForRecord != 1)
						cardexact1 = false;// all must be 1!

					if (countForRecord == 0)
						cardZero = true;// any must be 0

					if (countForRecord > 1)
						cardMult = true;// any must be multi					

				}

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
				subrecordStats.C = cardexact1 ? "1" : (!cardZero
														&& cardMult) ? "1+" : cardZero ? (cardMult ? "0+" : "0-1") : "??";

				subrecordStats.dataType = "???";
				//TODO: need to make sure it's formID or blanks to be formId
				//formTypeCounts has a 0 to mean pointer but to 0, so need only 0 or real
				int formTypeCountsLen = subrecordStats.formTypeCounts.entrySet().size();
				if (formTypeCountsLen > 0 && formTypeCountsLen < 3) {
					subrecordStats.dataType = "FormId";
				} else if (subrecordStats.countOfString > totalCount * 0.2)
					subrecordStats.dataType = "ZString";
				else if (subrecordStats.countOfInts > totalCount * 0.2)
					subrecordStats.dataType = "Int";
				else if (subrecordStats.countOfFloats > totalCount * 0.2)
					subrecordStats.dataType = "Float";
				else if (subrecordStats.countOfVec3 > totalCount * 0.2)
					subrecordStats.dataType = "Vec3";
				else if (subrecordStats.countOfVec4 > totalCount * 0.2)
					subrecordStats.dataType = "Vec4";
				else if (formTypeCountsLen > 2) {
					subrecordStats.dataType = "FormId?";//suspect cos to many types pointed at
				} else {
					subrecordStats.dataType = "byte[]";
					// we give up at 5
					if (subrecordStats.fewSizes.size() > 0 && subrecordStats.fewSizes.size() < 5) {
						for (int s : subrecordStats.fewSizes)
							subrecordStats.dataType += " " + s + ",";

						subrecordStats.dataType = subrecordStats.dataType.substring(0,
								subrecordStats.dataType.length() - 1);
					}
				}

				if (subrecordStats.countOfNif > 0)
					subrecordStats.desc += "Including pointers to nif files. ";
				if (subrecordStats.countOfDds > 0)
					subrecordStats.desc += "Including pointers to dds files. ";
				if (subrecordStats.formTypeCounts.keySet().size() < 3)// too many is suspect
					for (String formRecordType : subrecordStats.formTypeCounts.keySet())
						subrecordStats.desc += "Pointers to " + formRecordType + ". ";

			}

			///////////////////////////SORT THE SUBS!	

			// now in order roll through the subs stats with the determined C and type and desc
			ArrayList<SubrecordStats2> sortedAllSubTypesData = getSortedSubRecs(allSubTypesData.values());

			// make sure the after X is fulfilled
			for (SubrecordStats2 sub : allSubTypesData.values()) {
				// are we always after type of thing?
				if (sub.subTypesBefore.size() == 1) {
					// get the first and only one
					String alwaysAfter = sub.subTypesBefore.iterator().next();
					// now push it back if it's not already
					int subToMoveBackIdx = sortedAllSubTypesData.indexOf(sub);
					for (int i = 0; i < sortedAllSubTypesData.size(); i++) {
						//System.out.print("looking at i " + i + " which is a  " + sortedAllSubTypesData.get(i).subrecordType );	
						if (sortedAllSubTypesData.get(i).subrecordType.equals(alwaysAfter)) {
							// have we got to the one before first, if so all is well
							if (i > subToMoveBackIdx) {
								sortedAllSubTypesData.remove(subToMoveBackIdx);
								// note everything has just moved up by one so i was i + 1 before the remove above
								if (i < sortedAllSubTypesData.size())
									sortedAllSubTypesData.add(i, sub);
								else
									sortedAllSubTypesData.add(sub);
							}
							break;
						}
					}
				}

			}

			if (WRITE_MD) {
				// for each one build a table
				for (SubrecordStats2 subrecordStats : sortedAllSubTypesData) {
					String sr = "<tr> \n\n" + "<td>" + subrecordStats.C + "</td> \n\n" + "<td>"
								+ subrecordStats.subrecordType + "</td> \n\n" + "<td>" + subrecordStats.dataType
								+ "</td> \n\n" + "<td>" + subrecordStats.desc + "</td> \n\n" + "</tr> \n\n";

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

	public static ArrayList<SubrecordStats2> getSortedSubRecs(Collection<SubrecordStats2> collection) {
		ArrayList<SubrecordStats2> sorted = new ArrayList<SubrecordStats2>(collection);
		Collections.sort(sorted, new Comparator<SubrecordStats2>() {
			@Override
			public int compare(SubrecordStats2 a, SubrecordStats2 b) {
				if (a.maxSubPos < b.maxSubPos)
					return -1;
				else if (a.maxSubPos == b.maxSubPos)
					return a.minSubPos < b.minSubPos ? -1 : a.minSubPos == b.minSubPos ? 0 : 1;
				else
					return 1;

			}
		});

		return sorted;
	}

	private static void createJavaClass(RecordData recordData, String desc,
										ArrayList<SubrecordStats2> sortedAllSubTypesData)
			throws IOException {

		File javaRECOFolder = new File(outputFolder, esmFileName.substring(0, esmFileName.lastIndexOf(".")));
		javaRECOFolder.mkdirs();

		File javaClassOut = new File(javaRECOFolder, recordData.type + ".java");
		if (!javaClassOut.exists())
			javaClassOut.createNewFile();
		csvOut = new BufferedWriter(new FileWriter(javaClassOut));

		/**
		 * Concept Create a compilable java class, that I can open a project with adn load an esm of the appropriate
		 * version each game version will ahve a package space in this test project load the esm into java classes with
		 * members and zero erros, then fire every esm at it.
		 * 
		 * after all esm incl 76 are working perfectly, then integration with current code can happen (member names
		 * getting modded etc)
		 * 
		 * Possibly I could make a custom editor for each version to make the editor a bit sexier to make them all build?
		 * 
		 * note morrowind not currently going
		 */

		/*recordData.type;
		
		//for each one build a table
		for (SubrecordStats2 subrecordStats : sortedAllSubTypesData)
		{				
			String sr = "<tr> \n\n" +
			"<td>"+subrecordStats.C+"</td> \n\n" +
			"<td>"+subrecordStats.subrecordType+"</td> \n\n" +
			"<td>"+subrecordStats.dataType+"</td> \n\n" +
			"<td>"+subrecordStats.desc+"</td> \n\n" +
			"</tr> \n\n";
			 	 			
			mdOut.append(sr);		
		}*/
	}

}