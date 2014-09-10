package net.eads.astrium.it3s.sourcechecksum.resource;

/**
 * This class represent an abstract file system item.
 * 
 * @author Bruce BUJON
 * 
 */
public abstract class AbstractResource implements Comparable<AbstractResource> {
	/** The resource name. */
	protected final String name;
	/** The resource path cache (<code>null</code> until requested). */
	protected String path;
	/** The resource parent (<code>null</code> if no parent). */
	protected AbstractDirectory parent;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            The resource name.
	 */
	public AbstractResource(String name) {
		this.name = name;
	}

	/**
	 * Get the resource name.
	 * 
	 * @return The resource name.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Get the resource path.
	 * 
	 * @return The resource path.
	 */
	public String getPath() {
		// Check if resource path is already cached
		if (this.path==null) {
			// Check if resource has parent
			if (this.parent==null) {
				// Set resource path as URL
				this.path = this.name;
			} else {
				// Compose resource path from parent path
				this.path = this.parent.getPath()+"/"+this.name;
			}
		}
		// Return resource path
		return this.path;
	}

	/**
	 * Set the resource path.
	 * 
	 * @param path
	 *            The resource path to set.
	 */
	public void setPath(String path) {
		// Set the resource path cache, useful if not computed when resource is external
		this.path = path;
	}

	/**
	 * Get the resource parent.
	 * 
	 * @return The resource parent (<code>null</code> if no parent).
	 */
	public AbstractDirectory getParent() {
		return this.parent;
	}

	/*
	 * Comparable.
	 */

	@Override
	public int compareTo(AbstractResource other) {
		return this.name.compareTo(other.name);
	}
}