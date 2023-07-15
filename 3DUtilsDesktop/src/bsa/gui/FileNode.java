package bsa.gui;

import javax.swing.tree.DefaultMutableTreeNode;

import bsaio.ArchiveEntry;
import bsaio.displayables.Displayable;

public class FileNode extends DefaultMutableTreeNode implements Comparable<FileNode>
{
	private ArchiveEntry entry;

	public FileNode(ArchiveEntry entry)
	{
		super(entry);
		this.entry = entry;
	}

	public ArchiveEntry getEntry()
	{
		return entry;
	}

	@Override
	public boolean equals(Object obj)
	{
		boolean equal = false;
		if (obj != null && (obj instanceof FileNode) && ((Displayable) entry).getFileName()
				.equals(((Displayable) ((FileNode) obj).getEntry()).getFileName()))
			equal = true;
		return equal;
	}

	@Override
	public int compareTo(FileNode compare)
	{
		return ((Displayable) entry).getFileName().compareTo(((Displayable) compare.getEntry()).getFileName());
	}

	@Override
	public String toString()
	{
		return ((Displayable) entry).getFileName();
	}

}