package nif.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.prefs.Preferences;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Background;
import javax.media.j3d.Behavior;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Group;
import javax.media.j3d.Light;
import javax.media.j3d.Node;
import javax.media.j3d.PointLight;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.WakeupCondition;
import javax.media.j3d.WakeupOnElapsedFrames;
import javax.swing.JMenuItem;
import javax.swing.JSplitPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.sun.j3d.utils.universe.SimpleUniverse;

import archive.BSArchiveSet;
import awt.tools3d.resolution.QueryProperties;
import bsa.source.BsaTextureSource;
import nif.BgsmSource;
import nif.NifJ3dVisPhysRoot;
import nif.NifToJ3d;
import nif.appearance.NiGeometryAppearanceFactoryShader;
import nif.character.NifJ3dSkeletonRoot;
import nif.gui.util.ControllerInvokerThread;
import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiSkinInstance;
import nif.j3d.animation.J3dNiGeomMorpherController;
import nif.j3d.particles.tes3.J3dNiParticles;
import nif.shaders.NiGeometryAppearanceShader;
import tools.compressedtexture.dds.DDSTextureLoader;
import tools.swing.DetailsFileChooser;
import tools3d.camera.simple.SimpleCameraHandler;
import tools3d.utils.Utils3D;
import tools3d.utils.leafnode.Cube;
import tools3d.utils.scenegraph.SpinTransform;
import utils.PerFrameUpdateBehavior;
import utils.PerFrameUpdateBehavior.CallBack;
import utils.source.MeshSource;
import utils.source.TextureSource;
import utils.source.file.FileMeshSource;

public class NifDisplayTester
{
	public JMenuItem setGraphics = new JMenuItem("Set Graphics");

	private SimpleCameraHandler simpleCameraHandler;

	private TransformGroup spinTransformGroup = new TransformGroup();

	private TransformGroup rotateTransformGroup = new TransformGroup();

	private BranchGroup modelGroup = new BranchGroup();

	private SpinTransform spinTransform;

	private FileManageBehavior fileManageBehavior = new FileManageBehavior();

	private boolean cycle = true;

	private boolean showHavok = true;

	private boolean showVisual = true;

	private boolean animateModel = true;

	private boolean spin = false;

	private long currentFileLoadTime = 0;

	private File currentFileTreeRoot;

	private File nextFileTreeRoot;

	private File currentFileDisplayed;

	private File nextFileToDisplay;

	private JSplitPane splitterV = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

	private JSplitPane splitterH = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

	//	private NiObjectDisplayTable niObjectDisplayTable = new NiObjectDisplayTable();

	//	private NifFileDisplayTable nifFileDisplayTable = new NifFileDisplayTable(niObjectDisplayTable);

	//	private NifFileDisplayTree nifFileDisplayTree = new NifFileDisplayTree(niObjectDisplayTable);

	private SimpleUniverse simpleUniverse;

	private Background background = new Background();

	//private JFrame win = new JFrame("Nif model");

	private MeshSource meshSource = null;
	private TextureSource textureSource = null;

