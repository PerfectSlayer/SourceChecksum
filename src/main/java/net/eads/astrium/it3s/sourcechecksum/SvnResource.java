package net.eads.astrium.it3s.sourcechecksum;

/**
 * This class represent a Subversion versioned item.
 * 
 * @author Bruce BUJON
 * 
 */
public abstract class SvnResource implements Comparable<SvnResource> {
	/** The resource name. */
	protected final String name;
	/** The resource path cache (<code>null</code> until requested). */
	protected String path;
	/** The resource revision (<code>-1</code> if no revision is specified). */
	protected long revision;
	/** The resource working copy path cache (<code>null</code> until requested). */
	protected String workingCopyPath;
	/** The resource parent (<code>null</code> if no parent). */
	protected SvnResource parent;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            The resource name.
	 */
	public SvnResource(String name) {
		this.name = name;
		this.revision = -1;
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
	 * Get the resource revision.
	 * 
	 * @return The resource revision (<code>-1</code> if no revision is specified).
	 */
	public long getRevision() {
		return this.revision;
	}

	/**
	 * Set the resource revision.
	 * 
	 * @param revision
	 *            The resource revision to set (<code>-1</code> if no revision is specified).
	 */
	public void setRevision(long revision) {
		this.revision = revision;
	}

	/**
	 * Get the resource working copy path.
	 * 
	 * @return The resource working copy path.
	 */
	public String getWorkingCopyPath() {
		// Check if resource working copy path is already cached
		if (this.workingCopyPath==null) {
			// Check if resource has parent
			if (this.parent==null) {
				// Set resource working copy path as name
				this.workingCopyPath = this.name;
			} else {
				// Compose resource working copy path from parent
				this.workingCopyPath = this.parent.getWorkingCopyPath()+"/"+this.name;
			}
		}
		// Return resource working copy path
		return this.workingCopyPath;
	}

	/*
	 * Comparable.
	 */

	@Override
	public int compareTo(SvnResource other) {
		return this.name.compareTo(other.name);
	}
}