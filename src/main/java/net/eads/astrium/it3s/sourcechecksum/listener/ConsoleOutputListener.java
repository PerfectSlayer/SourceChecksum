package net.eads.astrium.it3s.sourcechecksum.listener;

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
	public void onProgress(int percent) {
		System.out.print("[");
		for (int i = 0; i<20; i++) {
			System.out.print(i*5<percent ? "#" : " ");
		}
		System.out.print("] "+percent+"%\r");
	}

	@Override
	public void onDone() {
		System.out.println("Source checksums successfully computed.");
	}

	@Override
	public void onError(Exception exception) {
		// Display exception to the console
		exception.printStackTrace();
	}
}