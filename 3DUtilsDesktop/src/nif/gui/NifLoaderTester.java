package nif.gui;

import java.io.File;
import java.util.HashSet;
import java.util.prefs.Preferences;

import javax.swing.filechooser.FileNameExtensionFilter;

import org.jogamp.java3d.compressedtexture.CompressedTextureLoader;

import nif.BgsmSource;
import nif.NifFile;
import nif.NifToJ3d;
import nif.appearance.NiGeometryAppearanceFactoryShader;
import tools.swing.DetailsFileChooser;
import utils.source.DummyTextureSource;
import utils.source.file.FileMeshSource;

public class NifLoaderTester
{
	private static final boolean NO_J3D = true;
	private static Preferences prefs;

	private static int filesProcessed = 0;

	public static void main(String[] args)
	{
		
		System.setProperty("sun.awt.noerasebackground", "true");
		System.setProperty("j3d.cacheAutoComputeBounds", "true");
		System.setProperty("j3d.defaultReadCapability", "false");
		System.setProperty("j3d.defaultNodePickable", "false");
		System.setProperty("j3d.defaultNodeCollidable", "false");
		
		NifToJ3d.SUPPRESS_EXCEPTIONS = false;
		NiGeometryAppearanceFactoryShader.setAsDefault();

		prefs = Preferences.userNodeForPackage(NifLoaderTester.class);
		String baseDir = prefs.get("NifToJ3dTester.baseDir", System.getProperty("user.dir"));

		DetailsFileChooser dfc = new DetailsFileChooser(baseDir, new DetailsFileChooser.Listener() {
			@Override
			public void directorySelected(final File dir)
			{
				prefs.put("NifToJ3dTester.baseDir", dir.getPath());
				Thread t = new Thread() {
					@Override
					public void run()
					{
						processDir(dir);
						System.out.println("Processing " + dir + " complete");

						//h 320204330	 230.75745
						//0001 0011 0001 0101 1110 1110 0010 1010
						//49x81

						//h 353626590	 230.75745
						//0001 0101 0001 0011 1110 1001 1101 1110
						// 1st match 2nd swapped 4th 3rd match  5th match 6-8 diff
						//OR! first 2 shorts swapped?

						//h 204075464	 230.75745
						//0000 ‭1100 0010 1001 1111 0001 1100 1000‬‬
						//h 320141420	 230.75745
						//‭0001 0011 0001 0100 1111 1000 0110 1100‬

						//h 369356854	 230.75528
						//0001 0101 1101 0101 1110 1000 0101 0010
						//81x93

						//h 366340178	 230.60962
						//0001 0110 0000 0011 1111 0000 0011 0110

						//h 176352978	 31.892433 - tiny
						//h 181202546	 6.069578
						//h 107668114	 11.071576
						//h 35514122	 7.4340568
						//h 54978104	 16.396296 h 374272794	 16.677258 h 100727290	 17.362154
						//h 7070944	 390.14697 - massive
						//h 387018	 752.0883

						//h 85123410	 58.19371
						//‭0000 0101 0001 0010 1110 0001 0101 0010							
						//80x33‬

						//h 404683870	 60.13027
						//‭0001 1000 0001 1110 1111 1100 0101 1110
						//129x225‬

						//h 67428454	 58.424664
						//h 72212614	 59.648174
						//h 326235942	 63.274323

						//		r = 4.6251073 h = 37620742 hyp = 9.250215 hyp285.56647
						//		o.bs[0] 6 o.bs[1] 12 o.bs[2] 62 o.bs[3] 2
						//		s1 3078 s2 15884 f1 2.4557114E-4 f2 1.5117188
						//		r = 4.6250944 h = 37621146 hyp = 9.250189 hyp285.565994
						//		o.bs[0] 154 o.bs[1] 13 o.bs[2] 62 o.bs[3] 2
						//		s1 3482 s2 15885 f1 3.4189224E-4 f2 1.5126953

						//		r = 4.6250653 h = 363805036 hyp = 9.250131 hyp285.56492
						//		o.bs[0] 108 o.bs[1] 57 o.bs[2] 175 o.bs[3] 21
						//		s1 14700 s2 -20679 f1 0.6777344 f2 -0.112854004

						//		r = 4.5707054 h = 35522938 hyp = 9.141411 hyp283.56539
						//		o.bs[0] 122 o.bs[1] 9 o.bs[2] 30 o.bs[3] 2
						//		s1 2426 s2 7689 f1 1.6713142E-4 f2 0.0058937073

				/*		for (int h : BSPackedCombinedSharedGeomDataExtra.hashToRadius.keySet())
						{
							//System.out.println("h " + h + "\t "+BSPackedCombinedSharedGeomDataExtra.hashToRadius.get(h));
							float r = BSPackedCombinedSharedGeomDataExtra.hashToRadius.get(h);
							BSPackedGeomObject o = BSPackedCombinedSharedGeomDataExtra.hashToUnknown.get(h);
							if (r < 10)
							{
								System.out.println("r = " + r + " h = " + h + " hyp = " + (r + r) + " hyp2" + ((r + r) * (r + r)));
								System.out.println("o.bs[0] " + (o.bs[0] & 0xff) + " o.bs[1] " + (o.bs[1] & 0xff) + " o.bs[2] "
										+ (o.bs[2] & 0xff) + " o.bs[3] " + (o.bs[3] & 0xff));

								System.out.println("o.bs[0] " + ((o.bs[0] / 255.0f) * 2.0f - 1.0f) + " o.bs[1] "
										+ ((o.bs[1] / 255.0f) * 2.0f - 1.0f) + " o.bs[2] " + ((o.bs[2] / 255.0f) * 2.0f - 1.0f)
										+ " o.bs[3] " + ((o.bs[3] / 255.0f) * 2.0f - 1.0f));
								System.out.println("s1 " + o.s1 + " s2 " + o.s2 + " f1 " + o.f1 + " f2 " + o.f2);
							}
						}*/

						/*	System.out.println("formats");						 
							for (Entry<String, Integer> e : BSTriShape.allFormatToCount.entrySet())
							{
								System.out.println("format " + e.getKey() + " count " + e.getValue());
							}
							
							System.out.println("In the presence of 7 & 0x01 == 1");
							System.out.println("flags7ToSizeDisagreements " + BSTriShape.flags7ToSizeDisagreements);
							for (Entry<Integer, Integer> e : BSTriShape.flags7ToSize.entrySet())
							{
								System.out.println("flag " + e.getKey() + " size " + e.getValue());
							}*/

						/*	for (Entry<String, Vec3f> e : BSPackedCombinedSharedGeomDataExtra.fileHashToMin.entrySet())
							{
								Vec3f min = e.getValue();
								Vec3f max = BSPackedCombinedSharedGeomDataExtra.fileHashToMax.get(e.getKey());
								Vec3f diff = new Vec3f(max);
								diff.sub(min);
								System.out.println("name " + e.getKey() + " min " + min + "\tmax " + max + " " + diff);
							}*/

						/*System.out.println(" processed  " + filesProcessed + "total files");
						System.out.println("countWithHavokRoot " + countWithHavokRoot);
						System.out.println("sizeWithHavokRoot " + sizeWithHavokRoot);
						System.out.println("countPhysics " + countPhysics);
						System.out.println("sizePhysics " + sizePhysics);*/

						//	System.out.println("files.size() " + files.size());

					}

				};
				t.start();
			}

			@Override
			public void fileSelected(File file)
			{
				prefs.put("NifToJ3dTester.baseDir", file.getPath());
				processFile(file);
			}
		});

		dfc.setFileFilter(new FileNameExtensionFilter("Nif", "nif"));

	}

