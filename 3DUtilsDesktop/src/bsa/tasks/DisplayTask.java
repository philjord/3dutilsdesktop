package bsa.tasks;

import java.util.ArrayDeque;
import java.util.List;

import javax.swing.SwingUtilities;

import org.jogamp.java3d.Texture;

import bsa.BSAToolMain;
import bsa.gui.BSAFileSetWithStatus;
import bsa.gui.StatusDialog;
import bsa.source.BsaMeshSource;
import bsa.source.BsaTextureSource;
import bsa.source.BsaTextureSource.AllowedTextureFormats;
import bsaio.ArchiveEntry;
import bsaio.displayables.Displayable;
import compressedtexture.DDSImage;
import nif.NifJ3dVisRoot;
import nif.NifToJ3d;
import nif.character.KfJ3dRoot;
import nif.gui.NifDisplayTester;
import texture.CompressedTextureLoaderExt;
import texture.Texture2DDisplay;
import utils.source.DummyTextureSource;

public class DisplayTask extends Thread {

	private static final int		MAX_NIF_CANVAS3D_VISIBLE	= 3;

	private BSAFileSetWithStatus	bsaFileSet;

	private List<ArchiveEntry>		entries;

	private StatusDialog			statusDialog;

	private boolean					verifyOnly;
	private boolean					together;

	private boolean					sopErrOnly;
	private boolean					completed;

	public DisplayTask(	BSAFileSetWithStatus bsaFileSet, List<ArchiveEntry> entries, StatusDialog statusDialog,
						boolean verifyOnly, boolean together, boolean sopErrOnly) {
		completed = false;
		this.bsaFileSet = bsaFileSet;
		this.entries = entries;
		this.statusDialog = statusDialog;
		this.verifyOnly = verifyOnly;
		this.together = together;
		this.sopErrOnly = sopErrOnly;
	}

	@Override
	public void run() {

		statusDialog.updateMessage("" + (verifyOnly ? "Verifying files" : "Displaying Files"));
		statusDialog.updateProgress(0);

		int fileCount = entries.size();
		int filesProcessCount = 0;
		float currentProgress = 0;

		if (!together) {
			for (ArchiveEntry entry : entries) {
				try {
					//InputStream in = ((Displayable)entry).getArchiveFile().getInputStream(entry);

					String fileName = ((Displayable)entry).getName();

					int sep = fileName.lastIndexOf('.');
					if (sep >= 0) {
						String ext = fileName.substring(sep);
						DDSImage.OUTPUT_IMAGE_DEBUG = false;//turn off, only for dds files
						//FO76 .bto is a nif lod
						if (ext.equals(".nif") || ext.equals(".bto")) {
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
							} else {
								getNifDisplayer().showNif(fileName, new BsaMeshSource(bsaFileSet));
							}
							//always clear in case same names appears in 2 different bsa files (fo4vfo76)
							NifToJ3d.clearCache();
						} else if (ext.equals(".dds") || ext.equals(".ktx")) {
							// if we are looking at an image we probably wants its info
							DDSImage.OUTPUT_IMAGE_DEBUG = true;
							if (verifyOnly) {
								Texture tex = new BsaTextureSource(bsaFileSet).getTexture(fileName);
								if (tex != null) {
									if (!sopErrOnly) {
										System.out.println("verified: " + fileName);
									}
								} else {
									System.out.println("issue: " + fileName);
								}
							} else {
								// when showing an image, we need to ignore extension swapping, cos we only have 1 bsa and 1 possible type						
								AllowedTextureFormats prevAllowedTextureType = BsaTextureSource.allowedTextureFormats;
								BsaTextureSource.allowedTextureFormats = AllowedTextureFormats.ALL;
								Texture2DDisplay.showImageInShape(fileName,
										new BsaTextureSource(bsaFileSet).getTexture(fileName));
								// now set it back
								BsaTextureSource.allowedTextureFormats = prevAllowedTextureType;
							}
							//always clear in case same names appears in 2 different bsa files (fo4vfo76)
							CompressedTextureLoaderExt.clearCache();
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
		} else {
			//together, for nifs only
			try {			 
				DDSImage.OUTPUT_IMAGE_DEBUG = false;//turn off, only for dds files

				getNifDisplayer().showNifs(entries, new BsaMeshSource(bsaFileSet));
				//always clear in case same names appears in 2 different bsa files (fo4vfo76)
				NifToJ3d.clearCache();

				filesProcessCount++;
				float newProgress = filesProcessCount / (float)fileCount;

				if ((newProgress - currentProgress) > 0.01) {
					statusDialog.updateMessage( "Displaying many nifs");
					statusDialog.updateProgress((filesProcessCount * 100) / fileCount);
					currentProgress = newProgress;
				}				
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

	private static ArrayDeque<NifDisplayTester> nifDisplays = new ArrayDeque<NifDisplayTester>();

	private NifDisplayTester getNifDisplayer() {

		// up to 5
		while (nifDisplays.size() > MAX_NIF_CANVAS3D_VISIBLE - 1) {
			NifDisplayTester ndt = nifDisplays.removeFirst();
			ndt.close();
		}
		NifDisplayTester nifDisplay = new NifDisplayTester(bsaFileSet);
		nifDisplays.addLast(nifDisplay);

		return nifDisplay;
	}
}