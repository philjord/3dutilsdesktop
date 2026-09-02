package nifbullet.util;

import java.io.File;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.swing.filechooser.FileNameExtensionFilter;

import org.jogamp.java3d.BranchGroup;
import org.jogamp.java3d.JoglesPipeline;
import org.jogamp.java3d.Transform3D;
import org.jogamp.vecmath.Vector3f;

import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;

import bsa.gui.BSAFileSetWithStatus;
import bsa.source.BsaMaterialsSource;
import bsa.source.BsaMeshSource;
import bsa.source.BsaTextureSource;
import nativeLinker.LWJGLLinker;
import nif.NifToJ3d;
import nif.appearance.NiGeometryAppearanceFactoryShader;
import nif.gui.NifKfFileFilter;
import nif.gui.util.ControllerInvokerThread;
import nifbullet.BulletNifModel;
import nifbullet.BulletNifModelClassifier;
import nifbullet.simple.NBSimpleModel;
import nifbullet.util.debug.opengl.DebugOutput;
import nifbullet.util.debug.opengl.LWJGL;
import tools.swing.DetailsFileChooser;
import tools3d.universe.HeadlessUniverse;
import utils.source.MaterialsSource;
import utils.source.MeshSource;
import utils.source.TextureSource;
import utils.source.file.FileMeshSource;

public class NifBulletDisplay {

	private HeadlessUniverse						j3dUni			= new HeadlessUniverse();

	// Gravity
	private static Vector3f							gravity			= new Vector3f(0f, 0f, -9.81f);

	//private static Clock clock = new Clock();

	// this is the most important class
	private  DynamicsWorld					dynamicsWorld	= null;

	private  BroadphaseInterface				broadphase;

	private  CollisionDispatcher				dispatcher;

	private  ConstraintSolver					solver;

	private  DefaultCollisionConfiguration	collisionConfiguration;

	private static Preferences						prefs;

	//TODO: can I dump these at all?
	private static MeshSource						meshSource		= null;
	private static TextureSource					textureSource	= null;
	private static BsaMaterialsSource				materialsSource	= null;

	public NifBulletDisplay(BSAFileSetWithStatus parentBsaFileSet) {
		new LWJGLLinker();
		// FIXME: the holding of context may add speed but it causes the pipeline to not call releaseContext on each update pass
		// on the GLWindow Surface so the GLWindow setVisible(false) won't remove it
		// and it can't be destroyed, to fix this issue at the least stopping renderer should force a releaseCtx on the pipeline		
		JoglesPipeline.LATE_RELEASE_CONTEXT = false;

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

		// collision configuration contains default setup for memory, collision setup
		collisionConfiguration = new DefaultCollisionConfiguration();

		// use the default collision dispatcher. For parallel processing you can use a diffent dispatcher (see Extras/BulletMultiThreaded)
		dispatcher = new CollisionDispatcher(collisionConfiguration);

		broadphase = new DbvtBroadphase();

		// the default constraint solver. For parallel processing you can use a different solver (see Extras/BulletMultiThreaded)
		SequentialImpulseConstraintSolver sol = new SequentialImpulseConstraintSolver();
		solver = sol;

		// TODO: needed for SimpleDynamicsWorld
		//sol.setSolverMode(sol.getSolverMode() & ~SolverMode.SOLVER_CACHE_FRIENDLY.getMask());

		dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);

