package nif.gui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.prefs.Preferences;

import javax.swing.filechooser.FileNameExtensionFilter;

import org.jogamp.java3d.Alpha;
import org.jogamp.java3d.AmbientLight;
import org.jogamp.java3d.Behavior;
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
import org.jogamp.java3d.WakeupCondition;
import org.jogamp.java3d.WakeupCriterion;
import org.jogamp.java3d.WakeupOnElapsedFrames;
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
import nif.NifJ3dVisPhysRoot;
import nif.NifToJ3d;
import nif.appearance.NiGeometryAppearanceFactoryShader;
import nif.character.NifJ3dSkeletonRoot;
import nif.gui.util.ControllerInvokerThread;
import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiSkinInstance;
import nif.j3d.animation.J3dNiGeomMorpherController;
import nif.j3d.particles.J3dNiParticleSystem;
import nif.shader.NiGeometryAppearanceShader;
import tools.QueryProperties;
import tools.swing.DetailsFileChooser;
import tools3d.camera.simple.SimpleCameraHandler;
import tools3d.utils.scenegraph.SpinTransform;
import utils.PerFrameUpdateBehavior;
import utils.PerFrameUpdateBehavior.CallBack;
import utils.source.MaterialsSource;
import utils.source.MeshSource;
import utils.source.TextureSource;
import utils.source.file.FileMediaRoots;
import utils.source.file.FileMeshSource;

public class NifDisplayTester {
	private SimpleCameraHandler			simpleCameraHandler;

	private TransformGroup				spinTransformGroup		= new TransformGroup();

	private TransformGroup				rotateTransformGroup	= new TransformGroup();

	// used for debug moving the model about a bit
	private TransformGroup				modelTransformGroup				= new TransformGroup();
	private BranchGroup					modelGroup				= new BranchGroup();

	private SpinTransform				spinTransform;

	private boolean						showHavok				= true;

	private boolean						showVisual				= true;

	private boolean						animateModel			= true;

	private boolean						spin					= false;

	private SimpleUniverse				simpleUniverse;

	private AmbientLight				ambLight;
	private DirectionalLight			dirLight;
	private PointLight					pointLight;
	private SpotLight					spotLight;

	private static MeshSource			meshSource				= null;
	private static TextureSource		textureSource				= null;
	private static BsaMaterialsSource	materialsSource			= null;

	public NifDisplayTester(BSAFileSetWithStatus parentBsaFileSet) {

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

		// only load reasources once 
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

		NiGeometryAppearanceShader.OUTPUT_BINDINGS = true;

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
		/*		GraphicsSettings gs = ScreenResolution.organiseResolution(Preferences.userNodeForPackage(NifDisplayTester.class), win, false, true,
						true);
		
				canvas3D.getView().setSceneAntialiasingEnable(gs.isAaRequired());
				DDSTextureLoader.setAnisotropicFilterDegree(gs.getAnisotropicFilterDegree());
			*/
		//TODO: these must come form a new one of those ^

		//FIXME: I should record the last location and size and reuse them if they are sensible
		canvas3D.getGLWindow().setSize(800, 600);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		canvas3D.getGLWindow().setPosition((screenSize.width / 2) - (canvas3D.getGLWindow().getWidth() / 2),
				(screenSize.height / 2) - (canvas3D.getGLWindow().getHeight() / 2));
		CompressedTextureLoader.setAnisotropicFilterDegree(8);

		//win.setVisible(true);
		canvas3D.addNotify();

		spinTransformGroup.addChild(rotateTransformGroup);
		rotateTransformGroup.addChild(modelTransformGroup);
		// debug move ita bout a bit
		Transform3D t = new Transform3D();
		t.setTranslation(new Vector3f(0,0,0));
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
		pointLight = new PointLight(true, lColor1, new Point3f(0f, 0f, 0f), new Point3f(1f, 0f, 0f));
		pointLight.setCapability(Light.ALLOW_INFLUENCING_BOUNDS_WRITE);
		pointLight.setCapability(Light.ALLOW_STATE_WRITE);
		pointLight.setEnable(true);
		pointLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
		l1Trans.addChild(pointLight);
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

		bg.addChild(fileManageBehavior);

		bg.addChild(spinTransformGroup);
		spinTransform = new SpinTransform(spinTransformGroup, 0.5);
		spinTransform.setEnable(false);
		bg.addChild(spinTransform);

		simpleUniverse.addBranchGraph(bg);

		simpleUniverse.getViewer().getView().setBackClipDistance(5000);

		simpleUniverse.getCanvas().getGLWindow().addKeyListener(new KeyHandler());
		//simpleUniverse.getCanvas().getGLWindow().setPosition(500, 20);

		/*	JMenuBar menuBar = new JMenuBar();
			menuBar.setOpaque(true);
			JMenu menu = new JMenu("File");
			menu.setMnemonic(70);
			menuBar.add(menu);
		
			menu.add(setGraphics);
			setGraphics.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0)
				{
					GraphicsSettings gs2 = ScreenResolution.organiseResolution(Preferences.userNodeForPackage(NifDisplayTester.class), win,
							false, true, true);
		
					simpleUniverse.getCanvas().getView().setSceneAntialiasingEnable(gs2.isAaRequired());
					DDSTextureLoader.setAnisotropicFilterDegree(gs2.getAnisotropicFilterDegree());
					System.out.println("filtering will require newly loaded textures remember");
				}
			});*/

		//win.setJMenuBar(menuBar);
		//win.setVisible(true);

	}
	
