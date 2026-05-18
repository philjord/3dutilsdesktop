package bsa.gui;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import bsa.BSAToolMain;
import bsa.tasks.ArchiveFileFilter;
import bsa.tasks.DisplayTask;
import bsaio.ArchiveEntry;
import compressedtexture.DDSImage;
import scrollsexplorer.simpleclient.settings.SetBethFoldersDialog;

public class BSAContentDisplay extends JFrame implements ActionListener {
	public static boolean			LOAD_ALL				= true;

	private boolean					windowMinimized;

	private JTree					tree;

	private DefaultTreeModel		treeModel;

	private BSAFileSetWithStatus	bsaFileSet;

	private JCheckBoxMenuItem		cbMenuItem				= new JCheckBoxMenuItem("Load all BSA Archives");

	private JCheckBoxMenuItem		sopErrMenuItem			= new JCheckBoxMenuItem("SOP errors only");

	private JCheckBoxMenuItem		autoOpenArchiveMenuItem	= new JCheckBoxMenuItem("autoOpenArchive");

	private JCheckBoxMenuItem		autoDisplayMenuItem		= new JCheckBoxMenuItem("autoDisplay");

	public JMenuItem				setFolders				= new JMenuItem("Set Folders");
	
	public JPopupMenu				treeNodePopup			= new JPopupMenu("Howdy");
	public JMenuItem				copyPathPopup			= new JMenuItem("copyPathPopup");
	public JMenuItem				setAutoLoadPopup		= new JMenuItem("setAutoLoadPopup");

