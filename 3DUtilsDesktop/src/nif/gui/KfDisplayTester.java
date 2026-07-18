package nif.gui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import org.jogamp.java3d.Alpha;
import org.jogamp.java3d.AmbientLight;
import org.jogamp.java3d.BoundingSphere;
import org.jogamp.java3d.BranchGroup;
import org.jogamp.java3d.Canvas3D;
import org.jogamp.java3d.DirectionalLight;
import org.jogamp.java3d.Group;
import org.jogamp.java3d.JoglesPipeline;
import org.jogamp.java3d.Light;
import org.jogamp.java3d.Node;
import org.jogamp.java3d.PointLight;
import org.jogamp.java3d.RotationInterpolator;
import org.jogamp.java3d.SpotLight;
import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.TransformGroup;
import org.jogamp.java3d.compressedtexture.CompressedTextureLoader;
import org.jogamp.java3d.utils.shader.Cube;
import org.jogamp.java3d.utils.universe.SimpleUniverse;
import org.jogamp.vecmath.Color3f;
import org.jogamp.vecmath.Point3d;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3f;

import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;

import bsa.gui.BSAFileSetWithStatus;
import bsa.source.BsaMaterialsSource;
import bsa.source.BsaMeshSource;
import bsa.source.BsaTextureSource;
import nif.NifToJ3d;
import nif.appearance.NiGeometryAppearanceFactoryShader;
import nif.character.AttachedParts;
import nif.character.NifCharacter;
import nif.character.NifCharacterTes3;
import nif.character.NifJ3dSkeletonRoot;
import nif.j3d.J3dNiSkinInstance;
import nif.j3d.animation.tes3.J3dNiSequenceStreamHelper;
import nif.j3d.particles.J3dNiParticleSystem;
import nif.shader.NiGeometryAppearanceShader;
import tools.QueryProperties;
import tools.swing.DetailsFileChooser;
import tools.swing.TitledJFileChooser;
import tools3d.camera.simple.SimpleCameraHandler;
import tools3d.utils.scenegraph.SpinTransform;
import utils.source.MaterialsSource;
import utils.source.MediaSources;
import utils.source.MeshSource;
import utils.source.TextureSource;

/**
 * Usage note
 * You must select a skeleton at least
 * If you cancel on skins it will show only bones
 * If you cancel on animation select it will show the bind pose with no animations 
 * @author philip
 *
 */
public class KfDisplayTester {

	public static boolean				ADD_LIGHT_LOCATION_BOX	= false;
	private SimpleCameraHandler			simpleCameraHandler;

	private TransformGroup				spinTransformGroup		= new TransformGroup();

	private TransformGroup				rotateTransformGroup	= new TransformGroup();

	// used for debug moving the model about a bit
	private TransformGroup				modelTransformGroup		= new TransformGroup();
	private BranchGroup					modelGroup				= new BranchGroup();

	private SpinTransform				spinTransform;

	private boolean						showHavok				= false;

	private boolean						showVisual				= true;

	private SimpleUniverse				simpleUniverse;

	private AmbientLight				ambLight;
	private DirectionalLight			dirLight;
	private PointLight					pointLight;
	private SpotLight					spotLight;

	private static MeshSource			meshSource				= null;
	private static TextureSource		textureSource			= null;
	private static BsaMaterialsSource	materialsSource			= null;

