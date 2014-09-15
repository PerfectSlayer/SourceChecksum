package net.eads.astrium.it3s.sourcechecksum.resource;

/**
 * This class represents an abstract file.
 * 
 * @author Bruce BUJON
 * 
 */
public abstract class AbstractFile extends AbstractResource {
	/** The resource checksum. */
	private byte[] checksum;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            The file name.
	 */
	public AbstractFile(String name) {
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

	@Override
	public String toString() {
		return "File "+this.getName();
	}
}