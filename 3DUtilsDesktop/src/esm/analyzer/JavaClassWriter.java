package esm.analyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import esfilemanager.common.data.plugin.PluginGroup;
import esm.analyzer.EsmMetaDataToCsv.SubRecTuple;

public class JavaClassWriter {
	
	public static void createJavaRECOClass(File outputFolder, String esmFileName, RecordData recordData, String desc, ArrayList<SubRecTuple> tuples)
			throws IOException {

		String recoJavaName = recordData.type;		

		String esmClean = esmFileName.substring(0, esmFileName.lastIndexOf("."));

		String srcFolder = "src" + File.separator + esmClean.toLowerCase();

		File javaRECOFolder = new File(outputFolder, srcFolder);
		javaRECOFolder.mkdirs();

		File javaClassOut = new File(javaRECOFolder, recoJavaName + ".java");

		System.out.println("Making Java class now for " + esmClean + " " + javaClassOut);

		if (!javaClassOut.exists())
			javaClassOut.createNewFile();

		BufferedWriter javaBW = new BufferedWriter(new FileWriter(javaClassOut));

		/**
		 * Concept Create a compilable java class, that I can open a project with and load an esm of the appropriate
		 * version each game version will have a package space in this test project load the esm into java classes with
		 * members and zero errors, then fire every esm at it.
		 * 
		 * after all esm incl 76 are working perfectly, then integration with current code can happen (member names
		 * getting modded etc)
		 * 
		 * Possibly I could make a custom editor for each version to make the editor a bit sexier to make them all
		 * build?
		 * 
		 * 
		 */

		boolean instanceType = PluginGroup.instTypes.contains(recordData.type);

		int tIdx = 0;
		int sIdx = 0;

		javaBW.append("package " + esmClean.toLowerCase() + "; \n");
		javaBW.newLine();
		javaBW.append("import esfilemanager.common.data.record.Record;\n");
		javaBW.append("import esfilemanager.common.data.record.Subrecord;\n");
		javaBW.append("import forms.baseforms.SubRecoHandler;\n");
		javaBW.append("import forms.baseforms." + (instanceType ? "InstForm" : "TypeForm") + ";\n");

		javaBW.newLine();
		javaBW.append(
				"public class " + recoJavaName + " extends " + (instanceType ? "InstForm" : "TypeForm") + " { \n");
		javaBW.newLine();

		//variable declarations here
		for (int t = 0; t < tuples.size(); t++) {
			SubRecTuple tup = tuples.get(t);
			for (int i = 0; i < tup.items.size(); i++) {
				SubrecordStats subrecordStats = tup.items.get(i).stat;
				if (!subrecordStats.subrecordType.equals("EDID")) {
					boolean tupleOptional = tup.items.get(i).optional;

					String sr = "//"	+ subrecordStats.C + " " + subrecordStats.subrecordType + " "
								+ subrecordStats.dataType + " opt " + tupleOptional + " " + subrecordStats.desc + " "
								+ "\n";

					javaBW.append(sr);
				}
			}
		}

		javaBW.newLine();
		javaBW.append("\tpublic " + recoJavaName + "(Record recordData) { \n");
		javaBW.append("\t\tsuper(recordData);\n");
		javaBW.append("\t\tSubRecoHandler srh = new SubRecoHandler(recordData);\n");

		//everything might have an EDID
		javaBW.append("\t\tsetEDID(srh.ifNext(\"EDID\"));\n");

		//TO DEBUG just chuck them out in order  a comments at the bottom
		for (int t = 0; t < tuples.size(); t++) {
			SubRecTuple tup = tuples.get(t);
			for (int i = 0; i < tup.items.size(); i++) {
				SubrecordStats subrecordStats = tup.items.get(i).stat;
				boolean tupleOptional = tup.items.get(i).optional;

				String sr = "//"	+ subrecordStats.C + " " + subrecordStats.subrecordType + " " + subrecordStats.dataType
							+ " opt " + tupleOptional + " " + subrecordStats.desc + " " + "\n";

				javaBW.append(sr);
			}
		}

		javaBW.append("\t} \n");
		javaBW.append("} \n");
		javaBW.flush();
		javaBW.close();

	}
	
