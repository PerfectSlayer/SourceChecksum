package net.eads.astrium.it3s.sourcechecksum;

/**
 * The generic checksum exception.
 * 
 * @author Bruce BUJON
 * 
 */
public class ChecksumException extends Exception {
	/** Serialization id. */
	private static final long serialVersionUID = -9060937293291756471L;

	/**
	 * Constructor.
	 * 
	 * @param message
	 *            The exception message.
	 */
	public ChecksumException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 * 
	 * @param message
	 *            The exception message.
	 * @param cause
	 *            The exception cause.
	 */
	public ChecksumException(String message, Throwable cause) {
		super(message, cause);
	}
}