	public static void clearTextureSource() {
		textureSource = null;
	}

	private void toggleSpin() {
		spin = !spin;
		if (spinTransform != null) {
			spinTransform.setEnable(spin);
		}
	}

	private void toggleAnimateModel() {
		animateModel = !animateModel;
		update();
	}

	private void toggleHavok() {
		showHavok = !showHavok;		
		update();
		
		// also show debug lines on particles on the same basis
		J3dNiParticleSystem.setSHOW_DEBUG_LINES(showHavok);
	}

	private void toggleVisual() {
		showVisual = !showVisual;
		update();
	}

	/**
	 * Only used by non bsa mesh file display so mesh source is forcibly set to the file system root of file
	 * @param f
	 */
	public void displayNif(File f) {
		System.out.println("Selected file: " + f);

		if (f.isDirectory()) {
			//spinTransform.setEnable(true);
			//processDir(f);
			System.out.println("Bad news dir sent into display nif");
		} else if (f.isFile()) {
			//special single file mesh source
			FileMediaRoots.setMediaRoots(new String[] {FileMediaRoots.splitOffMediaRoot(f.getAbsolutePath())[0]});
			showNif(f.getAbsolutePath(), new FileMeshSource(), textureSource);
		}

		System.out.println("done");

	}

	public void showNif(String filename, MeshSource meshSource) {

		try {
			display(NifToJ3d.loadNif(filename, meshSource, textureSource));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void showNif(String filename, MeshSource meshSource, TextureSource textureSource) {

		try {
			display(NifToJ3d.loadNif(filename, meshSource, textureSource));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private BranchGroup	hbg;

	private BranchGroup	vbg;

	private void update() {
		modelGroup.removeAllChildren();
		if (showHavok) {
			modelGroup.addChild(hbg);
		}
		if (showVisual) {
			modelGroup.addChild(vbg);
		}
	}

	private ArrayList<J3dNiSkinInstance>	allSkins;
	private NifJ3dSkeletonRoot				inputSkeleton;

	private void display(NifJ3dVisPhysRoot nif) {

		if (nif != null) {

			J3dNiAVObject havok = nif.getHavokRoot();

			// set up an animation thread
			if (nif.getVisualRoot().getJ3dNiControllerManager() != null && animateModel) {
				//note self cleaning uping
				ControllerInvokerThread controllerInvokerThread = new ControllerInvokerThread(
						nif.getVisualRoot().getName(), nif.getVisualRoot().getJ3dNiControllerManager(),
						havok.getJ3dNiControllerManager());
				controllerInvokerThread.start();
			}

			modelGroup.removeAllChildren();

			hbg = new BranchGroup();
			hbg.setCapability(BranchGroup.ALLOW_DETACH);

			if (showHavok && havok != null) {
				hbg.addChild(havok);
				modelGroup.addChild(hbg);
			}

			vbg = new BranchGroup();
			vbg.setCapability(BranchGroup.ALLOW_DETACH);
			vbg.setCapability(Node.ALLOW_BOUNDS_READ);

			if (showVisual) {
				// check for skins!
				if (NifJ3dSkeletonRoot.isSkeleton(nif.getNiToJ3dData())) {
					inputSkeleton = new NifJ3dSkeletonRoot(nif.getVisualRoot(), nif.getNiToJ3dData());
					// create skins from the skeleton and skin nif
					allSkins = J3dNiSkinInstance.createSkins(nif.getNiToJ3dData(), inputSkeleton);

					if (allSkins.size() > 0) {
						// add the skins to the scene
						for (J3dNiSkinInstance j3dNiSkinInstance : allSkins) {
							vbg.addChild(j3dNiSkinInstance);
						}

						PerFrameUpdateBehavior pub = new PerFrameUpdateBehavior(new CallBack() {
							@Override
							public void update() {
								// must be called to update the accum transform
								inputSkeleton.updateBones();
								for (J3dNiSkinInstance j3dNiSkinInstance : allSkins) {
									j3dNiSkinInstance.processSkinInstance();
								}
							}

						});
						vbg.addChild(inputSkeleton);
						vbg.addChild(pub);
						modelGroup.addChild(vbg);
					}
				} else {
					vbg.addChild(nif.getVisualRoot());

					//vbg.outputTraversal();
					vbg.compile();// oddly this does NOT get called automatically
					modelGroup.addChild(vbg);
				}
			}

			simpleCameraHandler.viewBounds(vbg.getBounds());

			spinTransform.setEnable(spin);
			BranchGroup bgc = new BranchGroup();
			bgc.setCapability(BranchGroup.ALLOW_DETACH);
			//			bgc.addChild(new Cube(0.01f));
			modelGroup.addChild(bgc);

			//Particles are aut looping for now
			// if a j3dparticlesystem exists fire it off

			for (J3dNiAVObject j3dNiAVObject : nif.getNiToJ3dData().j3dNiAVObjectValues()) {
				if (j3dNiAVObject.getJ3dNiTimeController() != null
					&& j3dNiAVObject.getJ3dNiTimeController() instanceof J3dNiGeomMorpherController) {
					((J3dNiGeomMorpherController)j3dNiAVObject.getJ3dNiTimeController()).fireFrameName("Frame_1", true);
				}
			}
		} else {
			System.out.println("why you give display a null eh?");
		}

	}

	public void close() {
		Canvas3D c = simpleUniverse.getCanvas();
		c.removeNotify();
		c.getGLWindow().destroy();
	}

	private class KeyHandler extends KeyAdapter {

		public KeyHandler() {
			System.out.println("H toggle havok display");
			System.out.println("L toggle visual display");
			System.out.println("J toggle spin");
			System.out.println("K toggle animate model");
			System.out.println("P toggle background color");
			System.out.println("Space toggle cycle through files");
		}

		@Override
		public void keyPressed(KeyEvent e) {

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
		}

	}

	//***************************************
	//Below here are the older File System based methods, for cycling through directories etc

	private boolean					cycle				= true;

	private long					currentFileLoadTime	= 0;

	private FileManageBehavior		fileManageBehavior	= new FileManageBehavior();

	private File					currentFileTreeRoot;

	private File					nextFileTreeRoot;

	private File					currentFileDisplayed;

	private File					nextFileToDisplay;

	public static NifDisplayTester	nifDisplay;

	private static Preferences		prefs;

	public static void main(String[] args) {
		System.setProperty("sun.awt.noerasebackground", "true");
		System.setProperty("j3d.cacheAutoComputeBounds", "true");
		System.setProperty("j3d.defaultReadCapability", "false");
		System.setProperty("j3d.defaultNodePickable", "false");
		System.setProperty("j3d.defaultNodeCollidable", "false");

		System.setProperty("j3d.displaylist", "false");

		prefs = Preferences.userNodeForPackage(NifDisplayTester.class);
		String baseDir = prefs.get("NifDisplayTester.baseDir", System.getProperty("user.dir"));

		nifDisplay = new NifDisplayTester(null);

		DetailsFileChooser dfc = new DetailsFileChooser(baseDir, new DetailsFileChooser.Listener() {
			@Override
			public void directorySelected(File dir) {
				prefs.put("NifDisplayTester.baseDir", dir.getPath());
				nifDisplay.setNextFileTreeRoot(dir);
			}

			@Override
			public void fileSelected(File file) {
				prefs.put("NifDisplayTester.baseDir", file.getPath());
				nifDisplay.setNextFileToDisplay(file);
			}
		});

		dfc.setFileFilter(new FileNameExtensionFilter("Nif", "nif", "btr"));

	}

	private class FileManageBehavior extends Behavior {

		private WakeupCondition FPSCriterion = new WakeupOnElapsedFrames(0, false);

		public FileManageBehavior() {

			setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
			setEnable(true);
		}

		@Override
		public void initialize() {
			wakeupOn(FPSCriterion);
		}

		@Override
		public void processStimulus(Iterator<WakeupCriterion> criteria) {
			process();
			wakeupOn(FPSCriterion);
		}

		private void process() {
			manage();
		}

	}

	private void toggleCycling() {
		cycle = !cycle;
		/*if (cycle)
		{
			// awake the directory processing thread
			synchronized (waitMonitor)
			{
				waitMonitor.notifyAll();
			}
		}*/
	}

	public void setNextFileTreeRoot(File nextFileTreeRoot) {
		this.nextFileToDisplay = null;
		this.nextFileTreeRoot = nextFileTreeRoot;
	}

	public void setNextFileToDisplay(File nextFileToDisplay) {
		this.nextFileTreeRoot = null;
		this.nextFileToDisplay = nextFileToDisplay;
	}

	private void manage() {

		if (nextFileTreeRoot != null) {
			if (!nextFileTreeRoot.equals(currentFileTreeRoot)) {
				currentFileTreeRoot = nextFileTreeRoot;
				currentFileDisplayed = null;
				currentFileLoadTime = Long.MAX_VALUE;
			}
		} else if (currentFileTreeRoot != null) {
			if (cycle) {
				File[] files = currentFileTreeRoot.listFiles(new NifKfFileFilter());
				if (files.length > 0) {
					if (currentFileDisplayed == null) {
						currentFileDisplayed = files[0];
						displayNif(currentFileDisplayed);
					} else if (System.currentTimeMillis() - currentFileLoadTime > 3000) {

					}
				}
			}
		} else if (nextFileToDisplay != null) {
			if (!nextFileToDisplay.equals(currentFileDisplayed)) {
				currentFileDisplayed = nextFileToDisplay;
				displayNif(currentFileDisplayed);
				nextFileToDisplay = null;
			}
		}
	}

}