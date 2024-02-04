package esm.display;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.StringCharacterIterator;
import java.util.Enumeration;
import java.util.zip.DataFormatException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import esfilemanager.common.PluginException;
import esfilemanager.common.data.display.Plugin;
import esfilemanager.common.data.display.PluginFile;
import esfilemanager.common.data.plugin.PluginGroup;
import esfilemanager.common.data.plugin.PluginRecord;
import esfilemanager.common.data.plugin.PluginSubrecord;
import esfilemanager.common.data.record.Record;
import esfilemanager.common.data.record.Subrecord;
import esm.EsmFileLocations;

public class PluginDisplayDialog extends JFrame implements ActionListener, TreeExpansionListener
{
	public static boolean SHOW_ALL = true;// if false hide WRLD CELL and DIAL

	public static void main(String[] args)
	{
		String generalEsmFile = EsmFileLocations.getGeneralEsmFile();
		if (generalEsmFile != null)
		{
			long startTime = System.currentTimeMillis();
			System.out.println("loading file " + generalEsmFile + " TODO: put the lovely BSA stye status dialog up and get this guy cleaned up compared to");

			File pluginFile = new File(generalEsmFile);
			Plugin plugin = new PluginFile(pluginFile);
			try
			{
				plugin.loadch(!SHOW_ALL);

				PluginDisplayDialog displayDialog = new PluginDisplayDialog(plugin);
				displayDialog.setTitle("Display of " + pluginFile.getName());
				displayDialog.setSize(1200, 800);
				displayDialog.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				displayDialog.setVisible(true);
				displayDialog.setLocationRelativeTo(null); 

				System.out.println("Finished loading in " + (System.currentTimeMillis() - startTime));
			}
			catch (PluginException e)
			{
				e.printStackTrace();
			}
			catch (DataFormatException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private JTree pluginTree;

	private JPanel displayPane;

	public PluginDisplayDialog(Plugin plugin)
	{
		pluginTree = new JTree(createPluginNodes(plugin));
		pluginTree.setScrollsOnExpand(true);
		pluginTree.addTreeExpansionListener(this);
		JScrollPane pluginScrollPane = new JScrollPane(pluginTree);

		JPanel treePane = new JPanel();
		treePane.setLayout(new BoxLayout(treePane, 0));
		treePane.setOpaque(true);
		treePane.setBackground(new Color(240, 240, 240));
		treePane.add(pluginScrollPane);
		treePane.add(Box.createHorizontalStrut(5));

		displayPane = new JPanel();
		displayPane.setLayout(new GridLayout(1, 1));
		treePane.add(displayPane);

		pluginTree.addTreeSelectionListener(new TreeSelectionListener() {

			@Override
			public void valueChanged(TreeSelectionEvent e)
			{
				displaySubrecordData();
			}

		});

		treePane.add(Box.createHorizontalStrut(5));
		JPanel buttonPane = new JPanel();
		buttonPane.setBackground(new Color(240, 240, 240));

		JButton button = new JButton("Done");
		button.setActionCommand("done");
		button.setHorizontalAlignment(0);
		button.addActionListener(this);
		buttonPane.add(button);
		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BoxLayout(contentPane, 1));
		contentPane.setOpaque(true);
		contentPane.setBackground(new Color(240, 240, 240));
		contentPane.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
		contentPane.add(treePane);
		contentPane.add(Box.createVerticalStrut(15));
		contentPane.add(buttonPane);

		setContentPane(contentPane);

	}

	@Override
	public void actionPerformed(ActionEvent ae)
	{
		String action = ae.getActionCommand();
		if (action.equals("done"))
		{
			setVisible(false);
			dispose();
		}
	}

	private void displaySubrecordData()
	{
		TreePath treePaths[] = pluginTree.getSelectionPaths();
		if (treePaths != null)
		{
			for (TreePath treePath : treePaths)
			{
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
				Object userObject = node.getUserObject();
				if (userObject instanceof PluginSubrecord)
				{
					displayPane.removeAll();
					displayPane.add(DisplaySubrecordDialog.getTextArea((PluginSubrecord) userObject));

					this.validate();

				}
			}
		}

	}

	private DefaultMutableTreeNode createPluginNodes(Plugin plugin)
	{
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(plugin);
		try
		{
			for (PluginGroup group : plugin.getGroupList())
			{
				PluginRecordTreeNode groupNode = new PluginRecordTreeNode(group);
				createGroupChildren(groupNode, group);
				root.add(groupNode);
			}
		}
		catch (DataFormatException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (PluginException e)
		{
			e.printStackTrace();
		}

		return root;
	}

	private void createGroupChildren(DefaultMutableTreeNode groupNode, PluginGroup group)
			throws DataFormatException, IOException, PluginException
	{
		for (Record r : group.getRecordList())
		{
			PluginRecord record = (PluginRecord) r;
			PluginRecordTreeNode recordNode = new PluginRecordTreeNode(record);
			boolean insertNode = false;
			int index = 0;
			if (record instanceof PluginGroup)
			{
				createGroupChildren(recordNode, (PluginGroup) record);
			}
			else
			{
				if (record.getSubrecords().size() > 0)
				{
					recordNode.add(new DefaultMutableTreeNode(null));
				}
				if (group.getGroupType() == 0)
				{
					String groupRecordType = group.getGroupRecordType();

					if (SHOW_ALL || (!groupRecordType.equals("WRLD") && !groupRecordType.equals("CELL") && !groupRecordType.equals("DIAL")))
					{
						String editorID = record.getEditorID();
						Enumeration<?> nodes = groupNode.children();
						while (nodes.hasMoreElements())
						{
							DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
							PluginRecord nodeRecord = (PluginRecord) node.getUserObject();
							String nodeEditorID = nodeRecord.getEditorID();
							if (nodeEditorID != null && editorID.compareToIgnoreCase(nodeEditorID) < 0)
							{
								insertNode = true;
								break;
							}
							index++;
						}
					}
				}
			}
			if (insertNode)
			{
				groupNode.insert(recordNode, index);
			}
			else
			{
				groupNode.add(recordNode);
			}
		}

	}

	private static void createRecordChildren(DefaultMutableTreeNode recordNode, PluginRecord record)
	{
		for (Subrecord subrecord : record.getSubrecords())
		{
			DefaultMutableTreeNode subrecordNode = new DefaultMutableTreeNode(subrecord);
			recordNode.add(subrecordNode);
		}
	}

	@Override
	public void treeExpanded(TreeExpansionEvent event)
	{
		JTree tree = (JTree) event.getSource();
		TreePath treePath = event.getPath();
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
		Object userObject = node.getUserObject();
		if ((userObject instanceof PluginRecord) && !(userObject instanceof PluginGroup))
		{
			PluginRecord record = (PluginRecord) userObject;
			DefaultMutableTreeNode subrecordNode = (DefaultMutableTreeNode) node.getFirstChild();
			if (subrecordNode.getUserObject() == null)
			{
				node.removeAllChildren();
				createRecordChildren(node, record);
				DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
				model.nodeStructureChanged(node);
			}
		}
	}

	@Override
	public void treeCollapsed(TreeExpansionEvent treeexpansionevent)
	{
	}
	
	
	private static class PluginRecordTreeNode extends DefaultMutableTreeNode {
		public PluginRecordTreeNode(PluginRecord record) {
			super(record);
		}

		@Override
		public String toString() {
			if (userObject == null) {
	            return "";
	        } else if (userObject instanceof PluginGroup) {
	            return userObject.toString() + " : " + humanReadableByteCountSI(((PluginGroup)userObject).getRecordDeepDataSize());	            
	        } else if (userObject instanceof PluginRecord) {
	            return userObject.toString() + " : " + humanReadableByteCountSI(((PluginRecord)userObject).getRecordDataLen());
	        } else {
	            return userObject.toString();
	        }
		}
		public static String humanReadableByteCountSI(long bytes) {
		    if (-1000 < bytes && bytes < 1000) {
		        return bytes + " B";
		    }
		    StringCharacterIterator ci = new StringCharacterIterator("kMGTPE");
		    while (bytes <= -999_950 || bytes >= 999_950) {
		        bytes /= 1000;
		        ci.next();
		    }
		    return String.format("%.1f %cB", bytes / 1000.0, ci.current());
		}
	}

}
