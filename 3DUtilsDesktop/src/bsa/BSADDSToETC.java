package bsa;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import bsa.gui.ArchiveNode;
import bsa.gui.BSAFileSetWithStatus;
import bsa.gui.FileNode;
import bsa.gui.FolderNode;
import bsa.gui.StatusDialog;
import bsa.tasks.ArchiveFileFilter;
import bsa.tasks.CreateTaskFromBSA;
import bsa.tasks.LoadTask;
import bsa.tasks.Main;
import bsaio.ArchiveEntry;
import bsaio.ArchiveFile;
import bsaio.DBException;

public class BSADDSToETC extends JFrame implements ActionListener
{	
	private static Preferences prefs;
	
 	private JTree tree;

	private DefaultTreeModel treeModel;

	private BSAFileSetWithStatus bsaFileSet;

	private ArchiveFile archiveFile;

	public BSADDSToETC()
	{
		super("BSA DSS to ETC display");
		
		prefs = Preferences.userNodeForPackage(BSADDSToETC.class);
		
		setDefaultCloseOperation(2);
 
		JMenuBar menuBar = new JMenuBar();
		menuBar.setOpaque(true);
		JMenu menu = new JMenu("File");
		menu.setMnemonic(70);		 
		JMenuItem menuItem = new JMenuItem("Open Archive");
		menuItem.setActionCommand("open");
		menuItem.addActionListener(this);
		menu.add(menuItem);
		menuItem = new JMenuItem("Convert Archive");
		menuItem.setActionCommand("convert");
		menuItem.addActionListener(this);
		menu.add(menuItem);
		menuBar.add(menu);
		setJMenuBar(menuBar);
		treeModel = new DefaultTreeModel(new ArchiveNode());
		tree = new JTree(treeModel);
		JScrollPane scrollPane = new JScrollPane(tree);
		scrollPane.setPreferredSize(new Dimension(700, 540));
		JPanel contentPane = new JPanel();
		contentPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		contentPane.add(scrollPane);
		setContentPane(contentPane);
		 

		 
	}

	public void actionPerformed(ActionEvent ae)
	{
		try
		{
			String action = ae.getActionCommand();
			if (action.equals("open"))
				openFile();
			else if (action.equals("convert"))
				saveFile();
			 
		}
		catch (Throwable exc)
		{
			Main.logException("Exception while processing action event", exc);
		}
	}

