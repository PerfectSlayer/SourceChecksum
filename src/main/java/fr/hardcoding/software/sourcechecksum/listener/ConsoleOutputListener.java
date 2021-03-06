package fr.hardcoding.software.sourcechecksum.listener;

/**
 * This class is a listener implementation to display progress to the standard output.
 * 
 * @author Bruce BUJON
 *
 */
public class ConsoleOutputListener implements ChecksumListener {
	/*
	 * Checksum Worker.
	 */

	@Override
	public void onStart() {
		System.out.print("Listing source files...\r");
	}

	@Override
	public synchronized void onProgress(int percent) {
		// Display ASCII progress bar
		System.out.print("\n[");
		for (int i = 0; i<50; i++)
			System.out.print(i*2<percent ? "#" : " ");
		System.out.print("] "+percent+"%\r");
		System.out.flush();
	}

	@Override
	public void onDone() {
		System.out.println("\nSource checksums successfully computed.");
	}

	@Override
	public void onError(Exception exception) {
		// Display error message
		System.out.println("\nChecksum could not be computed: "+exception.getMessage());
		// Display exception to the console
		exception.printStackTrace();
	}

	@Override
	public void onDebug(String message) {
		System.out.print("\n[debug] "+message);
	}
}