	public KfDisplayTester(BSAFileSetWithStatus parentBsaFileSet) {
		// FIXME: the holding of context may add speed but it causes the pipeline to not call releaseContext on each update pass
		// on the GLWindow Surface so the GLWindow setVisible(false) won't remove it
		// and it can't be destroyed, to fix this issue at the least stopping renderer should force a releaseCtx on the pipeline		
		JoglesPipeline.LATE_RELEASE_CONTEXT = false;

		//DDS requires no installed java3D
		if (QueryProperties.checkForInstalledJ3d()) {
			System.err.println("//DDS requires no installed java3D");
		}
		NifToJ3d.SUPPRESS_EXCEPTIONS = false;

		NiGeometryAppearanceFactoryShader.setAsDefault();
		//FileMediaRoots.setMediaRoots(new String[]{"E:\\Java\\dsstexturesconvert"});

		// only load resources once 
		if (textureSource == null) {
			BSAFileSetWithStatus bsaFileSet;
			if (parentBsaFileSet == null) {
				//Test for android
				//BSArchiveSet bsaFileSet = new BSArchiveSet(new String[] { "F:\\game_media\\Oblivion" }, true, false);
				bsaFileSet = new BSAFileSetWithStatus(new String[] { //
					"D:\\game_media\\Morrowind", //use the newer one with a few bits extra in it
					"D:\\game_media\\Oblivion", //
					"D:\\game_media\\Fallout3", //
					"D:\\game_media\\FalloutNV", //
					"D:\\game_media\\Skyrim", //
					"D:\\game_media\\Fallout4", //
					"D:\\game_media\\Fallout76", //
					"D:\\game_media\\Starfield", //
				}, true, false);
			} else {
				// must create a new set that includes the sibling texture bsas
				bsaFileSet = new BSAFileSetWithStatus(new String[] {parentBsaFileSet.getName()}, true, false);
			}

			textureSource = new BsaTextureSource(bsaFileSet);
			materialsSource = new BsaMaterialsSource(bsaFileSet);
			meshSource = new BsaMeshSource(bsaFileSet);

			//TODO: clean up this stupid
			MaterialsSource.setBgsmSource(materialsSource);
			MeshSource.setMeshSource(meshSource);
		}

		// for gotye where the texture only appear in the textures folder not in a bsa use this one
		//textureSource = new FileTextureSource();

		NiGeometryAppearanceShader.OUTPUT_BINDINGS = false;

		//win.setVisible(true);
		//win.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		final Canvas3D canvas3D = new Canvas3D();

		canvas3D.getGLWindow().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					System.exit(0);
				} else if (e.getKeyCode() == KeyEvent.VK_1) {
					ambLight.setEnable(!ambLight.getEnable());
					System.out.println("ambLight " + ambLight.getEnable());
				} else if (e.getKeyCode() == KeyEvent.VK_2) {
					dirLight.setEnable(!dirLight.getEnable());
					System.out.println("dirLight " + dirLight.getEnable());
				} else if (e.getKeyCode() == KeyEvent.VK_3) {
					pointLight.setEnable(!pointLight.getEnable());
					System.out.println("pointLight " + pointLight.getEnable());
				} else if (e.getKeyCode() == KeyEvent.VK_4) {
					spotLight.setEnable(!spotLight.getEnable());
					System.out.println("spotLight " + spotLight.getEnable());
				}
			}
		});

		canvas3D.getGLWindow().addWindowListener(new WindowAdapter() {
			@Override
			public void windowResized(final WindowEvent e) {
				J3dNiParticleSystem.setScreenWidth(canvas3D.getGLWindow().getWidth());
			}
		});
		J3dNiParticleSystem.setScreenWidth(canvas3D.getGLWindow().getWidth());
		//J3dNiParticleSystem.setSHOW_DEBUG_LINES(true);// H to toggle

		simpleUniverse = new SimpleUniverse(canvas3D);

		//FIXME: I should record the last location and size and reuse them if they are sensible
		canvas3D.getGLWindow().setSize(800, 600);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		canvas3D.getGLWindow().setPosition((screenSize.width / 2) - (canvas3D.getGLWindow().getWidth() / 2),
				(screenSize.height / 2) - (canvas3D.getGLWindow().getHeight() / 2));
		CompressedTextureLoader.setAnisotropicFilterDegree(8);

		canvas3D.addNotify();

		spinTransformGroup.addChild(rotateTransformGroup);
		rotateTransformGroup.addChild(modelTransformGroup);
		// debug move ita bout a bit
		Transform3D t = new Transform3D();
		t.setTranslation(new Vector3f(0, 0, 0));
		modelTransformGroup.setTransform(t);
		modelTransformGroup.addChild(modelGroup);

		simpleCameraHandler = new SimpleCameraHandler(simpleUniverse.getViewingPlatform(), simpleUniverse.getCanvas(),
				modelGroup, rotateTransformGroup, false);

		spinTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		spinTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

		modelGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		modelGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);

		// Create ambient light	and add it
		Color3f alColor = new Color3f(0.5f, 0.5f, 0.5f);
		ambLight = new AmbientLight(true, alColor);
		ambLight.setCapability(Light.ALLOW_INFLUENCING_BOUNDS_WRITE);
		ambLight.setCapability(Light.ALLOW_STATE_WRITE);
		ambLight.setEnable(true);
		ambLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));

		Color3f dlColor = new Color3f(0.9f, 0.9f, 0.85f);//slightly yellow
		dirLight = new DirectionalLight(true, dlColor, new Vector3f(0f, -1f, 0f));
		dirLight.setCapability(Light.ALLOW_INFLUENCING_BOUNDS_WRITE);
		dirLight.setCapability(Light.ALLOW_STATE_WRITE);
		dirLight.setEnable(true);
		dirLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));

		BranchGroup bg = new BranchGroup();

		bg.addChild(ambLight);
		bg.addChild(dirLight);

		//static point light
		/*Color3f plColor = new Color3f(1.0f, 0.95f, 0.95f);
		//Color3f plColor = new Color3f(0.4f, 0.4f, 0.7f);
		PointLight pLight = new PointLight(true, plColor, new Point3f(0f, 0f, 0f), new Point3f(1f, 1f, 0f));
		pLight.setCapability(Light.ALLOW_INFLUENCING_BOUNDS_WRITE);
		pLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
		
		TransformGroup tg = new TransformGroup();
		// light is above like nifskope
		Transform3D t = new Transform3D(new Quat4f(0, 0, 0, 1), new Vector3f(0, 10, 0), 1);
		tg.setTransform(t);
		tg.addChild(new Cube(0.1f));
		tg.addChild(pLight);
		bg.addChild(tg);*/

		// Create a spinning point light		
		TransformGroup l1RotTrans = new TransformGroup();
		l1RotTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		Transform3D t2 = new Transform3D();
		Vector3f lPos1 = new Vector3f(0.0f, 0.0f, 2.0f);
		t2.set(lPos1);
		TransformGroup l1Trans = new TransformGroup(t2);
		l1RotTrans.addChild(l1Trans);

		Color3f lColor1 = new Color3f(0.6f, 0.6f, 0.9f);
		pointLight = new PointLight(true, lColor1, new Point3f(0f, 0f, 0f), new Point3f(0f, 1.1f, 0f));
		pointLight.setCapability(Light.ALLOW_INFLUENCING_BOUNDS_WRITE);
		pointLight.setCapability(Light.ALLOW_STATE_WRITE);
		pointLight.setEnable(true);
		pointLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
		l1Trans.addChild(pointLight);
		if (ADD_LIGHT_LOCATION_BOX)
			l1Trans.addChild(new Cube(0.01f, lColor1.x, lColor1.y, lColor1.z));

		/*Appearance appL1 = new SimpleShaderAppearance(false, false);
		ColoringAttributes caL1 = new ColoringAttributes();
		caL1.setColor(lColor1);
		appL1.setColoringAttributes(caL1);
		l1Trans.addChild(new Sphere(0.02f, appL1));//oddly refuse to show anything?*/
		//		l1Trans.addChild(new Cube(0.01f));

		bg.addChild(l1RotTrans);

		Transform3D yAxis = new Transform3D();
		Alpha rotor1Alpha = new Alpha(-1, Alpha.INCREASING_ENABLE, 0, 0, 10000, 0, 0, 0, 0, 0);
		RotationInterpolator rotator1 = new RotationInterpolator(rotor1Alpha, l1RotTrans, yAxis, 0.0f,
				(float)Math.PI * 2.0f);
		rotator1.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
		l1RotTrans.addChild(rotator1);

		// Create a spinning point light		
		TransformGroup l2RotTrans = new TransformGroup();
		l2RotTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		Transform3D t3 = new Transform3D();
		Vector3f lPos2 = new Vector3f(0.0f, 0.0f, 2.5f);
		t3.set(lPos2);
		TransformGroup l2Trans = new TransformGroup(t3);
		l2RotTrans.addChild(l2Trans);

		Color3f lColor2 = new Color3f(0.6f, 0.9f, 0.6f);
		//Note default_ffp shader doesn't do spot lights yet
		spotLight = new SpotLight(true, lColor2, new Point3f(0f, 0f, 0f), new Point3f(2f, 0f, 0f),
				new Vector3f(0f, -1f, 0f), (float)(Math.PI / 8f), 48f);
		spotLight.setCapability(Light.ALLOW_INFLUENCING_BOUNDS_WRITE);
		spotLight.setCapability(Light.ALLOW_STATE_WRITE);
		spotLight.setEnable(true);
		spotLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
		l2Trans.addChild(spotLight);
		if (ADD_LIGHT_LOCATION_BOX)
			l2Trans.addChild(new Cube(0.01f, lColor2.x, lColor2.y, lColor2.z));

		bg.addChild(l2RotTrans);

		Transform3D yAxis2 = new Transform3D();
		yAxis2.rotZ(Math.PI / 2f);
		Alpha rotor2Alpha = new Alpha(-1, Alpha.INCREASING_ENABLE, 0, 0, 30000, 0, 0, 0, 0, 0);
		RotationInterpolator rotator2 = new RotationInterpolator(rotor2Alpha, l2RotTrans, yAxis2, 0.0f,
				(float)Math.PI * 2.0f);
		rotator2.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
		l2RotTrans.addChild(rotator2);

		bg.addChild(simpleCameraHandler);

		//bg.addChild(fileManageBehavior);

		bg.addChild(spinTransformGroup);
		spinTransform = new SpinTransform(spinTransformGroup, 0.5);
		spinTransform.setEnable(false);
		bg.addChild(spinTransform);

		bg.addChild(new Cube(0.01f));

		simpleUniverse.addBranchGraph(bg);

		simpleUniverse.getViewer().getView().setBackClipDistance(5000);

		simpleUniverse.getCanvas().getGLWindow().addKeyListener(new KeyHandler());
	}

	public static void clearTextureSource() {
		textureSource = null;
	}

	public void display(String skeletonNifFile, ArrayList<String> skinNifFiles, File kff) {
		display(skeletonNifFile, skinNifFiles, kff != null ? kff.getAbsolutePath() : null);
	}

	public void display(String skeletonNifFile, ArrayList<String> skinNifFiles, String animationFile) {
		modelGroup.removeAllChildren();

		BranchGroup bg = new BranchGroup();
		bg.setCapability(BranchGroup.ALLOW_DETACH);

		NifJ3dSkeletonRoot.showBoneMarkers = true;
		J3dNiSkinInstance.showSkinBoneMarkers = false;//TODO: this doesn't show anything?
		MediaSources mediaSources = new MediaSources(meshSource, textureSource, null);

		ArrayList<String> idleAnimations = new ArrayList<String>();

		if (animationFile != null) {
			idleAnimations.add(animationFile);
		}

		// now add the root to the scene so the controller sequence is live
		NifCharacter nifCharacter = new NifCharacter(skeletonNifFile, skinNifFiles, mediaSources);
		nifCharacter.setIdleAnimations(idleAnimations);
		nifCharacter.setCapability(Node.ALLOW_BOUNDS_READ);
		bg.addChild(nifCharacter);

		modelGroup.addChild(bg);

		simpleCameraHandler.viewBounds(nifCharacter.getBounds());

	}

	/**
	 * Only element 0 of skinNifFiles is passed to the character, via AttachedParts.Root
	 * @param skeletonNifFile
	 * @param skinNifFiles
	 */
	public void displayTes3(String skeletonNifFile, ArrayList<String> skinNifFiles) {
		modelGroup.removeAllChildren();

		BranchGroup bg = new BranchGroup();
		bg.setCapability(BranchGroup.ALLOW_DETACH);

		NifJ3dSkeletonRoot.showBoneMarkers = true;
		J3dNiSkinInstance.showSkinBoneMarkers = false;//TODO: this doesn't show anything?
		MediaSources mediaSources = new MediaSources(meshSource, textureSource, null);

		AttachedParts attachedParts = new AttachedParts();
		attachedParts.addPart(AttachedParts.Part.Root, skinNifFiles.get(0));

		final NifCharacterTes3 nifCharacter = new NifCharacterTes3(skeletonNifFile, attachedParts, mediaSources);
		nifCharacter.setCapability(Node.ALLOW_BOUNDS_READ);
		bg.addChild(nifCharacter);

		modelGroup.addChild(bg);
		simpleCameraHandler.viewBounds(nifCharacter.getBounds());

		// now display in a JFrame all sequences from the kf file for user to pickage
		J3dNiSequenceStreamHelper j3dNiSequenceStreamHelper = nifCharacter.getJ3dNiSequenceStreamHelper();

		seqFrame = new JFrame("Select Sequence");
		seqFrame.setSize(200, 600);
		seqFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		final DefaultTableModel tableModel = new DefaultTableModel(new String[] {"FireName", "Length (ms)",}, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false; // disallow editing of the table
			}

			@Override
			public Class<? extends Object> getColumnClass(int c) {
				return getValueAt(0, c).getClass();
			}
		};

		for (String fireName : j3dNiSequenceStreamHelper.getAllSequences()) {
			long len = j3dNiSequenceStreamHelper.getSequence(fireName).getLengthMS();
			tableModel.addRow(new Object[] {fireName, len});
		}
		final JTable table = new JTable(tableModel);
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				String newAnimation = (String)tableModel
						.getValueAt(table.convertRowIndexToModel(table.getSelectedRow()), 0);
				System.out.println("newAnimation addToQueue " + newAnimation);
				nifCharacter.addToQueue(newAnimation, false);
			}

		});

		table.setRowSorter(new TableRowSorter<DefaultTableModel>(tableModel));

		seqFrame.getContentPane().add(new JScrollPane(table));

		seqFrame.setVisible(true);
	}

	private JFrame seqFrame;

	public void close() {
		Canvas3D c = simpleUniverse.getCanvas();
		c.removeNotify();
		c.getGLWindow().destroy();
		if (seqFrame != null) {
			seqFrame.dispose();
		}
	}

	private class KeyHandler extends KeyAdapter {

		public KeyHandler() {
			/*//TODO:
			System.out.println("H toggle havok display");
			System.out.println("L toggle visual display");
			System.out.println("J toggle spin");
			System.out.println("K toggle animate model");
			System.out.println("P toggle background color");
			System.out.println("Space toggle cycle through files");
			*/
		}

		@Override
		public void keyPressed(KeyEvent e) {
			/*
						if (e.getKeyCode() == KeyEvent.VK_SPACE) {
								toggleCycling();
						} else if (e.getKeyCode() == KeyEvent.VK_H) {
								toggleHavok();
						} else if (e.getKeyCode() == KeyEvent.VK_J) {
								toggleSpin();
						} else if (e.getKeyCode() == KeyEvent.VK_K) {
								toggleAnimateModel();
						} else if (e.getKeyCode() == KeyEvent.VK_L) {
								toggleVisual();
							}
							*/
		}
	}

	//***************************************
	//Below here are the older File System based methods, for cycling through directories etc

	public static void main(String[] args) {
		System.setProperty("sun.awt.noerasebackground", "true");
		System.setProperty("j3d.cacheAutoComputeBounds", "true");
		System.setProperty("j3d.defaultReadCapability", "false");
		System.setProperty("j3d.defaultNodePickable", "false");
		System.setProperty("j3d.defaultNodeCollidable", "false");

		NifToJ3d.SUPPRESS_EXCEPTIONS = false;
		KfDisplayTester nifDisplay = new KfDisplayTester(null);
	}

	public void doOldSkoolDisplay() {
		try {
			// pick the nif model
			Preferences prefs = Preferences.userNodeForPackage(KfDisplayTester.class);
			String baseDir = prefs.get("skeletonNifModelFile", System.getProperty("user.dir"));
			TitledJFileChooser skeletonFc = new TitledJFileChooser(baseDir);
			skeletonFc.setDialogTitle("Select Skeleton");
			skeletonFc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			skeletonFc.setMultiSelectionEnabled(false);
			skeletonFc.setFileFilter(new FileFilter() {
				@Override
				public boolean accept(File f) {
					String fname = f.getName().toLowerCase();
					return f.isDirectory()	|| fname.contains("skeleton")
							|| (fname.contains("xbase_anim") && fname.endsWith(".nif"));
				}

				@Override
				public String getDescription() {
					return "Skeleton Files";
				}
			});

			skeletonFc.showOpenDialog(null);
			String skeletonNifModelFile;
			if (skeletonFc.getSelectedFile() != null) {
				skeletonNifModelFile = skeletonFc.getSelectedFile().getCanonicalPath();
				prefs.put("skeletonNifModelFile", skeletonNifModelFile);

				System.out.println("Selected skeleton file: " + skeletonNifModelFile);

				TitledJFileChooser skinFc = new TitledJFileChooser(skeletonNifModelFile);
				skinFc.setDialogTitle("Select Skin(s)");
				skinFc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				skinFc.setMultiSelectionEnabled(true);
				skinFc.setFileFilter(new FileNameExtensionFilter("Nif files", "nif"));
				skinFc.showOpenDialog(null);

				ArrayList<String> skinNifFiles = new ArrayList<String>();

				if (skinFc.getSelectedFile() != null) {
					File[] skinNifModelFiles = skinFc.getSelectedFiles();

					for (File skinNifModelFile : skinNifModelFiles) {
						System.out.println("Selected skin file : " + skinNifModelFile);
						skinNifFiles.add(skinNifModelFile.getCanonicalPath());
					}
				} else {
					//This is fine, just animate the bones and show them
				}
				if (!skeletonNifModelFile.toLowerCase().contains("morrowind")) {
					DetailsFileChooser dfc = new DetailsFileChooser(skeletonNifModelFile,
							new DetailsFileChooser.Listener() {
								@Override
								public void fileSelected(File file) {
									try {
										System.out.println("\tFile: " + file);
										display(skeletonNifModelFile, skinNifFiles, file);
									} catch (Exception ex) {
										ex.printStackTrace();
									}
								}

								@Override
								public void directorySelected(File dir) {
									//  ignored
								}
							});

					dfc.setFileFilter(new FileNameExtensionFilter("Kf files", "kf"));

				} else {
					//morrowind has a single kf files named after sekeleton
					displayTes3(skeletonNifModelFile, skinNifFiles);
				}
			} else {
				System.exit(0);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}