		dynamicsWorld.setGravity(gravity);

	}

	public void displayNif(String nifToDisplay) {
		System.out.println("Selected bullet nif: " + nifToDisplay);

		try {
			for (BulletNifModel bnm : bulletNifModels) {
				bnm.removeFromDynamicsWorld();
			}

			BulletNifModelClassifier.testNif(nifToDisplay, meshSource);
			
			BulletNifModel nb = BulletNifModelClassifier.createNifBullet(nifToDisplay, meshSource, 0);
			
			// for fun
			nb = new NBSimpleModel(nifToDisplay, meshSource, new Transform3D());
			
			
			//Fo4: Croup Manor has a SetDressing/Rowboat/Rowboat.nif showing in JBullet, but not for rendered physics
			// what the hell
			// also 2 bookshelve not in rend phys nor bullet
			// fan is in rend physics but not bull
			
			
			
			if (nb != null) {
				nb.addToDynamicsWorld(dynamicsWorld);
				bulletNifModels.add(nb);
				startDisplay();

				if (nb instanceof NBSimpleModel) {
					NBSimpleModel nbKinematicModel = (NBSimpleModel)nb;
					if (nbKinematicModel.getJ3dNiControllerManager() != null) {
						BranchGroup bg = new BranchGroup();
						bg.addChild(nbKinematicModel);
						j3dUni.addBranchGraph(bg);
						//note self cleaning uping
						ControllerInvokerThread controllerInvokerThread = new ControllerInvokerThread(
								nifToDisplay, nbKinematicModel.getJ3dNiControllerManager(), null);
						controllerInvokerThread.start();
					}
				}

			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	public void close() {
		// TODO Auto-generated method stub

	}

	private Thread displayThread;

	private void startDisplay() {
		if (displayThread == null) {
			displayThread = new Thread(new Runnable() {

				@Override
				public void run() {

					DebugOutput.initDebug(dynamicsWorld);
					while (true) {

						LWJGL.step();
						try {
							Thread.sleep(100);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

				}
			});
			displayThread.setDaemon(true);
			displayThread.start();
		}

	}

	//OOOOOOOOOOLLLLLLLLLLLLLLDDDDDDDDDDDDDDD*******************************

	public static NifBulletDisplay	nifDisplay;

	private long					currentFileLoadTime	= 0;

	private File					currentFileTreeRoot;

	private File					nextFileTreeRoot;

	private File					currentFileDisplayed;

	private File					nextFileToDisplay;

	public void setNextFileTreeRoot(File nextFileTreeRoot) {
		this.nextFileToDisplay = null;
		this.nextFileTreeRoot = nextFileTreeRoot;
	}

	public void setNextFileToDisplay(File nextFileToDisplay) {
		this.nextFileTreeRoot = null;
		this.nextFileToDisplay = nextFileToDisplay;
	}

	public void manage() {
		if (nextFileTreeRoot != null) {
			if (!nextFileTreeRoot.equals(currentFileTreeRoot)) {
				currentFileTreeRoot = nextFileTreeRoot;
				currentFileDisplayed = null;
				currentFileLoadTime = Long.MAX_VALUE;
			}
		} else if (currentFileTreeRoot != null) {

			File[] files = currentFileTreeRoot.listFiles(new NifKfFileFilter());
			if (files.length > 0) {
				if (currentFileDisplayed == null) {
					currentFileDisplayed = files[0];
					displayNif(currentFileDisplayed);
				} else if (System.currentTimeMillis() - currentFileLoadTime > 3000) {

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

	public void displayNif(File f) {
		System.out.println("Selected file: " + f);

		if (f.isDirectory()) {
			//spinTransform.setEnable(true);
			processDir(f);
			System.out.println("Bad news dir sent into display nif");
		} else if (f.isFile()) {
			try {
				for (BulletNifModel bnm : bulletNifModels) {
					bnm.removeFromDynamicsWorld();
				}

				BulletNifModel nb = BulletNifModelClassifier.createNifBullet(f.getAbsolutePath(), new FileMeshSource(),
						0);
				if (nb != null) {
					nb.addToDynamicsWorld(dynamicsWorld);
					bulletNifModels.add(nb);
					startDisplay();

					if (nb instanceof NBSimpleModel) {
						NBSimpleModel nbKinematicModel = (NBSimpleModel)nb;
						if (nbKinematicModel.getJ3dNiControllerManager() != null) {
							BranchGroup bg = new BranchGroup();
							bg.addChild(nbKinematicModel);
							j3dUni.addBranchGraph(bg);
							//note self cleaning uping
							ControllerInvokerThread controllerInvokerThread = new ControllerInvokerThread(
									f.getAbsolutePath(), nbKinematicModel.getJ3dNiControllerManager(), null);
							controllerInvokerThread.start();
						}
					}

				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		System.out.println("done");

	}

	public NifBulletDisplay() {
		// collision configuration contains default setup for memory, collision setup
		collisionConfiguration = new DefaultCollisionConfiguration();

		// use the default collision dispatcher. For parallel processing you can use a diffent dispatcher (see Extras/BulletMultiThreaded)
		dispatcher = new CollisionDispatcher(collisionConfiguration);

		broadphase = new DbvtBroadphase();

		// the default constraint solver. For parallel processing you can use a different solver (see Extras/BulletMultiThreaded)
		SequentialImpulseConstraintSolver sol = new SequentialImpulseConstraintSolver();
		solver = sol;

		// TODO: needed for SimpleDynamicsWorld
		//sol.setSolverMode(sol.getSolverMode() & ~SolverMode.SOLVER_CACHE_FRIENDLY.getMask());

		dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);

		dynamicsWorld.setGravity(gravity);
	}

	public static void main(String[] args) {

		new LWJGLLinker();

		nifDisplay = new NifBulletDisplay();
		prefs = Preferences.userNodeForPackage(NifBulletDisplay.class);
		String baseDir = prefs.get("NifBulletDisplay.baseDir", System.getProperty("user.dir"));

		DetailsFileChooser dfc = new DetailsFileChooser(baseDir, new DetailsFileChooser.Listener() {

			@Override
			public void directorySelected(File dir) {
				prefs.put("NifBulletDisplay.baseDir", dir.getPath());
				nifDisplay.setNextFileTreeRoot(dir);
			}

			@Override
			public void fileSelected(File file) {
				prefs.put("NifBulletDisplay.baseDir", file.getPath());
				nifDisplay.setNextFileToDisplay(file);
			}
		});

		dfc.setFileFilter(new FileNameExtensionFilter("Nif or Kf", "nif", "kf"));

		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(100);
					} catch (Exception e) {
						e.printStackTrace();
					}
					nifDisplay.manage();
				}
			}
		});
		t.setDaemon(true);
		t.start();
	}

	private static ArrayList<BulletNifModel> bulletNifModels = new ArrayList<BulletNifModel>();

	private static void processDir(File dir) {
		System.out.println("Processing directory " + dir);
		File[] fs = dir.listFiles();
		for (int i = 0; i < fs.length; i++) {
			try {
				if (fs[i].isFile() && (fs[i].getName().endsWith(".nif") || fs[i].getName().endsWith(".kf"))) {

				} else if (fs[i].isDirectory()) {
					processDir(fs[i]);
				}

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

}