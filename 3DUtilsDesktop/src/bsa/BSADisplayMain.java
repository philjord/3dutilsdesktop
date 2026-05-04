package bsa;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import bsa.gui.BSAContentDisplay;

public class BSADisplayMain extends BSAToolMain {
	public static void main(String args[]) {
		try {
			fileSeparator = System.getProperty("file.separator");
			lineSeparator = System.getProperty("line.separator");
			tmpDir = System.getProperty("java.io.tmpdir");
			String option = System.getProperty("UseShellFolder");
			if (option != null && option.equals("0"))
				useShellFolder = false;
			String filePath = (new StringBuilder()).append(System.getProperty("user.home")).append(fileSeparator)
					.append("Application Data").append(fileSeparator).append("ScripterRon").toString();
			File dirFile = new File(filePath);
			if (!dirFile.exists())
				dirFile.mkdirs();
			filePath = (new StringBuilder()).append(filePath).append(fileSeparator).append("FO3Archive.properties")
					.toString();
			propFile = new File(filePath);
			properties = new Properties();
			if (propFile.exists()) {
				FileInputStream in = new FileInputStream(propFile);
				properties.load(in);
				in.close();
			}
			properties.setProperty("java.version", System.getProperty("java.version"));
			properties.setProperty("java.home", System.getProperty("java.home"));
			properties.setProperty("os.name", System.getProperty("os.name"));
			properties.setProperty("sun.os.patch.level", System.getProperty("sun.os.patch.level"));
			properties.setProperty("user.name", System.getProperty("user.name"));
			properties.setProperty("user.home", System.getProperty("user.home"));
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					createAndShowGUI();
				}
			});
		} catch (Throwable exc) {
			logException("Exception during program initialization", exc);
		}
	}

	public static String	ARCHIVE_TO_AUTO_OPEN	=								// 
			"D:\\game_media\\Starfield\\Data\\Starfield - Meshes01.ba2";
	//null;
	public static String[] 	FILEPATH_TO_ACTION			=								//
			//new String[] {"meshes","setdressing","akila","ak_soccer","ak_soccer_ball_01.nif"};	
			new String[] {"meshes","setdressing","akila","ak_bag_produce01","ak_bag_produce01_close.nif"};
			//new String[] {"meshes","architecture","catwalks","industrial","catindwalksmstaira_lgbot_5m.nif"};	
			//null;

	public static void createAndShowGUI() {
		try {
			mainWindow = new BSAContentDisplay();
			mainWindow.pack();
			mainWindow.setLocationRelativeTo(null);
			mainWindow.setVisible(true);
			if (ARCHIVE_TO_AUTO_OPEN == null) {
				//might as well open an archive, not much will happen otherwise
				((BSAContentDisplay)mainWindow).actionPerformed(new ActionEvent(mainWindow, -1, "open"));
			} else {
				//For testing purposes we can place bsa fiel selection then nif file clicking here
				File arhcive = new File(ARCHIVE_TO_AUTO_OPEN);
				((BSAContentDisplay)mainWindow).openArchive(arhcive);
				//synchronous call so now loaded and I can fire the file double click now
				if (FILEPATH_TO_ACTION != null) {
					((BSAContentDisplay)mainWindow).setSelectedNode(FILEPATH_TO_ACTION);
					((BSAContentDisplay)mainWindow).displayFiles(false, false);
				}
			}

		} catch (Throwable exc) {
			logException("Exception while initializing application window", exc);
		}
	}
}
