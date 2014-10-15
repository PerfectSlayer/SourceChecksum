package fr.hardcoding.software.sourcechecksum.listener;

/**
 * This interface is used to notify source checksum computation progress.
 * 
 * @author m027017
 *
 */
public interface ChecksumListener {
	/**
	 * Notify start listing source files.
	 */
	public void onStart();

	/**
	 * Notify checksum computation.
	 * 
	 * @param percent
	 *            The percent of source already checked (from <code>0</code> to <code>100</code> included).
	 */
	public void onProgress(int percent);

	/**
	 * Notify checksum computation done.
	 */
	public void onDone();

	/**
	 * Notify something goes wrong.
	 * 
	 * @param exception
	 *            The related exception.
	 */
	public void onError(Exception exception);

	/**
	 * Notify a debug message.
	 * 
	 * @param message
	 *            The debug message.
	 */
	public void onDebug(String message);
}