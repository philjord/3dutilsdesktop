package bsa.tasks;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class ArchiveFileFilter extends FileFilter
{

	public ArchiveFileFilter()
	{
	}

	public String getDescription()
	{
		return "Archive Files (*.bsa, *.ba2)";
	}

	public boolean accept(File file)
	{
		boolean accept = false;
		if (file.isFile())
		{
			String name = file.getName();
			int sep = name.lastIndexOf('.');
			if (sep > 0)
			{
				String extension = name.substring(sep);
				if (extension.equalsIgnoreCase(".bsa") || extension.equalsIgnoreCase(".ba2"))
					accept = true;
			}
		}
		else
		{
			accept = true;
		}
		return accept;
	}
}