	private void openFile() throws IOException
	{
		String baseDir = prefs.get("BSADDSToETCBase", System.getProperty("user.dir"));
		JFileChooser  chooser = new JFileChooser(baseDir);
 
		chooser.putClientProperty("FileChooser.useShellFolder", Boolean.valueOf(Main.useShellFolder));
		chooser.setDialogTitle("Select Archive File");
		chooser.setFileFilter(new ArchiveFileFilter());
		if (chooser.showOpenDialog(this) == 0)
		{
			File file = chooser.getSelectedFile();			

			bsaFileSet = new BSAFileSetWithStatus(file.getAbsolutePath(), false, true);
			
			prefs.put("BSADDSToETCBase", file.getParent());

			//record the archive as the input file
			this.archiveFile = bsaFileSet.get(0);

			DefaultMutableTreeNode root = new DefaultMutableTreeNode();
			for (MutableTreeNode node : bsaFileSet.nodes)
			{
				root.add(node);
			}

			treeModel = new DefaultTreeModel(root);
			tree.setModel(treeModel);

		}
	}
	private void saveFile() throws InterruptedException, IOException, DBException {
		 
		String baseDir = prefs.get("BSADDSToETCBaseOut", System.getProperty("user.dir"));
		JFileChooser  chooser = new JFileChooser(baseDir);
 
		chooser.putClientProperty("FileChooser.useShellFolder", Boolean.valueOf(Main.useShellFolder));
		chooser.setDialogTitle("New Archive File");
		chooser.setApproveButtonText("Create");
		chooser.setFileFilter(new ArchiveFileFilter());
		if (chooser.showOpenDialog(this) != 0)
			return;
		File file = chooser.getSelectedFile();
		prefs.put("BSADDSToETCBaseOut", file.getParent());
		
		if (file.exists()) {
			int option = JOptionPane.showConfirmDialog(this,
					file.getPath() + " already exists.  Do you want to overwrite it?", "File already exists", 0);
			if (option != 0)
				return;
			if (!file.delete()) {
				JOptionPane.showMessageDialog(this, "Unable to delete " + file.getPath(), "Delete failed", 0);
				return;
			}
		}
	
		long tstart = System.currentTimeMillis();
		StatusDialog statusDialog = new StatusDialog(this, "Creating " + file.getPath());
		CreateTaskFromBSA createTask = new CreateTaskFromBSA(file, archiveFile, statusDialog);
		createTask.start();
		int status = statusDialog.showDialog();
		createTask.join();
		if (status != 1)
			return;
		if (!file.exists()) {
			JOptionPane.showMessageDialog(this, "No files were included in the archive", "Archive empty", 1);
			return;
		}
		System.out.println(""	+ (System.currentTimeMillis() - tstart) + "ms to compress " + file.getPath() );

		ArchiveFile archiveFile2 = ArchiveFile.createArchiveFile(new FileInputStream(file).getChannel(),
				file.getName());

		ArchiveNode archiveNode = new ArchiveNode(archiveFile2);
		statusDialog = new StatusDialog(this, "Loading " + archiveFile2.getName());
		LoadTask loadTask = new LoadTask(archiveFile2, archiveNode, statusDialog);
		loadTask.start();
		status = statusDialog.showDialog();
		loadTask.join();
		if (status == 1) {
			this.archiveFile = archiveFile2;
			treeModel = new DefaultTreeModel(archiveNode);
			tree.setModel(treeModel);
		} else {
			archiveFile2.close();
		}

	}

	 

	private void displayFiles(boolean displayAllFiles, boolean verifyOnly) throws InterruptedException
	{
		if (bsaFileSet == null)
		{
			JOptionPane.showMessageDialog(this, "You must open an archive file", "No archive file", 0);
			return;
		}
		StatusDialog statusDialog = new StatusDialog(this, "Displaying files from " + bsaFileSet.getName());

		List<ArchiveEntry> entries = null;
		if (displayAllFiles)
		{
			entries = bsaFileSet.getEntries(statusDialog);
		}
		else
		{
			TreePath treePaths[] = tree.getSelectionPaths();
			if (treePaths == null)
			{
				JOptionPane.showMessageDialog(this, "You must select one or more files to extract", "No files selected", 0);
				return;
			}
			entries = new ArrayList<ArchiveEntry>(100);
			for (int i = 0; i < treePaths.length; i++)
			{
				TreePath treePath = treePaths[i];
				Object obj = treePath.getLastPathComponent();
				if (obj instanceof FolderNode)
				{
					addFolderChildren((FolderNode) obj, entries);
					continue;
				}
				if (!(obj instanceof FileNode))
					continue;
				ArchiveEntry entry = ((FileNode) obj).getEntry();
				if (!entries.contains(entry))
					entries.add(entry);
			}

		}

		DisplayTask displayTask = new DisplayTask(bsaFileSet, entries, statusDialog, verifyOnly, false);
		displayTask.start();
		statusDialog.showDialog();
		displayTask.join();
	}

	private void addFolderChildren(FolderNode folderNode, List<ArchiveEntry> entries)
	{
		int count = folderNode.getChildCount();
		for (int i = 0; i < count; i++)
		{
			TreeNode node = folderNode.getChildAt(i);
			if (node instanceof FolderNode)
			{
				addFolderChildren((FolderNode) node, entries);
				continue;
			}
			if (!(node instanceof FileNode))
				continue;
			ArchiveEntry entry = ((FileNode) node).getEntry();
			if (!entries.contains(entry))
				entries.add(entry);
		}

	}

 
 
	
	public static void main(String args[])
	{
		BSADDSToETC mainWindow = new BSADDSToETC();
		mainWindow.pack();
		mainWindow.setLocationRelativeTo(null);
		mainWindow.setVisible(true);
	}


	
	
}
