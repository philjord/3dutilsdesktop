package nifbullet.util;

import java.io.File;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.swing.filechooser.FileNameExtensionFilter;

import org.jogamp.java3d.BranchGroup;
import org.jogamp.java3d.JoglesPipeline;
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

	private HeadlessUniverse				j3dUni			= new HeadlessUniverse();

	// Gravity
	private static Vector3f					gravity			= new Vector3f(0f, 0f, -9.81f);

	//private static Clock clock = new Clock();

	// this is the most important class
	private DynamicsWorld					dynamicsWorld	= null;

	private BroadphaseInterface				broadphase;

	private CollisionDispatcher				dispatcher;

	private ConstraintSolver				solver;

	private DefaultCollisionConfiguration	collisionConfiguration;

	private static Preferences				prefs;

	//TODO: can I dump these at all?
	private static MeshSource				meshSource		= null;
	private static TextureSource			textureSource	= null;
	private static BsaMaterialsSource		materialsSource	= null;

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

			// this guy will show all categories on sop
			BulletNifModelClassifier.testNif(nifToDisplay, meshSource);

			BulletNifModel nb = BulletNifModelClassifier.createNifBullet(nifToDisplay, meshSource, 0);

			//Fo4: Croup Manor get visuals and phys and bullet to all move together
			// this is not calssifiable, presumable kinematics are not finished yet
			//Auto opening node: ArchiveFile:Fallout4 - Meshes.ba2/meshes/interiors/building/woodp/doors/bldwoodpdoorload01.nif

			//door are not rotating in the bullet world with the physics world for FO4 and FO76
			//ArchiveFile:SeventySix - Meshes.ba2/meshes/interiors/hightech/animated/doors/hitdoorsm01.nif
			//I see the filing cabinet in croup manior opening the drawer without moving visuals!!

			//Auto opening node: ArchiveFile:SeventySix - Meshes.ba2/meshes/interiors/hightech/animated/doors/hitdoorsm01.nif
			// is not opening the jbullet side

			// Pod is good, but no move bullet, like prewar doors
			//ArchiveFile:Fallout4 - Meshes.ba2/meshes/furniture/cryopod/cryopod01.nif

			

			// as an aside skyrim Dungeons\Imperial\Jail\ImpJailDoorFrame01.nif is loaded in bullet but not in vis or phys
			// loads fine in bsa display? but not in Setting cell to ID:469070
			//ArchiveFile:Skyrim - Meshes.bsa/meshes/dungeons/imperial/jail/impjaildoorframe01.nif

			// in fact glossy pots, silver candles stick and the jail doors all fail to render
			// it the env thing breaking stuff again like brass all ovver agina press L twice slowly gets them back
			// need ot investigate the pipelines sending of shader attributes, dman it
			// and I can't do that easily ousing BSA display
			// unless of course I display 2 at once?? oooo....??

			
			//Morrowind
			// I think this is ok
			// getNifFile nif Meshes\r\Golden Saint.kf not found in archive bsas checked: Tribunal_ktx.bsa,  checked: Bloodmoon_ktx.bsa,  checked: Morrowind_ktx.bsa,  checked: Tribunal.bsa,  checked: Bloodmoon.bsa,  checked: Morrowind.bsa, 
			//MultipleParentException parent =[dynamicsRootBranchGroup] org.jogamp.java3d.BranchGroup@73f4fafc child = NifBullet, file: i\In_lava_1024.NIF class;NBSimpleModel
			//Structure (ADD dynamicsRootBranchGroup-NBSimpleModel:i\In_lava_1024.NIF) update took 23ms
			// when these happen I see no visual for lava and no havok, but bullet is added
			// this seems ot be the same for all lava_1024 everywhere, even a single like dagon fel, vacant tower
			
			
			//Oblivion
			//PhysicsDynamics skipping unknown type Clutter\UpperClass\UpperSilverPitcher02.NIF physicsInfo rigidBodyCount 1 massedRigidBodyCount 1 nonMassedRigidBodyCount 0 transformControllerCount 0 constraintCount 0 skinAndBoneCount 0
			//java.lang.Throwable: what is this layer being given to me for? 5 NifBullet, file: Weapons\Silver\Dagger.NIF class;NBSimpleDynamicModel
			//	at nifbullet.dyn.NBSimpleDynamicModel.<init>(NBSimpleDynamicModel.java:110)
			//PhysicsDynamics skipping unknown type Clutter\FightersGuild\TargetHeavy01.NIF physicsInfo rigidBodyCount 14 massedRigidBodyCount 8 nonMassedRigidBodyCount 6 transformControllerCount 0 constraintCount 9 skinAndBoneCount 0
			//PhysicsDynamics skipping unknown type Clutter\Books\Broadsheet01.NIF physicsInfo rigidBodyCount 1 massedRigidBodyCount 1 nonMassedRigidBodyCount 0 transformControllerCount 0 constraintCount 0 skinAndBoneCount 0

			
			//PhysicsDynamics skipping unknown type Clutter\Farm\Hoe01.NIF physicsInfo rigidBodyCount 1 massedRigidBodyCount 1 nonMassedRigidBodyCount 0 transformControllerCount 0 constraintCount 0 skinAndBoneCount 0
			//PhysicsDynamics skipping unknown type Clutter\Farm\Rake01.NIF physicsInfo rigidBodyCount 1 massedRigidBodyCount 1 nonMassedRigidBodyCount 0 transformControllerCount 0 constraintCount 0 skinAndBoneCount 0
			//PhysicsDynamics skipping unknown type Plants\Dementia\SEDSwampTentacleIngred.NIF physicsInfo rigidBodyCount 1 massedRigidBodyCount 1 nonMassedRigidBodyCount 0 transformControllerCount 0 constraintCount 0 skinAndBoneCount 0
			//phys skipping unknown type Dungeons\Caves\Traps\CTrapSwingMaceShort01.NIF
			//phys skipping unknown type Dungeons\Caves\Traps\CTrapSwingMaceLong01.NIF
			//Meshes\Dungeons\RuinInteriors\TorturedStatue\TortureStatuePedestal2.NIF java.io.IOException: Channel not open for writing - cannot extend file to required size java.base/sun.nio.ch.FileChannelImpl.map(FileChannelImpl.java:976)
			//PhysicsDynamics skipping isComplexDynamic Armor\Fur\M\Gauntlets_gnd.NIF
			//PhysicsDynamics skipping unknown type Dungeons\RuinInteriors\Traps\RuinsRoomColmAtrap.NIF physicsInfo rigidBodyCount 8 massedRigidBodyCount 8 nonMassedRigidBodyCount 0 transformControllerCount 1 constraintCount 0 skinAndBoneCount 0


			
			//fallout 3 
			//phys skipping unknown type Clutter\Fridge\FridgeClosed01.NIF
			//phys skipping isComplexDynamic Clutter\Junk\Bucket01.NIF
			//phys skipping unknown type Weapons\1handGrenadeThrow\GrenadeFrag.NIF
			//phys skipping unknown type Weapons\1HandPistol\10mmPistol.NIF
			//phys skipping unknown type Vehicles\Motorcycle01.NIF


			//Skyrim 
			//phys skipping isComplexDynamic Clutter\Bones\TrollSkull.nif
			// this has 2 rigid bodies, skull and jaw with a bhkLimitedHingeConstraint between them
			//phys skipping isComplexDynamic Clutter\Books\BasicBook03.nif
			//phys skipping isComplexDynamic Clutter\Books\BasicBook05a.nif			
			//phys skipping isComplexDynamic Clutter\Books\SpellTomeConjurationLowPoly.nif
			// good candidates, books, 2 rigid bodies boxes and a hinge constraint
			// the trishapes are skinned to the 3 bones connected to the 2 boxes

			//phys skipping unknown type Skyrim Traps\BoneAlarm02\TrapBoneAlarmBHavok.nif
			//phys skipping unknown type Skyrim Weapons\Volendrung\Volendrung.nif
			//No Physics category for file!! meshes/weapons/orcish/orcishwarhammer.nif
			//physicsInfo rigidBodyCount 1 massedRigidBodyCount 1 nonMassedRigidBodyCount 0 transformControllerCount 0 constraintCount 0 skinAndBoneCount 0

			
			//FO4
			//phys skipping unknown type SetDressing\Building\DeskFanOffice01.nif
			//phys skipping unknown type SetDressing\Vault\Vault_OxygenTank_01.nif
			//phys skipping unknown type Interiors\Vault\Vault111\Rooms\V111GearRoomGate01.nif
			//PhysicsDynamics skipping unknown type Gore\GoreHumanArmR.nif physicsInfo rigidBodyCount 3 massedRigidBodyCount 3 nonMassedRigidBodyCount 0 transformControllerCount 0 constraintCount 0 skinAndBoneCount 0
			//PhysicsDynamics skipping unknown type SetDressing\Construction\ConstructionBarrier01Dest.nif physicsInfo rigidBodyCount 2 massedRigidBodyCount 2 nonMassedRigidBodyCount 0 transformControllerCount 0 constraintCount 0 skinAndBoneCount 0

						
			
			
			//FO76			
			//PhysicsDynamics skipping unknown type SetDressing\Skeletons\SkeletonBareBody01.nif physicsInfo rigidBodyCount 18 massedRigidBodyCount 0 nonMassedRigidBodyCount 18 transformControllerCount 0 constraintCount 17 skinAndBoneCount 0
			//PhysicsDynamics skipping unknown type SetDressing\Skeletons\SkeletonClothed01.nif physicsInfo rigidBodyCount 17 massedRigidBodyCount 0 nonMassedRigidBodyCount 17 transformControllerCount 0 constraintCount 16 skinAndBoneCount 0
			//PhysicsDynamics skipping unknown type Props\SmithingTools\SmithingToolTongs01.nif physicsInfo rigidBodyCount 2 massedRigidBodyCount 0 nonMassedRigidBodyCount 2 transformControllerCount 0 constraintCount 1 skinAndBoneCount 0

			
			
			
			
			
			if (nb != null) {
				nb.addToDynamicsWorld(dynamicsWorld);
				bulletNifModels.add(nb);
				startDisplay();

				if (nb instanceof NBSimpleModel) {
					NBSimpleModel nBSimpleModel = (NBSimpleModel)nb;
					if (nBSimpleModel.getJ3dNiControllerManager() != null) {
						BranchGroup bg = new BranchGroup();
						bg.addChild(nBSimpleModel);
						j3dUni.addBranchGraph(bg);
						//note self cleaning uping
						ControllerInvokerThread controllerInvokerThread = new ControllerInvokerThread(nifToDisplay,
								nBSimpleModel.getJ3dNiControllerManager(), null);
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