	public NifDisplayTester()
	{

		//DDS requires no installed java3D
		if (QueryProperties.checkForInstalledJ3d())
		{
			System.exit(0);
		}
		NifToJ3d.SUPPRESS_EXCEPTIONS = false;
		//ASTC or DDS
		//FileTextureSource.compressionType = FileTextureSource.CompressionType.KTX;
		NiGeometryAppearanceFactoryShader.setAsDefault();
		//FileMediaRoots.setMediaRoots(new String[]{"E:\\Java\\dsstexturesconvert"});

		meshSource = new FileMeshSource();
		//textureSource = new FileTextureSource();

		//Test for android
		//BSArchiveSet bsaFileSet = new BSArchiveSet(new String[] { "F:\\game_media\\Oblivion" }, true, false);
		BSArchiveSet bsaFileSet = new BSArchiveSet(new String[] { "F:\\game_media\\Morrowind", //
				"F:\\game_media\\Oblivion", //
				"F:\\game_media\\Fallout3", //
				"F:\\game_media\\Skyrim", //
				"F:\\game_media\\Fallout4", //
		}, true);
		textureSource = new BsaTextureSource(bsaFileSet);

		NiGeometryAppearanceShader.OUTPUT_BINDINGS = true;

		//win.setVisible(true);

		//win.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		Canvas3D canvas3D = new Canvas3D();
		canvas3D.getGLWindow().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					System.exit(0);
				}
			}
		});

		canvas3D.getGLWindow().addWindowListener(new WindowAdapter() {
			@Override
			public void windowResized(final WindowEvent e)
			{
				J3dNiParticles.setScreenWidth(canvas3D.getGLWindow().getWidth());
			}
		});
		J3dNiParticles.setScreenWidth(canvas3D.getGLWindow().getWidth());

		simpleUniverse = new SimpleUniverse(canvas3D);
		/*		GraphicsSettings gs = ScreenResolution.organiseResolution(Preferences.userNodeForPackage(NifDisplayTester.class), win, false, true,
						true);
		
				canvas3D.getView().setSceneAntialiasingEnable(gs.isAaRequired());
				DDSTextureLoader.setAnisotropicFilterDegree(gs.getAnisotropicFilterDegree());
			*/
		//TODO: these must come form a new one of those ^
		canvas3D.getGLWindow().setSize(800, 600);
		canvas3D.getGLWindow().setPosition(400, 30);
		DDSTextureLoader.setAnisotropicFilterDegree(8);

		//win.setVisible(true);
		canvas3D.addNotify();

		/*	JFrame dataF = new JFrame();
			dataF.getContentPane().setLayout(new GridLayout(1, 1));
		
			splitterH.setTopComponent(nifFileDisplayTree);
			splitterH.setBottomComponent(nifFileDisplayTable);
		
			splitterV.setTopComponent(splitterH);
			splitterV.setBottomComponent(niObjectDisplayTable);
		
			dataF.getContentPane().add(splitterV);
		
			dataF.setSize(900, 900);
			dataF.setLocation(400, 0);
			dataF.setVisible(true);*/

		spinTransformGroup.addChild(rotateTransformGroup);
		rotateTransformGroup.addChild(modelGroup);
		simpleCameraHandler = new SimpleCameraHandler(simpleUniverse.getViewingPlatform(), simpleUniverse.getCanvas(), modelGroup,
				rotateTransformGroup, false);

		splitterV.setDividerLocation(0.5d);
		splitterH.setDividerLocation(0.5d);

		spinTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		spinTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

		modelGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		modelGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);

		// Create ambient light	and add it
		Color3f alColor = new Color3f(0.5f, 0.5f, 0.5f);
		//Color3f alColor = new Color3f(0.5f, 0.5f, 0.5f);
		AmbientLight ambLight = new AmbientLight(true, alColor);
		ambLight.setCapability(Light.ALLOW_INFLUENCING_BOUNDS_WRITE);
		ambLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));

		//Color3f dlColor = new Color3f(0.1f, 0.1f, 0.6f);
		//DirectionalLight dirLight = new DirectionalLight(true, dlColor, new Vector3f(0f, -1f, 0f));
		//dirLight.setCapability(Light.ALLOW_INFLUENCING_BOUNDS_WRITE);
		//dirLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));

		Color3f plColor = new Color3f(1.0f, 0.95f, 0.95f);
		//Color3f plColor = new Color3f(0.4f, 0.4f, 0.7f);
		PointLight pLight = new PointLight(true, plColor, new Point3f(0f, 0f, 0f), new Point3f(1f, 1f, 0f));
		pLight.setCapability(Light.ALLOW_INFLUENCING_BOUNDS_WRITE);
		pLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));

		BranchGroup bg = new BranchGroup();

		bg.addChild(ambLight);
		//bg.addChild(dirLight);

		TransformGroup tg = new TransformGroup();
		// light is above like nifskope
		Transform3D t = new Transform3D(new Quat4f(0, 0, 0, 1), new Vector3f(0, 10, 0), 1);
		tg.setTransform(t);
		tg.addChild(new Cube(0.1f));
		tg.addChild(pLight);
		bg.addChild(tg);

		bg.addChild(simpleCameraHandler);

		bg.addChild(fileManageBehavior);

		bg.addChild(spinTransformGroup);
		spinTransform = new SpinTransform(spinTransformGroup, 0.5);
		spinTransform.setEnable(false);
		bg.addChild(spinTransform);

		background.setColor(0.8f, 0.8f, 0.8f);
		background.setApplicationBounds(null);
		background.setCapability(Background.ALLOW_APPLICATION_BOUNDS_WRITE);
		background.setCapability(Background.ALLOW_APPLICATION_BOUNDS_READ);
		bg.addChild(background);

		bg.addChild(new Cube(0.01f));

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

	public void setNextFileTreeRoot(File nextFileTreeRoot)
	{
		this.nextFileToDisplay = null;
		this.nextFileTreeRoot = nextFileTreeRoot;
	}

	public void setNextFileToDisplay(File nextFileToDisplay)
	{
		this.nextFileTreeRoot = null;
		this.nextFileToDisplay = nextFileToDisplay;
	}

	private void manage()
	{
		if (nextFileTreeRoot != null)
		{
			if (!nextFileTreeRoot.equals(currentFileTreeRoot))
			{
				currentFileTreeRoot = nextFileTreeRoot;
				currentFileDisplayed = null;
				currentFileLoadTime = Long.MAX_VALUE;
			}
		}
		else if (currentFileTreeRoot != null)
		{
			if (cycle)
			{
				File[] files = currentFileTreeRoot.listFiles(new NifKfFileFilter());
				if (files.length > 0)
				{
					if (currentFileDisplayed == null)
					{
						currentFileDisplayed = files[0];
						displayNif(currentFileDisplayed);
					}
					else if (System.currentTimeMillis() - currentFileLoadTime > 3000)
					{

					}
				}
			}
		}
		else if (nextFileToDisplay != null)
		{
			if (!nextFileToDisplay.equals(currentFileDisplayed))
			{
				currentFileDisplayed = nextFileToDisplay;
				displayNif(currentFileDisplayed);
				nextFileToDisplay = null;
			}
		}
	}

	private void toggleSpin()
	{
		spin = !spin;
		if (spinTransform != null)
		{
			spinTransform.setEnable(spin);
		}
	}

	private void toggleAnimateModel()
	{
		animateModel = !animateModel;
		update();
	}

	private void toggleHavok()
	{
		showHavok = !showHavok;
		update();
	}

	private void toggleVisual()
	{
		showVisual = !showVisual;
		update();
	}

	private void toggleBackground()
	{
		if (background.getApplicationBounds() == null)
		{
			background.setApplicationBounds(Utils3D.defaultBounds);
		}
		else
		{
			background.setApplicationBounds(null);
		}

	}

	private void toggleCycling()
	{
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

	public void displayNif(File f)
	{
		System.out.println("Selected file: " + f);

		if (f.isDirectory())
		{
			//spinTransform.setEnable(true);
			//processDir(f);
			System.out.println("Bad news dir sent into display nif");
		}
		else if (f.isFile())
		{
			showNif(f.getAbsolutePath(), meshSource, textureSource);
		}

		System.out.println("done");

	}

	public void showNif(String filename, MeshSource meshSource, TextureSource textureSource)
	{
		BgsmSource.setBgsmSource(meshSource);
		display(NifToJ3d.loadNif(filename, meshSource, textureSource));
	}

	private BranchGroup hbg;

	private BranchGroup vbg;

	private void update()
	{
		modelGroup.removeAllChildren();
		if (showHavok)
		{
			modelGroup.addChild(hbg);
		}
		if (showVisual)
		{
			modelGroup.addChild(vbg);
		}
	}

	private ArrayList<J3dNiSkinInstance> allSkins;
	private NifJ3dSkeletonRoot inputSkeleton;

	private void display(NifJ3dVisPhysRoot nif)
	{

		if (nif != null)
		{

			J3dNiAVObject havok = nif.getHavokRoot();

			// set up an animation thread
			if (nif.getVisualRoot().getJ3dNiControllerManager() != null && animateModel)
			{
				//note self cleaning uping
				ControllerInvokerThread controllerInvokerThread = new ControllerInvokerThread(nif.getVisualRoot().getName(),
						nif.getVisualRoot().getJ3dNiControllerManager(), havok.getJ3dNiControllerManager());
				controllerInvokerThread.start();
			}

			modelGroup.removeAllChildren();

			hbg = new BranchGroup();
			hbg.setCapability(BranchGroup.ALLOW_DETACH);

			if (showHavok && havok != null)
			{
				hbg.addChild(havok);
				modelGroup.addChild(hbg);
			}

			vbg = new BranchGroup();
			vbg.setCapability(BranchGroup.ALLOW_DETACH);
			vbg.setCapability(Node.ALLOW_BOUNDS_READ);

			if (showVisual)
			{
				// check for skins!
				if (NifJ3dSkeletonRoot.isSkeleton(nif.getNiToJ3dData()))
				{
					inputSkeleton = new NifJ3dSkeletonRoot(nif.getVisualRoot(), nif.getNiToJ3dData());
					// create skins from the skeleton and skin nif
					allSkins = J3dNiSkinInstance.createSkins(nif.getNiToJ3dData(), inputSkeleton);

					if (allSkins.size() > 0)
					{
						// add the skins to the scene
						for (J3dNiSkinInstance j3dNiSkinInstance : allSkins)
						{
							vbg.addChild(j3dNiSkinInstance);
						}

						PerFrameUpdateBehavior pub = new PerFrameUpdateBehavior(new CallBack() {
							@Override
							public void update()
							{
								// must be called to update the accum transform
								inputSkeleton.updateBones();
								for (J3dNiSkinInstance j3dNiSkinInstance : allSkins)
								{
									j3dNiSkinInstance.processSkinInstance();
								}
							}

						});
						vbg.addChild(inputSkeleton);
						vbg.addChild(pub);
						modelGroup.addChild(vbg);
					}
				}
				else
				{
					vbg.addChild(nif.getVisualRoot());

					//vbg.outputTraversal();
					vbg.compile();// oddly this does NOT get called automatically
					modelGroup.addChild(vbg);
				}
			}
			System.out.println("vbg.getBounds() " + vbg.getBounds());
			//simpleCameraHandler.viewBounds(vbg.getBounds());

			spinTransform.setEnable(spin);
			BranchGroup bgc = new BranchGroup();
			bgc.setCapability(BranchGroup.ALLOW_DETACH);
			bgc.addChild(new Cube(0.01f));
			modelGroup.addChild(bgc);

			//Particles are aut looping for now
			// if a j3dparticlesystem exists fire it off

			for (J3dNiAVObject j3dNiAVObject : nif.getNiToJ3dData().j3dNiAVObjectValues())
			{
				if (j3dNiAVObject.getJ3dNiTimeController() != null
						&& j3dNiAVObject.getJ3dNiTimeController() instanceof J3dNiGeomMorpherController)
				{
					((J3dNiGeomMorpherController)j3dNiAVObject.getJ3dNiTimeController()) .fireFrameName("Frame_1", true);
				}
			}
		}
		else
		{
			System.out.println("why you give display a null eh?");
		}

	}

	public static NifDisplayTester nifDisplay;

	private static Preferences prefs;

	public static void main(String[] args)
	{
		prefs = Preferences.userNodeForPackage(NifDisplayTester.class);
		String baseDir = prefs.get("NifDisplayTester.baseDir", System.getProperty("user.dir"));

		nifDisplay = new NifDisplayTester();

		DetailsFileChooser dfc = new DetailsFileChooser(baseDir, new DetailsFileChooser.Listener() {
			@Override
			public void directorySelected(File dir)
			{
				prefs.put("NifDisplayTester.baseDir", dir.getPath());
				nifDisplay.setNextFileTreeRoot(dir);
			}

			@Override
			public void fileSelected(File file)
			{
				prefs.put("NifDisplayTester.baseDir", file.getPath());
				nifDisplay.setNextFileToDisplay(file);
			}
		});

		dfc.setFileFilter(new FileNameExtensionFilter("Nif", "nif", "btr"));

	}

	private class FileManageBehavior extends Behavior
	{

		private WakeupCondition FPSCriterion = new WakeupOnElapsedFrames(0, false);

		public FileManageBehavior()
		{

			setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
			setEnable(true);
		}

		@Override
		public void initialize()
		{
			wakeupOn(FPSCriterion);
		}

		@Override
		public void processStimulus(Enumeration criteria)
		{
			process();
			wakeupOn(FPSCriterion);
		}

		private void process()
		{
			manage();
		}

	}

	private class KeyHandler extends KeyAdapter
	{

		public KeyHandler()
		{
			System.out.println("H toggle havok display");
			System.out.println("L toggle visual display");
			System.out.println("J toggle spin");
			System.out.println("K toggle animate model");
			System.out.println("P toggle background color");
			System.out.println("Space toggle cycle through files");
		}

		@Override
		public void keyPressed(KeyEvent e)
		{

			if (e.getKeyCode() == KeyEvent.VK_SPACE)
			{
				toggleCycling();
			}
			else if (e.getKeyCode() == KeyEvent.VK_H)
			{
				toggleHavok();
			}
			else if (e.getKeyCode() == KeyEvent.VK_J)
			{
				toggleSpin();
			}
			else if (e.getKeyCode() == KeyEvent.VK_K)
			{
				toggleAnimateModel();
			}
			else if (e.getKeyCode() == KeyEvent.VK_L)
			{
				toggleVisual();
			}
			else if (e.getKeyCode() == KeyEvent.VK_P)
			{
				toggleBackground();
			}
		}

	}

}