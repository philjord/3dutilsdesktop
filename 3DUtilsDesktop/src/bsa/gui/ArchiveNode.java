package bsa.gui;

import javax.swing.tree.DefaultMutableTreeNode;

import archive.ArchiveFile;

public class ArchiveNode extends DefaultMutableTreeNode
{
	private ArchiveFile archiveFile;

	public ArchiveNode()
	{
		super("(closed)");
	}

	public ArchiveNode(ArchiveFile archiveFile)
	{
		super(archiveFile);
		this.archiveFile = archiveFile;
	}

	public ArchiveFile getArchiveFile()
	{
		return archiveFile;
	}
}