	public BSAContentDisplay() {
		super("BSA test display");
		
		DDSImage.OUTPUT_IMAGE_DEBUG = true;
		
		
		windowMinimized = false;
		setDefaultCloseOperation(2);
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
		JMenuBar menuBar = new JMenuBar();
		menuBar.setOpaque(true);
		JMenu menu = new JMenu("File");
		menu.setMnemonic(70);
		boolean loadAll = Boolean.parseBoolean(BSAToolMain.properties.getProperty("load.all"));
		cbMenuItem.setSelected(loadAll);
		menu.add(cbMenuItem);
		menu.add(sopErrMenuItem);
		boolean autoOpenArchive = Boolean.parseBoolean(BSAToolMain.properties.getProperty("autoOpenArchive"));
		autoOpenArchiveMenuItem.setSelected(autoOpenArchive);
		menu.add(autoOpenArchiveMenuItem);
		boolean autoDisplay = Boolean.parseBoolean(BSAToolMain.properties.getProperty("autoDisplay"));
		autoDisplayMenuItem.setSelected(autoDisplay);
		menu.add(autoDisplayMenuItem);
		JMenuItem menuItem = new JMenuItem("Open Archive");
		menuItem.setActionCommand("open");
		menuItem.addActionListener(this);
		menu.add(menuItem);
		menuItem = new JMenuItem("Close Archive");
		menuItem.setActionCommand("close");
		menuItem.addActionListener(this);
		menu.add(menuItem);
		menuItem = new JMenuItem("Exit Program");
		menuItem.setActionCommand("exit");
		menuItem.addActionListener(this);
		menu.add(menuItem);
		menu.add(setFolders);
		setFolders.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				setFolders();
			}
		});

		menuBar.add(menu);
		menu = new JMenu("Action");
		menu.setMnemonic(65);
		menuItem = new JMenuItem("Display Selected Files");
		menuItem.setActionCommand("display selected");
		menuItem.addActionListener(this);
		menu.add(menuItem);
		menuItem = new JMenuItem("Display All Files");
		menuItem.setActionCommand("display all");
		menuItem.addActionListener(this);
		menu.add(menuItem);
		menuItem = new JMenuItem("Verify Selected Files");
		menuItem.setActionCommand("verify selected");
		menuItem.addActionListener(this);
		menu.add(menuItem);
		menuItem = new JMenuItem("Verify All Files");
		menuItem.setActionCommand("verify all");
		menuItem.addActionListener(this);
		menu.add(menuItem);
		menuBar.add(menu);
		menuItem = new JMenuItem("Set Auto Display");
		menuItem.setActionCommand("auto display");
		menuItem.addActionListener(this);
		menu.add(menuItem);
		menuBar.add(menu);
		menu = new JMenu("Help");
		menu.setMnemonic(72);
		menuItem = new JMenuItem("About");
		menuItem.setActionCommand("about");
		menuItem.addActionListener(this);
		menu.add(menuItem);
		menuBar.add(menu);
		setJMenuBar(menuBar);
		
		
			
		copyPathPopup.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String archiveEntryName = getSelectNodeString();
				System.out.println("Archive entry = " + archiveEntryName);
				StringSelection selection = new StringSelection(archiveEntryName);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(selection, selection);
			}
		});
		treeNodePopup.add(copyPathPopup);

		setAutoLoadPopup.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setAutoDisplay();
			}
		});
		treeNodePopup.add(setAutoLoadPopup);
		
		
		treeModel = new DefaultTreeModel(new ArchiveNode());
		tree = new JTree(treeModel);
		JScrollPane scrollPane = new JScrollPane(tree);
		scrollPane.setPreferredSize(new Dimension(700, 540));
		JPanel contentPane = new JPanel();
		contentPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
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

		//auto open gear
		if (autoOpenArchive) {
			String fname = BSAToolMain.properties.getProperty("last opened archive");
			if (fname != null) {
				File arhcive = new File(fname);
				openArchive(arhcive);
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

	@Override
	public void actionPerformed(ActionEvent ae) {
		try {
			String action = ae.getActionCommand();
			if (action.equals("open"))
				openFile();
			else if (action.equals("close"))
				closeFile();
			else if (action.equals("exit"))
				exitProgram();
			else if (action.equals("about"))
				aboutProgram();
			else if (action.equals("display selected"))
				displayFiles(false, false);
			else if (action.equals("display all"))
				displayFiles(true, false);
			else if (action.equals("verify selected"))
				displayFiles(false, true);
			else if (action.equals("verify all"))
				displayFiles(true, true);
			else if (action.equals("auto display"))
				setAutoDisplay();
		} catch (Throwable exc) {
			BSAToolMain.logException("Exception while processing action event", exc);
		}
	}

	private void openFile() throws IOException {
		closeFile();
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
		chooser.putClientProperty("FileChooser.useShellFolder", Boolean.valueOf(BSAToolMain.useShellFolder));
		chooser.setDialogTitle("Select Archive File");
		chooser.setFileFilter(new ArchiveFileFilter());
		if (chooser.showOpenDialog(this) == 0) {
			File file = chooser.getSelectedFile();
			openArchive(file);
		}
	}

	
	public void openArchive(File file) {
		BSAToolMain.properties.setProperty("current.directory", file.getParent());
		BSAToolMain.properties.setProperty("last opened archive", file.getAbsolutePath());

		if (cbMenuItem.isSelected()) {
			bsaFileSet = new BSAFileSetWithStatus(file.getParent(), true, true);
		} else {
			bsaFileSet = new BSAFileSetWithStatus(file.getAbsolutePath(), false, true);
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

	private void setAutoDisplay() {
		String archiveEntryName = getSelectNodeString();
		System.out.println("Auto open archive entry = " + archiveEntryName);
		BSAToolMain.properties.setProperty("auto open archive entry", archiveEntryName);
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

	private void closeFile() throws IOException {
		if (bsaFileSet != null) {
			bsaFileSet.close();
			bsaFileSet = null;
		}
		treeModel = new DefaultTreeModel(new ArchiveNode());
		tree.setModel(treeModel);
	}

	public void displayFiles(boolean displayAllFiles, boolean verifyOnly) throws InterruptedException {
		if (bsaFileSet == null) {
			JOptionPane.showMessageDialog(this, "You must open an archive file", "No archive file", 0);
			return;
		}
		StatusDialog statusDialog = new StatusDialog(this, "Displaying files from " + bsaFileSet.getName());

		List<ArchiveEntry> entries = null;
		if (displayAllFiles) {
			entries = bsaFileSet.getEntries(statusDialog);
		} else {
			TreePath treePaths[] = tree.getSelectionPaths();
			if (treePaths == null) {
				JOptionPane.showMessageDialog(this, "You must select one or more files to display", "No files selected",
						0);
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

	private void exitProgram() {
		if (!windowMinimized) {
			Point p = BSAToolMain.mainWindow.getLocation();
			Dimension d = BSAToolMain.mainWindow.getSize();
			BSAToolMain.properties.setProperty("window.main.position", "" + p.x + "," + p.y);
			BSAToolMain.properties.setProperty("window.main.size", "" + d.width + "," + d.height);

			BSAToolMain.properties.setProperty("load.all", Boolean.toString(cbMenuItem.isSelected()));
			BSAToolMain.properties.setProperty("autoOpenArchive",
					Boolean.toString(autoOpenArchiveMenuItem.isSelected()));
			BSAToolMain.properties.setProperty("autoDisplay", Boolean.toString(autoDisplayMenuItem.isSelected()));

		}
		BSAToolMain.saveProperties();
		System.exit(0);
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
		info += "<br>Java class path: ";
		info += System.getProperty("java.class.path");
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
