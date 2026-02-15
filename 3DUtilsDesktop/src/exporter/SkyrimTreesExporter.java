package exporter;

import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

import org.jogamp.java3d.compressedtexture.CompressedTextureLoader;

import bsa.gui.BSAFileSetWithStatus;
import bsaio.ArchiveEntry;
import bsaio.displayables.Displayable;
import esfilemanager.loader.ESMManagerFile;
import esfilemanager.loader.IESMManager;
import esfilemanager.utils.source.EsmSoundKeyToName;
import nif.NifToJ3d;
import nif.appearance.NiGeometryAppearanceFactoryShader;
import scrollsexplorer.PropertyLoader;
import tools.QueryProperties;
import tools.io.ConfigLoader;
import utils.source.MediaSources;

public class SkyrimTreesExporter
{

	public Preferences prefs;

	public static String outputFolderTrees = "D:\\game_media\\output\\skyrimTrees";

	private SkyrimTreesExporter() throws IOException
	{
		NiGeometryAppearanceFactoryShader.setAsDefault();
		CompressedTextureLoader.setAnisotropicFilterDegree(8);
		
		PropertyLoader.load();

		String scrollsFolder = PropertyLoader.properties.getProperty("SkyrimFolder");
		String mainESMFile = scrollsFolder + PropertyLoader.fileSep + "Skyrim.esm";

		IESMManager esmManager = ESMManagerFile.getESMManager(mainESMFile);
		new EsmSoundKeyToName(esmManager);

		BSAFileSetWithStatus bsaFileSet = null;
		BsaRecordedMeshSource meshSource;
		BsaRecordedTextureSource textureSource;
		BsaRecordedSoundSource soundSource;

		String plusSkyrim = PropertyLoader.properties.getProperty("SkyrimFolder");

		bsaFileSet = new BSAFileSetWithStatus(new String[]
		{ plusSkyrim }, true, false);

		meshSource = new BsaRecordedMeshSource(bsaFileSet);
		textureSource = new BsaRecordedTextureSource(bsaFileSet);
		soundSource = new BsaRecordedSoundSource(bsaFileSet, new EsmSoundKeyToName(esmManager));

		MediaSources mediaSources = new MediaSources(meshSource, textureSource, soundSource);

		long startTime = System.currentTimeMillis();
		System.out.println("starting trees/plants nif load and record...");
		// for each cell picked
		for (ArchiveEntry tree : meshSource.getEntriesInFolder("Meshes\\landscape\\trees"))
		{
			System.out.println("Tree: " + tree);
			NifToJ3d.loadNif(((Displayable)tree).getFileName(), meshSource, textureSource);
		}
		
		for (ArchiveEntry tree : meshSource.getEntriesInFolder("Meshes\\landscape\\plants"))
		{
			System.out.println("Plant: " + tree);
			NifToJ3d.loadNif(((Displayable)tree).getFileName(), meshSource, textureSource);
		}
		System.out.println("finished trees/plants load, starting copy");
		File outputFolder = new File(outputFolderTrees);
		try
		{
			ESMBSAExporter.copyToOutput(outputFolder, mediaSources, bsaFileSet);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		System.out.println("Export complete in " + (System.currentTimeMillis() - startTime) + "ms");
	}

	public static void main(String[] args)
	{

		// DDS requires no installed java3D
		if (QueryProperties.checkForInstalledJ3d())
		{
			System.exit(0);
		}

		ConfigLoader.loadConfig(args);
		try
		{
			new SkyrimTreesExporter();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

}
