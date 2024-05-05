package esm.analyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
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
	public static String					OUTPUT_FILE_KEY			= "outputFile";

	public static boolean					ANALYZE_CELLS			= true;

	public static BufferedWriter 			out;

	public static Plugin					pluginToAnalyze;

	public static Preferences				prefs;
	
	public static int						currentRowOrderNum				= 0;
	
	public static String 					esmFileName = "unknown";

	public static void main(String args[]) {
		String generalEsmFile = EsmFileLocations.getGeneralEsmFile();

		// this is for csv full details output 
		if (generalEsmFile != null) {
			prefs = Preferences.userNodeForPackage(EsmMetaDataToCsv.class);
			File csvFolder = TitledJFileChooser.requestFolderName("Select Output Folder", prefs.get(OUTPUT_FILE_KEY, ""), null);

			if (csvFolder != null) {
				prefs.put(OUTPUT_FILE_KEY, csvFolder.getAbsolutePath());
				esmFileName = generalEsmFile.substring(generalEsmFile.lastIndexOf("\\") + 1);
				try {
					File csv = new File(csvFolder, esmFileName + ".csv");
					if(!csv.exists())
						csv.createNewFile();
					out = new BufferedWriter(new FileWriter(csv));
					long startTime = System.currentTimeMillis();
					System.out.println("loading file "	+ generalEsmFile);
	
					File pluginFile = new File(generalEsmFile);
					Plugin plugin = new PluginFile(pluginFile);
				
					plugin.load(false);
					
					
					//oddly form map isn't populated, not sure why I'll do it now
					List<FormInfo> allForms = plugin.getFormList();
					for(FormInfo fi : allForms) {
						plugin.getFormMap().put(fi.getFormID(), fi);
					}
					
					
					analzePlugin(plugin);
					System.out.println("Finished loading in " + (System.currentTimeMillis() - startTime));
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

	private static void analzePlugin(Plugin plugin) {
		pluginToAnalyze = plugin;
		try {
			
			// output header row
			out.append("num,");
			out.append("EsmFileName,");
			out.append("RecordType,");
			out.append("FormID,");
			out.append("RecordFlags1,");
			out.append("RecordFlags2,");
			out.append("EditorID,");
			out.append("SubrecordType,");
			out.append("SubOrder,");
			out.append("DataLen,");
			out.append("CouldBeFormID,");						
			out.append("CouldBeString,"); 							
			
			out.newLine();
			for (PluginGroup group : pluginToAnalyze.getGroupList()) {
				analzeGroupChildren(group);
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
				if (group.getGroupType() == 0) {
					String groupRecordType = group.getGroupRecordType();
					
					// to ease the burden slightly skip some trivial ones, negate this to get them decoded
					if(skipit(groupRecordType))
						continue;					
					
					String editorID = record.getEditorID();
					int formID = record.getFormID();					
					int recordFlags1 = record.getRecordFlags1();
					int recordFlags2 = record.getRecordFlags2();
					
					
					int currentSubOrderNum = 0;
					List<Subrecord> subs = record.getSubrecords();
					for (int i = 0; i < subs.size(); i++)
					{
						Subrecord sub = subs.get(i);
						
						// skip the editorid we know about that guy
						if (sub.getSubrecordType().equals("EDID"))
							continue;
											
						
						//RECO header info, repeated everytime
						out.append(""+currentRowOrderNum++);
						out.append(",");
						out.append(esmFileName);
						out.append(",");
						out.append(groupRecordType);
						out.append(",");
						out.append(""+formID);
						out.append(",");
						out.append(""+recordFlags1);
						out.append(",");
						out.append(""+recordFlags2);
						out.append(",");
						out.append(editorID);// this has no spaces 
						out.append(",");						
						out.append(sub.getSubrecordType()); 
						out.append(",");
						out.append(""+currentSubOrderNum++);
						out.append(",");
						out.append(""+sub.getSubrecordData().length); 
						out.append(",");
						String couldBeFormID = couldBeFormID(sub.getSubrecordData());
						out.append(couldBeFormID == null ? "" : couldBeFormID); 
						out.append(",");
						String couldBeString = couldBeString(groupRecordType, sub.getSubrecordType(), sub.getSubrecordData());
						out.append(couldBeString == null ? "" : escape(couldBeString)); 							
																	
						
						out.newLine();
					}
					
					

					
				}
			}

		}

	}
	
	
	private static boolean skipit(String recType) {
		if (esmFileName.equals("Fallout4.esm")
			&& (recType.equals("GMST") || recType.equals("KYWD") || recType.equals("LCRT") || recType.equals("GLOB")))
			return true;
		
		
		
		return false;
	}
	
	//https://stackoverflow.com/questions/6377454/escaping-tricky-string-to-csv-format
	private static String escape(String str) {
	 
		boolean mustQuote = (str.contains(",") || str.contains("\"") || str.contains("\r") || str.contains("\n"));
	    if (mustQuote)
	    {
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
			int possFormId = ESMByteConvert.extractInt3(bs, 0);
			if (possFormId >= 0 || possFormId < 1000000) {
				FormInfo fi = pluginToAnalyze.getFormMap().get(possFormId);
				if(fi != null)
					return fi.getRecordType();
			}
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
		if(recType.equals("NPC_") && subtype.equals("HCLR") && bs.length == 4 || // color4 of bytes
				recType.equals("REGN") && subtype.equals("RCLR") && bs.length == 4 || // color4 of bytes
				recType.equals("ACTI") && subtype.equals("FULL") && bs.length == 2 ||
				recType.equals("TERM") && subtype.equals("ITXT") && bs.length == 3 || 
				recType.equals("QUST") && subtype.equals("INDX") && bs.length == 2
				) 
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

}