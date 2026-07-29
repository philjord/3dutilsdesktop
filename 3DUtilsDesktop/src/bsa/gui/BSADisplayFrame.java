package bsa.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.jogamp.java3d.compressedtexture.CompressedTextureLoader;

import bsa.BSAToolMain;
import bsa.source.BsaMeshSource;
import bsa.source.BsaTextureSource;
import bsa.source.BsaTextureSource.CompressedTextureLoaderETCPackDDS;
import bsa.tasks.ArchiveFileFilter;
import bsa.tasks.DisplayTask;
import bsaio.ArchiveEntry;
import nif.gui.KfDisplayTester;
import nif.gui.NifDisplayTester;
import nif.j3d.particles.J3dNiParticleSystem;
import scrollsexplorer.simpleclient.settings.SetBethFoldersDialog;

public class BSADisplayFrame extends JFrame implements ActionListener, ItemListener {

	private boolean					windowMinimized;

	private JTree					tree;

	private DefaultTreeModel		treeModel;

	private BSAFileSetWithStatus	bsaFileSet;

	private JPopupMenu				treeNodePopup	= new JPopupMenu("Unseen");

	private JCheckBoxMenuItem		cbMenuItem		= new JCheckBoxMenuItem("Load all BSA Archives");
	private JCheckBoxMenuItem		sopErrMenuItem	= new JCheckBoxMenuItem("SOP errors only");

	private JMenu					menuRecentFile	= new JMenu("Recent Files");

	private JMenu					menuSkele		= new JMenu("Skeleton");
	private JMenu					menuSkins		= new JMenu("Skins");

	public BSADisplayFrame() {
		super("BSA test display");

		// just for the now
		J3dNiParticleSystem.DEBUG_DATA = true;

		setupWindow();
		setUpMenus();
		setUpTree();
	}

	@Override
	public void setLocation(int x, int y) {
		super.setLocation(x, y);
	}

	private void setupWindow() {

		windowMinimized = false;
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		String propValue = BSAToolMain.properties.getProperty("window.main.position");
		if (propValue != null) {
			int sep = propValue.indexOf(',');
			int frameX = Integer.parseInt(propValue.substring(0, sep));
			int frameY = Integer.parseInt(propValue.substring(sep + 1));
			setLocation(frameX, frameY);
		}
		int frameWidth = 800;
		int frameHeight = 640;
		propValue = BSAToolMain.properties.getProperty("window.main.size");
		if (propValue != null) {
			int sep = propValue.indexOf(',');
			frameWidth = Integer.parseInt(propValue.substring(0, sep));
			frameHeight = Integer.parseInt(propValue.substring(sep + 1));
		}
		setPreferredSize(new Dimension(frameWidth, frameHeight));
		setSize(new Dimension(frameWidth, frameHeight));
	}