	public static HashSet<String> files = new HashSet<String>();
	public static int countWithHavokRoot = 0;
	public static long sizeWithHavokRoot = 0;
	public static int countPhysics = 0;
	public static long sizePhysics = 0;

	private static void processFile(File f)
	{
		try
		{
			//System.out.println("\tFile: " + f);
			// long start = System.currentTimeMillis();
			if (f.getName().endsWith(".kf"))
			{
				NifToJ3d.loadKf(f.getCanonicalPath(), new FileMeshSource());
			}
			else if (f.getName().endsWith(".dds"))
			{
				CompressedTextureLoader.DDS.getTexture(f);
				CompressedTextureLoader.clearCache();
			}
			else
			{
				FileMeshSource fileMeshSource = new FileMeshSource();
				//TODO: this has probably broekn
				BgsmSource.setBgsmSource(null);

				if (NO_J3D)
				{
					if (f.getName().contains("Physics.NIF"))
					{
						countPhysics++;
						sizePhysics += f.length();
					}
					else
					{
						NifFile o = NifToJ3d.loadNiObjects(f.getCanonicalPath(), fileMeshSource);

						/*		for (NiObject nio : o.blocks)
								{
									if (nio instanceof BSPackedCombinedSharedGeomDataExtra)
									{
										BSPackedCombinedSharedGeomDataExtra packed = (BSPackedCombinedSharedGeomDataExtra) nio;
						
										float prevValue = -9999;
						
										Data[] datas = packed.data;
						
										for (int da = 0; da < datas.length; da++)
										{
											Data data = datas[da];
						
											for (int c = 0; c < data.NumCombined; c++)
											{
												Combined combined = data.Combined[c];
												if (prevValue != -9999 && combined.f1 != prevValue)
												{
													System.out
															.println("missed match " + prevValue + " " + combined.f1 + " " + f.getName());
													files.add(f.getName());
												}
												prevValue = combined.f1;
						
											}
										}
						
									}
								}*/
					}

				}
				else
				{
					NifToJ3d.loadHavok(f.getCanonicalPath(), fileMeshSource);
					// NifJ3dVisRoot r =
					NifToJ3d.loadShapes(f.getCanonicalPath(), fileMeshSource, new DummyTextureSource());

					// System.out.println("modelSizes.put(\"\\" + f.getParent().substring(f.getParent().lastIndexOf("\\")) +
					// "\\\\" + f.getName() + "\", "
					// + ((BoundingSphere) r.getVisualRoot().getBounds()).getRadius() + "f);");
				}
			}

			NifToJ3d.clearCache();

			filesProcessed++;
			if (filesProcessed % 1000 == 0)
				System.out.println("FilesProcessed " + filesProcessed);

			// System.out.println(" in " + (System.currentTimeMillis() - start));
		}
		catch (Exception ex)
		{
			System.out.println("Exception in ");
			ex.printStackTrace();
		}
	}

