package bsa.tasks;

import java.io.FileNotFoundException;
import java.nio.channels.FileChannel;

import bsa.gui.StatusDialog;
import bsa.source.DDSToKTXBsaConverter;
import bsaio.ArchiveFile;

public class CreateTaskDDStoKTXBsa extends Thread {

	private StatusDialog			statusDialog;

	private boolean					completed;

	private DDSToKTXBsaConverter ddsToKTXBsaConverter;

	public CreateTaskDDStoKTXBsa(java.io.File outputArchiveFile, ArchiveFile inputArchive, StatusDialog statusDialog) {
		if(!outputArchiveFile.getName().endsWith("_ktx.bsa")) {
			System.out.println("outputArchiveFile " + outputArchiveFile.getName() + " REALLY shold end with \"_ktx.bsa\" ");
		}
		this.statusDialog = statusDialog;
		try {
			@SuppressWarnings("resource") // not closed until run finished
			FileChannel fco = new java.io.RandomAccessFile(outputArchiveFile, "rw").getChannel();
		
			// I can use fco twice because it comes from a RAF		
			ddsToKTXBsaConverter = new DDSToKTXBsaConverter(fco, fco, inputArchive, statusDialog);
        
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		ddsToKTXBsaConverter.start();
		try {
			ddsToKTXBsaConverter.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}	
		if(statusDialog != null)
			statusDialog.closeDialog(completed);		
	}
  
}