	private void setUpMenus() {
		JMenuBar menuBar = new JMenuBar();
		menuBar.setOpaque(true);

		JMenu menuFile = new JMenu("File");
		menuFile.setMnemonic(70);

		JMenuItem menuItem = new JMenuItem("Open Archive");
		menuItem.setActionCommand("open");
		menuItem.addActionListener(this);
		menuFile.add(menuItem);

		menuFile.add(menuRecentFile);

		menuItem = new JMenuItem("Close Archive");
		menuItem.setActionCommand("close");
		menuItem.addActionListener(this);
		menuFile.add(menuItem);
		boolean loadAll = Boolean.parseBoolean(BSAToolMain.properties.getProperty("load.all"));
		cbMenuItem.setSelected(loadAll);
		menuFile.add(cbMenuItem);
		menuFile.add(sopErrMenuItem);
		boolean autoOpenArchive = Boolean.parseBoolean(BSAToolMain.properties.getProperty("autoOpenArchive"));
		JCheckBoxMenuItem autoOpenArchiveMenuItem = new JCheckBoxMenuItem("Auto Open Archive");
		autoOpenArchiveMenuItem.setSelected(autoOpenArchive);
		autoOpenArchiveMenuItem.setActionCommand("autoOpenArchiveMenuItem");
		menuFile.add(autoOpenArchiveMenuItem);
		boolean autoDisplay = Boolean.parseBoolean(BSAToolMain.properties.getProperty("autoDisplay"));
		JCheckBoxMenuItem autoDisplayMenuItem = new JCheckBoxMenuItem("Auto Display");
		autoDisplayMenuItem.setSelected(autoDisplay);
		autoDisplayMenuItem.setActionCommand("autoDisplayMenuItem");
		menuFile.add(autoDisplayMenuItem);
		menuItem = new JMenuItem("Set Folders");
		menuItem.setActionCommand("setfolders");
		menuItem.addActionListener(this);
		menuFile.add(menuItem);
		menuItem = new JMenuItem("Exit Program");
		menuItem.setActionCommand("exit");
		menuItem.addActionListener(this);
		menuFile.add(menuItem);
		menuBar.add(menuFile);

		JMenu menuAction = new JMenu("Action");
		menuAction.setMnemonic(65);
		menuItem = new JMenuItem("Display Selected Files");
		menuItem.setActionCommand("display selected");
		menuItem.addActionListener(this);
		menuAction.add(menuItem);
		menuItem = new JMenuItem("Display All Files");
		menuItem.setActionCommand("display all");
		menuItem.addActionListener(this);
		menuAction.add(menuItem);
		menuItem = new JMenuItem("Verify Selected Files");
		menuItem.setActionCommand("verify selected");
		menuItem.addActionListener(this);
		menuAction.add(menuItem);
		menuItem = new JMenuItem("Verify All Files");
		menuItem.setActionCommand("verify all");
		menuItem.addActionListener(this);
		menuAction.add(menuItem);
		menuItem = new JMenuItem("Extract Selected Files");
		menuItem.setActionCommand("extract selected");
		menuItem.addActionListener(this);
		menuAction.add(menuItem);
		menuItem = new JMenuItem("Set Auto Display");
		menuItem.setActionCommand("setAutoDisplayEntry");
		menuItem.addActionListener(this);
		menuAction.add(menuItem);
		menuBar.add(menuAction);

		JMenu menuTexture = new JMenu("Texture");
		JCheckBoxMenuItem convertDDStoKTXMenuItem = new JCheckBoxMenuItem("Convert DDS to KTX");
		convertDDStoKTXMenuItem.setActionCommand("convertDDStoKTXMenuItem");
		convertDDStoKTXMenuItem.addItemListener(this);
		boolean convertDDStoKTX = Boolean.parseBoolean(BSAToolMain.properties.getProperty("convertDDStoKTX"));
		convertDDStoKTXMenuItem.setSelected(convertDDStoKTX);
		menuTexture.add(convertDDStoKTXMenuItem);

		menuBar.add(menuTexture);

		JMenu menuNif = new JMenu("Nif");
		ButtonGroup bg1 = new ButtonGroup();

		JRadioButtonMenuItem useOnlyKTXMenuItem = new JRadioButtonMenuItem("Use only KTX");
		useOnlyKTXMenuItem.setActionCommand("useOnlyKTXMenuItem");
		useOnlyKTXMenuItem.addItemListener(this);
		bg1.add(useOnlyKTXMenuItem);
		menuNif.add(useOnlyKTXMenuItem);
		JRadioButtonMenuItem useOnlyDDSMenuItem = new JRadioButtonMenuItem("Use only DDS");
		useOnlyDDSMenuItem.setActionCommand("useOnlyDDSMenuItem");
		useOnlyDDSMenuItem.addItemListener(this);
		bg1.add(useOnlyDDSMenuItem);
		menuNif.add(useOnlyDDSMenuItem);
		JRadioButtonMenuItem anyTextureMenuItem = new JRadioButtonMenuItem("Any Texture");
		anyTextureMenuItem.setActionCommand("anyTextureMenuItem");
		anyTextureMenuItem.addItemListener(this);
		bg1.add(anyTextureMenuItem);
		menuNif.add(anyTextureMenuItem);

		String allowableTextureType = BSAToolMain.properties.getProperty("AllowableTextureType");
		if (allowableTextureType == null || allowableTextureType.equals("anyTextureMenuItem")) {
			anyTextureMenuItem.setSelected(true);
		} else if (allowableTextureType.equals("useOnlyKTXMenuItem")) {
			useOnlyKTXMenuItem.setSelected(true);
		} else if (allowableTextureType.equals("useOnlyDDSMenuItem")) {
			useOnlyDDSMenuItem.setSelected(true);
		}
		menuBar.add(menuNif);

		JMenu menuAnimation = new JMenu("Animation");
		menuItem = new JMenuItem("Clear"); // because we will record across session, make it easy to reset
		menuItem.setActionCommand("clear animation");
		menuItem.addActionListener(this);
		menuAnimation.add(menuItem);
		menuItem = new JMenuItem("Select Skeleton");
		menuItem.setActionCommand("select skeleton");
		menuItem.addActionListener(this);
		menuAnimation.add(menuItem);
		menuItem = new JMenuItem("Select Skin");
		menuItem.setActionCommand("select skin");
		menuItem.addActionListener(this);
		menuAnimation.add(menuItem);
		menuAnimation.add(menuSkele);
		menuAnimation.add(menuSkins);
		menuItem = new JMenuItem("Play Animation");
		menuItem.setActionCommand("play animation");
		menuItem.addActionListener(this);
		menuAnimation.add(menuItem);

		menuBar.add(menuAnimation);

		JMenu menuHelp = new JMenu("Help");
		menuHelp.setMnemonic(72);
		menuItem = new JMenuItem("About");
		menuItem.setActionCommand("about");
		menuItem.addActionListener(this);
		menuHelp.add(menuItem);
		menuBar.add(menuHelp);

		setJMenuBar(menuBar);

		//************ now add pop ups for right click

		JMenuItem copyPathPopup = new JMenuItem("Copy Path To Clipboard");
		copyPathPopup.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String archiveEntryName = getSelectNodeString();
				System.out.println("Copied name of archive entry = " + archiveEntryName);
				StringSelection selection = new StringSelection(archiveEntryName);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(selection, selection);
			}
		});
		treeNodePopup.add(copyPathPopup);

		JMenuItem setAutoLoadPopup = new JMenuItem("Set Auto Load Entry");
		setAutoLoadPopup.setActionCommand("setAutoDisplayEntry");
		setAutoLoadPopup.addActionListener(this);
		treeNodePopup.add(setAutoLoadPopup);

		JMenuItem selectSkeletonPopup = new JMenuItem("Select Skeleton");
		selectSkeletonPopup.setActionCommand("select skeleton");
		selectSkeletonPopup.addActionListener(this);
		treeNodePopup.add(selectSkeletonPopup);

		JMenuItem selectSkinPopup = new JMenuItem("Select Skin");
		selectSkinPopup.setActionCommand("select skin");
		selectSkinPopup.addActionListener(this);
		treeNodePopup.add(selectSkinPopup);

		JMenuItem playAnimationPopup = new JMenuItem("Play Animation");
		playAnimationPopup.setActionCommand("play animation");
		playAnimationPopup.addActionListener(this);
		treeNodePopup.add(playAnimationPopup);

		createRecentMenu();
	}

	private void createRecentMenu() {
		menuRecentFile.removeAll();
		for (int i = 0; i < 10; i++) {
			String recentFileName = BSAToolMain.properties.getProperty("recentFile" + i);
			if (recentFileName == null)
				break;
			JMenuItem recentFileMenuItem = new JMenuItem(recentFileName);
			recentFileMenuItem.setActionCommand("loadRecent" + i);
			recentFileMenuItem.addActionListener(this);
			menuRecentFile.add(recentFileMenuItem);

			//System.out.println("Adding " + i + " recent of " + recentFileName);
		}
	}

	private void openFilePutInRecent(String fileName) {
		// grab all ten out to an array
		String[] recents = new String[10];
		int idx = 0;
		//System.out.println("Putting  " + fileName);
		for (int i = 0; i < 10; i++) {
			String recentFileName = BSAToolMain.properties.getProperty("recentFile" + i);

			if (recentFileName == null)
				break;

			//System.out.println("checking " + i + " recent of " + recentFileName);
			// skip a match
			if (!recentFileName.equals(fileName)) {
				//System.out.println("recentFileName.equals(fileName)");
				recents[idx++] = recentFileName;
			}
		}

		// now set them again after the current
		BSAToolMain.properties.setProperty("recentFile" + 0, fileName);
		// tenth item is dropped
		for (int i = 0; i < 9; i++) {
			if (recents[i] == null)
				break;

			BSAToolMain.properties.setProperty("recentFile" + (i + 1), recents[i]);

			//System.out.println("setting " + "recentFile" + (i + 1) + " to " + recents[i]);
		}
		BSAToolMain.saveProperties();
		createRecentMenu();
	}

	private void loadRecent(String action) {
		int recentIdx = Integer.parseInt(action.substring(action.length() - 1));
		String recentFileName = BSAToolMain.properties.getProperty("recentFile" + recentIdx);
		if (recentFileName != null) {
			File file = new File(recentFileName);
			if (file.exists()) {
				openArchiveFile(new File[] {file});
			} else {
				System.out.println(recentFileName + " does not exist");
			}
		}
	}

	private void setUpTree() {

		treeModel = new DefaultTreeModel(new ArchiveNode());
		tree = new JTree(treeModel);
		JScrollPane scrollPane = new JScrollPane(tree);
		JPanel contentPane = new JPanel();
		contentPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		contentPane.setLayout(new GridLayout(1, 1));
		contentPane.add(scrollPane);
		setContentPane(contentPane);
		addWindowListener(new ApplicationWindowListener());

		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					try {
						displayFiles(false, false);
					} catch (Throwable exc) {
						BSAToolMain.logException("Exception while processing action event", exc);
					}
				}
			}
		});

		boolean autoOpenArchive = Boolean.parseBoolean(BSAToolMain.properties.getProperty("autoOpenArchive"));
		boolean autoDisplay = Boolean.parseBoolean(BSAToolMain.properties.getProperty("autoDisplay"));
		//auto open gear
		if (autoOpenArchive) {
			String fname = BSAToolMain.properties.getProperty("last opened archive");
			if (fname != null) {
				File archive = new File(fname);
				// sometimes deleted/moved since
				if (archive.exists()) {
					openArchiveFile(new File[] {archive});
					//synchronous call so now loaded and I can fire the file double click now
					if (autoDisplay) {
						String aname = BSAToolMain.properties.getProperty("auto open archive entry");
						if (aname != null) {
							System.out.println("Auto opening node: " + aname);
							if (setSelectedNode(aname.split("/"))) {
								try {
									displayFiles(false, false);
								} catch (InterruptedException e1) {
									e1.printStackTrace();
								}
							}
						}
					}
				}
			}
		}

	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		try {
			String action = ae.getActionCommand();
			if (action.equals("open"))
				selectAndOpenFile();
			else if (action.equals("close"))
				closeFile();
			else if (action.equals("exit"))
				exitProgram();
			else if (action.equals("about"))
				aboutProgram();
			else if (action.equals("setfolders"))
				setFolders();
			else if (action.equals("display selected"))
				displayFiles(false, false);
			else if (action.equals("display all"))
				displayFiles(true, false);
			else if (action.equals("verify selected"))
				displayFiles(false, true);
			else if (action.equals("verify all"))
				displayFiles(true, true);
			else if (action.equals("extract selected")) {
				//TODO: extract task requires a single archive file, need to modfiy to use a bsafileset like display does
				//BSAToolFrame.extractFiles(archiveFile, this, tree, false);
				System.out.println("extractFiles not implemeneted yet");
			} else if (action.equals("setAutoDisplayEntry"))
				setAutoDisplayEntry();
			else if (action.startsWith("loadRecent"))
				loadRecent(action);
			else if (action.equals("clear animation"))
				clearAnimation();
			else if (action.equals("select skeleton"))
				selectSkeleton();
			else if (action.equals("select skin"))
				selectSkin();
			else if (action.equals("play animation"))
				playAnimation();

		} catch (Throwable exc) {
			BSAToolMain.logException("Exception while processing action event", exc);
		}
	}

	private void clearAnimation() {
		menuSkele.removeAll();
		menuSkins.removeAll();
	}

	private void selectSkeleton() {
		String skeleName = getSelectNodeString();
		// check it's a reasonable skele
		if (skeleName.contains("skeleton") || (skeleName.contains("xbase_anim") && skeleName.endsWith(".nif"))) {
			System.out.println("setting skeleton to: " + skeleName);

			menuSkele.removeAll();

			JMenuItem skeleMenu = new JMenuItem(skeleName);
			skeleMenu.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					menuSkele.remove((JMenuItem)e.getSource());
				}
			});
			menuSkele.add(skeleMenu);
		}
	}

	private void selectSkin() {
		String skinName = getSelectNodeString();
		// check it's a reasonable skele
		if (skinName.endsWith(".nif")) {
			System.out.println("adding skin : " + skinName);

			// remove if present
			for (Component c : menuSkins.getMenuComponents()) {
				if (((JMenuItem)c).getText().equals(skinName))
					menuSkins.remove(c);
			}

			JMenuItem skinMenu = new JMenuItem(skinName);
			skinMenu.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					menuSkins.remove((JMenuItem)e.getSource());
				}
			});
			menuSkins.add(skinMenu);

			// if there is no skeleton file selected, but one exists in the same folder, set it now as it's likely right
			if (menuSkele.getMenuComponentCount() == 0) {
				String skeleName = "";
				Object[] stp = tree.getSelectionPath().getPath();
				// first is root, not included, then archive name, included
				for (int i = 1; i < stp.length - 1; i++) {
					Object pc = stp[i];
					skeleName = skeleName + (i == 1 ? "" : "/") + pc.toString();
				}
				skeleName += "/skeleton.nif";
				// check if that file exists						
				BsaMeshSource meshSource = new BsaMeshSource(bsaFileSet);
				if (meshSource.nifFileExists(stripArchiveFile(skeleName))) {
					JMenuItem skeleMenu = new JMenuItem(skeleName);
					skeleMenu.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							menuSkele.remove((JMenuItem)e.getSource());
						}
					});
					menuSkele.add(skeleMenu);
				}
			}
		}

	}

	// strip the archive name
	private static String stripArchiveFile(String entryName) {
		int start = entryName.indexOf(".bsa");
		if (start == -1)
			start = entryName.indexOf(".ba2");
		if (start != -1)
			return entryName.substring(start + 5);//for .bxx/
		else
			return entryName;
	}

	private void playAnimation() {
		String animationToDisplay = getSelectNodeString();
		if (menuSkele.getMenuComponentCount() > 0) {
			String skeletonNifModelFile = ((JMenuItem)menuSkele.getMenuComponent(0)).getText();
			// before stripping
			boolean isMorrowind = skeletonNifModelFile.toLowerCase().contains("morrowind");
			skeletonNifModelFile = stripArchiveFile(skeletonNifModelFile);
			System.out.println("Selected skeleton file: " + skeletonNifModelFile);

			// morrowind ignores the animation file and uses one based on the skele
			if (isMorrowind || animationToDisplay.toLowerCase().endsWith(".kf")
				|| animationToDisplay.toLowerCase().endsWith(".hkx")

				//https://forums.nexusmods.com/topic/13200546-animation-in-af-afx/	
				// so its back to the lab again
				|| animationToDisplay.toLowerCase().endsWith(".afx")
				|| animationToDisplay.toLowerCase().endsWith(".af")) {

				ArrayList<String> skinNifModelFiles = new ArrayList<String>();

				if (menuSkins.getMenuComponentCount() > 0) {
					for (Component c : menuSkins.getMenuComponents()) {
						String skinName = ((JMenuItem)c).getText();
						skinName = stripArchiveFile(skinName);
						System.out.println("Selected skin file : " + skinName);
						skinNifModelFiles.add(skinName);
					}
				} else {
					//This is fine, just animate the bones and show them
					System.out.println("No skins selectd animating bones only");
				}

				KfDisplayTester kfDisplayTester = getKfDisplayer();

				if (!isMorrowind) {
					if (animationToDisplay != null) {
						animationToDisplay = stripArchiveFile(animationToDisplay);
						System.out.println("Animation file: " + animationToDisplay);

						try {
							kfDisplayTester.display(skeletonNifModelFile, skinNifModelFiles, animationToDisplay);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					} else {
						System.out.println("No Animation file selected");
					}
				} else {
					//morrowind has a single kf files named after skeleton
					kfDisplayTester.displayTes3(skeletonNifModelFile, skinNifModelFiles);
				}
			} else {
				System.out.println("Animation file no good " + animationToDisplay);
			}
		} else {
			System.out.println("No Skeleton selected");
		}

	}

	//This code set is similar to the DisplayTask which manages Nif Display windows
	private static final int					MAX_KF_CANVAS3D_VISIBLE	= 3;
	private static ArrayDeque<KfDisplayTester>	kfDisplays				= new ArrayDeque<KfDisplayTester>();

	private KfDisplayTester getKfDisplayer() {

		// up to 5
		while (kfDisplays.size() > MAX_KF_CANVAS3D_VISIBLE - 1) {
			KfDisplayTester ndt = kfDisplays.removeFirst();
			ndt.close();
		}
		KfDisplayTester kfDisplay = new KfDisplayTester(bsaFileSet);
		kfDisplays.addLast(kfDisplay);

		return kfDisplay;
	}

	//used by checkboxes/radio buttons
	@Override
	public void itemStateChanged(ItemEvent e) {
		String action = ((JMenuItem)e.getItemSelectable()).getActionCommand();
		if ((action.equals("useOnlyKTXMenuItem")	|| action.equals("useOnlyDDSMenuItem")
				|| action.equals("anyTextureMenuItem"))
			&& e.getStateChange() == ItemEvent.SELECTED) {
			setAllowableTextureType(action);
		} else if (action.equals("convertDDStoKTXMenuItem")) {
			boolean convertDDStoKTX = e.getStateChange() == ItemEvent.SELECTED;
			CompressedTextureLoaderETCPackDDS.CONVERT_DDS_TO_ETC2 = convertDDStoKTX;
			CompressedTextureLoader.clearCache();
			BSAToolMain.properties.setProperty("convertDDStoKTX", Boolean.toString(convertDDStoKTX));
			BSAToolMain.saveProperties();
		} else if (action.equals("autoOpenArchiveMenuItem")) {
			BSAToolMain.properties.setProperty("autoOpenArchive",
					Boolean.toString(e.getStateChange() == ItemEvent.SELECTED));
		} else if (action.equals("autoDisplayMenuItem")) {
			BSAToolMain.properties.setProperty("autoDisplay",
					Boolean.toString(e.getStateChange() == ItemEvent.SELECTED));
		}

	}

	private void selectAndOpenFile() throws IOException {

		String currentDirectory = BSAToolMain.properties.getProperty("current.directory");
		JFileChooser chooser;
		if (currentDirectory != null) {
			File dirFile = new File(currentDirectory);
			if (dirFile.exists() && dirFile.isDirectory())
				chooser = new JFileChooser(dirFile);
			else
				chooser = new JFileChooser();
		} else {
			chooser = new JFileChooser();
		}
		chooser.setMultiSelectionEnabled(true);
		chooser.putClientProperty("FileChooser.useShellFolder", Boolean.valueOf(BSAToolMain.useShellFolder));
		chooser.setDialogTitle("Select Archive File");
		chooser.setFileFilter(new ArchiveFileFilter());
		if (chooser.showOpenDialog(this) == 0) {
			File[] files = chooser.getSelectedFiles();
			openArchiveFile(files);
		}
	}

	public void openArchiveFile(File[] files) {
		// record it's opening in the list (but only when 1 is selected too chaos otherwise
		if (files.length == 1) {
			openFilePutInRecent(files[0].getAbsolutePath());
			BSAToolMain.properties.setProperty("current.directory", files[0].getParent());
			BSAToolMain.properties.setProperty("last opened archive", files[0].getAbsolutePath());
		}
		BSAToolMain.saveProperties();
		
		// we close late in case they just cancel
		closeFile();
		
		if (cbMenuItem.isSelected()) {
			bsaFileSet = new BSAFileSetWithStatus(files[0].getParent(), true, true);
		} else {
			String[] fileNames = new String[files.length];
			for (int i = 0; i < files.length; i++)
				fileNames[i] = files[i].getAbsolutePath();
			bsaFileSet = new BSAFileSetWithStatus(fileNames, false, true);
		}

		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		for (MutableTreeNode node : bsaFileSet.nodes) {
			root.add(node);
		}

		treeModel = new DefaultTreeModel(root);
		tree.setModel(treeModel);

		// add the popup right click
		tree.setComponentPopupMenu(treeNodePopup);
		// make it select a node on right click too, note it is a poor performer, need to dismiss prior pop up
		MouseAdapter ml = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
				if (selPath != null) {
					tree.setSelectionPath(selPath);
				}
			}
		};
		tree.addMouseListener(ml);
		
		loadAnimationMenus();
	}

	private String getSelectNodeString() {
		String archiveEntryName = "";
		Object[] stp = tree.getSelectionPath().getPath();
		// first is root, not included, then archive name, included
		for (int i = 1; i < stp.length; i++) {
			Object pc = stp[i];
			archiveEntryName = archiveEntryName + (i == 1 ? "" : "/") + pc.toString();
		}
		return archiveEntryName;
	}

	private void setAutoDisplayEntry() {
		String archiveEntryName = getSelectNodeString();
		System.out.println("Auto open archive entry = " + archiveEntryName);
		BSAToolMain.properties.setProperty("auto open archive entry", archiveEntryName);
		BSAToolMain.saveProperties();
	}

	public boolean setSelectedNode(String[] pathNames) {
		TreeNode node = (DefaultMutableTreeNode)treeModel.getRoot();

		for (int level = 0; level < pathNames.length; level++) {
			DefaultMutableTreeNode foundChild = null;
			Enumeration<TreeNode> e = (Enumeration<TreeNode>)node.children();
			TreeNode c = null;
			while (e.hasMoreElements()) {
				c = e.nextElement();
				DefaultMutableTreeNode child = (DefaultMutableTreeNode)c;
				if (child.toString().equals(pathNames[level])) {
					foundChild = child;
					break;
				}
			}
			if (foundChild != null) {
				//System.out.println("child found for " + pathNames[level]);
				node = c;
			} else {
				//System.out.println("no child found for " + pathNames[level]);
				return false;
			}

		}

		TreePath treePath = new TreePath(treeModel.getPathToRoot(node));
		tree.expandPath(treePath);
		tree.setSelectionPath(treePath);
		return true;
	}

	private void closeFile() {
		if (bsaFileSet != null) {
			try {
				bsaFileSet.close();
			} catch (IOException e) {
				//not a big deal
			}
			bsaFileSet = null;
		}
		// forget current texture source as it's likely the next open will be whole new game folder 
		NifDisplayTester.clearTextureSource();
		KfDisplayTester.clearTextureSource();
		treeModel = new DefaultTreeModel(new ArchiveNode());
		tree.setModel(treeModel);
	}

	public void displayFiles(boolean displayAllFiles, boolean verifyOnly) throws InterruptedException {
		if (bsaFileSet == null) {
			JOptionPane.showMessageDialog(this, "You must open an archive file", "No archive file", 0);
			return;
		}
		String op = verifyOnly ? "Verify" : "Display";
		StatusDialog statusDialog = new StatusDialog(this, op + "ing files from " + bsaFileSet.getName());

		List<ArchiveEntry> entries = null;
		if (displayAllFiles) {
			entries = bsaFileSet.getEntries(statusDialog);
			int response = JOptionPane.showConfirmDialog(this,
					"This might be a very long operation " + entries.size() + " items in archive", op + " All Files?",
					JOptionPane.OK_CANCEL_OPTION);
			if (response != JOptionPane.OK_OPTION)
				return;
		} else {
			TreePath treePaths[] = tree.getSelectionPaths();
			if (treePaths == null) {
				// probably just an accidental double click somewhere
				return;
			}
			entries = new ArrayList<ArchiveEntry>(100);
			for (int i = 0; i < treePaths.length; i++) {
				TreePath treePath = treePaths[i];
				Object obj = treePath.getLastPathComponent();
				if (obj instanceof FolderNode) {
					addFolderChildren((FolderNode)obj, entries);
					continue;
				}
				if (!(obj instanceof FileNode))
					continue;
				ArchiveEntry entry = ((FileNode)obj).getEntry();
				if (!entries.contains(entry))
					entries.add(entry);
			}

		}

		DisplayTask displayTask = new DisplayTask(bsaFileSet, entries, statusDialog, verifyOnly,
				sopErrMenuItem.isSelected());
		displayTask.start();
		statusDialog.showDialog();
		displayTask.join();
	}

	private void addFolderChildren(FolderNode folderNode, List<ArchiveEntry> entries) {
		int count = folderNode.getChildCount();
		for (int i = 0; i < count; i++) {
			TreeNode node = folderNode.getChildAt(i);
			if (node instanceof FolderNode) {
				addFolderChildren((FolderNode)node, entries);
				continue;
			}
			if (!(node instanceof FileNode))
				continue;
			ArchiveEntry entry = ((FileNode)node).getEntry();
			if (!entries.contains(entry))
				entries.add(entry);
		}

	}

	private void setFolders() {
		SetBethFoldersDialog setBethFoldersDialog = new SetBethFoldersDialog(this);
		setBethFoldersDialog.setSize(300, 400);
		setBethFoldersDialog.setVisible(true);
	}

	private static void setAllowableTextureType(String action) {
		if (action.equals("useOnlyKTXMenuItem")) {
			BsaTextureSource.allowedTextureFormats = BsaTextureSource.AllowedTextureFormats.KTX;
		} else if (action.equals("useOnlyDDSMenuItem")) {
			BsaTextureSource.allowedTextureFormats = BsaTextureSource.AllowedTextureFormats.DDS;
		} else if (action.equals("anyTextureMenuItem")) {
			BsaTextureSource.allowedTextureFormats = BsaTextureSource.AllowedTextureFormats.ALL;
		}
		System.out.println("setAllowableTextureType " + action);
		BSAToolMain.properties.setProperty("AllowableTextureType", action);
		BSAToolMain.saveProperties();
	}

	
	private void loadAnimationMenus() {
		menuSkele.removeAll();
		String skeleName = BSAToolMain.properties.getProperty("anim.skele", null);
		if (skeleName != null) {
			JMenuItem skeleMenu = new JMenuItem(skeleName);
			skeleMenu.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					menuSkele.remove((JMenuItem)e.getSource());
				}
			});
			menuSkele.add(skeleMenu);
		}
		menuSkins.removeAll();
		for (int i = 0; i < 10; i++) {
			String skinName = BSAToolMain.properties.getProperty("anim.skin" + i, null);
			if (skinName != null && skinName.length() > 0) {
				JMenuItem skinMenu = new JMenuItem(skinName);
				skinMenu.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						menuSkins.remove((JMenuItem)e.getSource());
					}
				});
				menuSkins.add(skinMenu);
			}
		}
	}
	
	private void saveAnimationMenus() {
		if (menuSkele.getMenuComponentCount() > 0) {
			String skeletonNifModelFile = ((JMenuItem)menuSkele.getMenuComponent(0)).getText();
			BSAToolMain.properties.setProperty("anim.skele", skeletonNifModelFile);
		} else {
			BSAToolMain.properties.setProperty("anim.skele", "");
		}

		ArrayList<String> skinNifModelFiles = new ArrayList<String>();
		if (menuSkins.getMenuComponentCount() > 0) {
			for (Component c : menuSkins.getMenuComponents()) {
				String skinName = ((JMenuItem)c).getText();
				skinNifModelFiles.add(skinName);
			}
		}

		for (int i = 0; i < 10; i++) {
			String skinName = "";
			if (skinNifModelFiles.size() > i) {
				skinName = skinNifModelFiles.get(i);
			}
			BSAToolMain.properties.setProperty("anim.skin" + i, skinName);
		}
		BSAToolMain.saveProperties();
	}

	private void exitProgram() {
		saveAnimationMenus();

		// don't record window if minimised!
		if (!windowMinimized) {
			Point p = BSAToolMain.mainWindow.getLocation();
			Dimension d = BSAToolMain.mainWindow.getSize();
			System.out.println("window.main.position " + p.x + "," + p.y);
			System.out.println("window.main.size " + d.width + "," + d.height);
			BSAToolMain.properties.setProperty("window.main.position", "" + p.x + "," + p.y);
			BSAToolMain.properties.setProperty("window.main.size", "" + d.width + "," + d.height);
		}

		BSAToolMain.properties.setProperty("load.all", Boolean.toString(cbMenuItem.isSelected()));
		BSAToolMain.saveProperties();
		//System.exit(0);
	}

	private void aboutProgram() {
		String info = "<html>Phil's reworking of the fallout BSA file manager<br>";
		info += "<br>User name: ";
		info += System.getProperty("user.name");
		info += "<br>Home directory: ";
		info += System.getProperty("user.home");
		info += "<br><br>OS: ";
		info += System.getProperty("os.name");
		info += "<br>OS version: ";
		info += System.getProperty("os.version");
		info += "<br>OS patch level: ";
		info += System.getProperty("sun.os.patch.level");
		info += "<br><br>Java vendor: ";
		info += System.getProperty("java.vendor");
		info += "<br>Java version: ";
		info += System.getProperty("java.version");
		info += "<br>Java home directory: ";
		info += System.getProperty("java.home");
		//info += "<br>Java class path: ";
		//info += System.getProperty("java.class.path");
		info += "</html>";
		JOptionPane.showMessageDialog(this, info.toString(), "About This Utility", 1);
	}

	private class ApplicationWindowListener extends WindowAdapter {

		public ApplicationWindowListener() {

		}

		@Override
		public void windowIconified(WindowEvent we) {
			windowMinimized = true;
		}

		@Override
		public void windowDeiconified(WindowEvent we) {
			windowMinimized = false;
		}

		@Override
		public void windowClosing(WindowEvent we) {
			try {
				exitProgram();
			} catch (Exception exc) {
				BSAToolMain.logException("Exception while closing application window", exc);
			}
		}

	}

}
