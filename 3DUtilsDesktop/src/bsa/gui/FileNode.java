package bsa.gui;

import javax.swing.tree.DefaultMutableTreeNode;

import archive.ArchiveEntry;
import archive.displayables.DisplayableArchiveEntry;

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

	public boolean equals(Object obj)
	{
		boolean equal = false;
		if (obj != null && (obj instanceof FileNode) && ((DisplayableArchiveEntry) entry).getFileName()
				.equals(((DisplayableArchiveEntry) ((FileNode) obj).getEntry()).getFileName()))
			equal = true;
		return equal;
	}

	public int compareTo(FileNode compare)
	{
		return ((DisplayableArchiveEntry) entry).getFileName().compareTo(((DisplayableArchiveEntry) compare.getEntry()).getFileName());
	}

	public String toString()
	{
		return ((DisplayableArchiveEntry) entry).getFileName();
	}

}