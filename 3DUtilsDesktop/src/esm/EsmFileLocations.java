package esm;

import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

public class EsmFileLocations {

	private static String		GENERAL_ESM_FILE	= null;

	private static Preferences	prefs;

	static {
		prefs = Preferences.userNodeForPackage(EsmFileLocations.class);
	}

	public static String getGeneralEsmFile() {
		if (GENERAL_ESM_FILE == null) {
			File f = requestEsmFileName("Select ESM File", prefs.get("General", ""));
			if (f == null) {
				return null;
			} else {
				prefs.put("General", f.getAbsolutePath());
				GENERAL_ESM_FILE = f.getAbsolutePath();
			}
		}

		return GENERAL_ESM_FILE;
	}

	private static File requestEsmFileName(String title, String defaultFile) {
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setSelectedFile(new File(defaultFile));
		fc.setDialogTitle(title);
		fc.setFileFilter(new ESMFileFilter());
		int result = fc.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
			File sf = fc.getSelectedFile();
			return sf;
		} else {
			return null;
		}
	}

	public static class ESMFileFilter extends FileFilter {

		public ESMFileFilter() {
		}

		@Override
		public String getDescription() {
			return "ESM Files (*.esm)";
		}

		@Override
		public boolean accept(File file) {
			boolean accept = false;
			if (file.isFile()) {
				String name = file.getName();
				int sep = name.lastIndexOf('.');
				if (sep > 0) {
					String extension = name.substring(sep);
					if (extension.equalsIgnoreCase(".esm"))
						accept = true;
				}
			} else {
				accept = true;
			}
			return accept;
		}
	}
}
