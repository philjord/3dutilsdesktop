package bsa.gui;

import javax.swing.tree.DefaultMutableTreeNode;

public class FolderNode extends DefaultMutableTreeNode implements Comparable<FolderNode>
{
	private String name;

	public FolderNode(String name)
	{
		super(name);
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public boolean equals(Object obj)
	{
		boolean equal = false;
		if (obj != null && (obj instanceof FolderNode) && name.equals(((FolderNode) obj).getName()))
			equal = true;
		return equal;
	}

	public int compareTo(FolderNode compare)
	{
		return name.compareTo(compare.getName());
	}

}