	public static void createJavaClass(File outputFolder, String esmFileName, RecordData recordData, String desc, ArrayList<SubRecTuple> tuples)
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

		String srcFolder = "src" + File.separator + esmClean.toLowerCase();

		File javaRECOFolder = new File(outputFolder, srcFolder);
		javaRECOFolder.mkdirs();

		File javaClassOut = new File(javaRECOFolder, betterJavaName + ".java");

		System.out.println("Making Java class now for " + esmClean + " " + javaClassOut);

		if (!javaClassOut.exists())
			javaClassOut.createNewFile();

		BufferedWriter javaBW = new BufferedWriter(new FileWriter(javaClassOut));

		/**
		 * Concept Create a compilable java class, that I can open a project with and load an esm of the appropriate
		 * version each game version will have a package space in this test project load the esm into java classes with
		 * members and zero errors, then fire every esm at it.
		 * 
		 * after all esm incl 76 are working perfectly, then integration with current code can happen (member names
		 * getting modded etc)
		 * 
		 * Possibly I could make a custom editor for each version to make the editor a bit sexier to make them all
		 * build?
		 * 
		 * 
		 */

		boolean instanceType = PluginGroup.instTypes.contains(recordData.type);

		int tIdx = 0;
		int sIdx = 0;

		javaBW.append("package " + esmClean.toLowerCase() + "; \n");
		javaBW.newLine();
		javaBW.append("import esfilemanager.common.data.record.Record;\n");
		javaBW.append("import esfilemanager.common.data.record.Subrecord;\n");
		javaBW.append("import forms.baseforms.SubRecoHandler;\n");
		javaBW.append("import forms.baseforms." + (instanceType ? "InstForm" : "TypeForm") + ";\n");

		javaBW.newLine();
		javaBW.append(
				"public class " + betterJavaName + " extends " + (instanceType ? "InstForm" : "TypeForm") + " { \n");
		javaBW.newLine();

		//variable declarations here
		for (int t = 0; t < tuples.size(); t++) {
			SubRecTuple tup = tuples.get(t);
			for (int i = 0; i < tup.items.size(); i++) {
				SubrecordStats subrecordStats = tup.items.get(i).stat;
				if (!subrecordStats.subrecordType.equals("EDID")) {
					boolean tupleOptional = tup.items.get(i).optional;

					String sr = "//"	+ subrecordStats.C + " " + subrecordStats.subrecordType + " "
								+ subrecordStats.dataType + " opt " + tupleOptional + " " + subrecordStats.desc + " "
								+ "\n";

					javaBW.append(sr);
				}
			}
		}

		javaBW.newLine();
		javaBW.append("\tpublic " + betterJavaName + "(Record recordData) { \n");
		javaBW.append("\t\tsuper(recordData);\n");
		javaBW.append("\t\tSubRecoHandler srh = new SubRecoHandler(recordData);\n");

		//everything might have an EDID
		javaBW.append("\t\tsetEDID(srh.ifNext(\"EDID\"));\n");

		//TO DEBUG just chuck them out in order  a comments at the bottom
		for (int t = 0; t < tuples.size(); t++) {
			SubRecTuple tup = tuples.get(t);
			for (int i = 0; i < tup.items.size(); i++) {
				SubrecordStats subrecordStats = tup.items.get(i).stat;
				boolean tupleOptional = tup.items.get(i).optional;

				String sr = "//"	+ subrecordStats.C + " " + subrecordStats.subrecordType + " " + subrecordStats.dataType
							+ " opt " + tupleOptional + " " + subrecordStats.desc + " " + "\n";

				javaBW.append(sr);
			}
		}

		javaBW.append("\t} \n");
		javaBW.append("} \n");
		javaBW.flush();
		javaBW.close();

	}
}
