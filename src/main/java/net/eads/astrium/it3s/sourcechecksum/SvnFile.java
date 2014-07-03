package net.eads.astrium.it3s.sourcechecksum;

/**
 * This class represents a versioned file.
 * 
 * @author Bruce BUJON
 * 
 */
public class SvnFile extends SvnResource {
	/** The resource checksum. */
	private byte[] checksum;
	
	/**
	 * Constructor.
	 * 
	 * @param name
	 *            The file name.
	 */
	public SvnFile(String name) {
		super(name);
	}
	
	/**
	 * Get the resource checksum.
	 * 
	 * @return The resource checksum.
	 */
	public byte[] getChecksum() {
		return this.checksum;
	}

	/**
	 * Set the resource checksum.
	 * 
	 * @param checksum
	 *            The resource checksum to set.
	 */
	public void setChecksum(byte[] checksum) {
		this.checksum = checksum;
	}
}
