package net.eads.astrium.it3s.sourcechecksum.listener;

/**
 * This class is an abstract stub checksum listener.
 * 
 * @author Bruce BUJON
 *
 */
public class AbstractChecksumListener implements ChecksumListener {
	/*
	 * Checksum listener.
	 */

	@Override
	public void onStart() {
		// Stub method
	}

	@Override
	public void onProgress(int percent) {
		// Stub method
	}

	@Override
	public void onDone() {
		// Stub method
	}

	@Override
	public void onError(Exception exception) {
		// Stub method
	}

	@Override
	public void onDebug(String message) {
		// Stub method
	}
}