package bsa.tasks;

import java.util.List;

import javax.swing.SwingUtilities;

import org.jogamp.java3d.Texture;
import org.jogamp.java3d.compressedtexture.CompressedTextureLoader;

import bsa.BSAToolMain;
import bsa.gui.BSAFileSetWithStatus;
import bsa.gui.StatusDialog;
import bsa.source.BsaMeshSource;
import bsa.source.BsaTextureSource;
import bsaio.ArchiveEntry;
import bsaio.displayables.Displayable;
import nif.NifJ3dVisRoot;
import nif.NifToJ3d;
import nif.character.KfJ3dRoot;
import nif.gui.NifDisplayTester;
import texture.Texture2DDisplay;
import utils.source.DummyTextureSource;

public class DisplayTask extends Thread {
	private BSAFileSetWithStatus	bsaFileSet;

	private List<ArchiveEntry>		entries;

	private StatusDialog			statusDialog;

	private boolean					verifyOnly;

	private boolean					sopErrOnly;
	private boolean					completed;

	public DisplayTask(	BSAFileSetWithStatus bsaFileSet, List<ArchiveEntry> entries, StatusDialog statusDialog,
						boolean verifyOnly, boolean sopErrOnly) {
		completed = false;
		this.bsaFileSet = bsaFileSet;
		this.entries = entries;
		this.statusDialog = statusDialog;
		this.verifyOnly = verifyOnly;
		this.sopErrOnly = sopErrOnly;
	}

	@Override
	public void run() {

		statusDialog.updateMessage("" + (verifyOnly ? "Verifying files" : "Displaying Files"));
		statusDialog.updateProgress(0);

		int fileCount = entries.size();
		int filesProcessCount = 0;
		float currentProgress = 0;
		for (ArchiveEntry entry : entries) {
			try {
				//InputStream in = ((Displayable)entry).getArchiveFile().getInputStream(entry);

				String fileName = ((Displayable)entry).getName();

				int sep = fileName.lastIndexOf('.');
				if (sep >= 0) {
					String ext = fileName.substring(sep);
					if (ext.equals(".nif")) {
						if (verifyOnly) {
							NifJ3dVisRoot nr = NifToJ3d.loadShapes(fileName, new BsaMeshSource(bsaFileSet),
									new DummyTextureSource());
							if (nr != null) {
								if (!sopErrOnly) {
									System.out.println("verified: " + fileName);
								}
							} else {
								System.out.println("issue: " + fileName);
							}
							NifToJ3d.clearCache();
						} else {
							getNifDisplayer().showNif(fileName, new BsaMeshSource(bsaFileSet));
						}
					} else if (ext.equals(".dds") || ext.equals(".ktx")) {
						if (verifyOnly) {
							Texture tex = new BsaTextureSource(bsaFileSet).getTexture(fileName);
							if (tex != null) {
								if (!sopErrOnly) {
									System.out.println("verified: " + fileName);
								}
							} else {
								System.out.println("issue: " + fileName);
							}

							CompressedTextureLoader.clearCache();
						} else {
							Texture2DDisplay.showImageInShape(fileName,
									new BsaTextureSource(bsaFileSet).getTexture(fileName));
						}
					} else if (ext.equals(".kf")) {
						//only verify with no skeleton
						KfJ3dRoot kr = NifToJ3d.loadKf(fileName, new BsaMeshSource(bsaFileSet));
						if (kr != null) {
							if (!sopErrOnly) {
								System.out.println("verified: " + fileName);
							}
						} else {
							System.out.println("issue: " + fileName);
						}
						NifToJ3d.clearCache();
					}
					//FIXME: no generic image loading systems anymore
					/*else if (ext.equals(".png"))
					{
						if (verifyOnly)
						{
							BufferedImage bi = SimpleImageLoader.getImage(fileName);
							if (bi != null)
							{
								if (!sopErrOnly)
								{
									System.out.println("verified: " + fileName);
								}
							}
							else
							{
								System.out.println("issue: " + fileName);
							}
						}
						else
						{
							if (!sopErrOnly)
							{
								System.out.println("I would have displayed you a png just now! " + fileName);
							}
						}
					
					}*/
					else if (ext.equals(".wav")) {

						if (!sopErrOnly) {
							System.out.println("I would have played you a wav just now! " + fileName);
						}

					} else if (ext.equals(".lip")) {

						if (!sopErrOnly) {
							System.out.println("display lip file? " + fileName);
						}

					} else if (ext.equals(".mp3")) {

						if (!sopErrOnly) {
							System.out.println("I would have played you a mp3 just now! " + fileName);
						}

					} else if (ext.equals(".ogg")) {

						if (!sopErrOnly) {
							System.out.println("I would have played you a ogg just now! " + fileName);
						}

					} else if (ext.equals(".xml")) {

						if (!sopErrOnly) {
							System.out.println("display xml file? " + fileName);
						}

					} else {
						if (!sopErrOnly) {
							System.out.println("unknown file : " + fileName);
						}
					}
				}

				//in.close();

				filesProcessCount++;
				float newProgress = filesProcessCount / (float)fileCount;

				if ((newProgress - currentProgress) > 0.01) {
					statusDialog.updateMessage("" + (verifyOnly ? "Verifying " : "Displaying ") + fileName);
					statusDialog.updateProgress((filesProcessCount * 100) / fileCount);
					currentProgress = newProgress;
				}
			//} catch (IOException exc) {
			//	BSAToolMain.logException("I/O error while extracting files", exc);
			} catch (Throwable exc) {
				if (verifyOnly) {
					exc.printStackTrace();
				} else {
					BSAToolMain.logException("Exception while extracting files", exc);
				}
			}
		}
		completed = true;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				statusDialog.closeDialog(completed);
			}
		});
	}

	private NifDisplayTester nifDisplay;

	private NifDisplayTester getNifDisplayer() {
		if (nifDisplay == null) {
			nifDisplay = new NifDisplayTester(bsaFileSet);
		}

		return nifDisplay;
	}
}