	private static void processDir(File dir)
	{
		// is this dir full of any files we want
		File[] fs = dir.listFiles();
		boolean hasFileOfInterest = false;
		for (int i = 0; i < fs.length; i++)
		{
			String fn = fs[i].getName().toLowerCase();
			if (fs[i].isFile() && (fn.endsWith(".nif") || fn.endsWith(".kf") || fn.endsWith(".dds") || fn.endsWith(".btr")))
			{
				hasFileOfInterest = true;
				break;
			}
		}

		//precombined is 124k of 196k files! and I don't yet parse the geom data anyway
		if (hasFileOfInterest)//&& !dir.getAbsolutePath().toLowerCase().contains("precombined"))
		{
			System.out.println("Processing directory " + dir);
			Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

			for (int i = 0; i < fs.length; i++)
			{
				try
				{
					String fn = fs[i].getName().toLowerCase();
					if (fs[i].isFile() && (fn.endsWith(".nif") || fn.endsWith(".kf") || fn.endsWith(".dds") || fn.endsWith(".btr")))
					{

						// only skels
						// if(!fs[i].getName().toLowerCase().contains("skeleton"))
						// continue;

						processFile(fs[i]);
					}
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		}

		for (int i = 0; i < fs.length; i++)
		{
			try
			{
				if (fs[i].isDirectory())
				{
					processDir(fs[i]);
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}

}