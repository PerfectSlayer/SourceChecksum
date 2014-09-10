package net.eads.astrium.it3s.sourcechecksum.resource.svn;

import net.eads.astrium.it3s.sourcechecksum.resource.AbstractResource;

/**
 * This class represent a Subversion versioned item.
 * 
 * @author Bruce BUJON
 * 
 */
public class SvnResourceAttributes {
	/** The resource revision (<code>-1</code> if no revision is specified). */
	protected long revision;
	/** The resource working copy path cache (<code>null</code> until requested). */
	protected String workingCopyPath;

	/**
	 * Constructor.
	 */
	public SvnResourceAttributes() {
		this.revision = -1;
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
	 * @param resource
	 *            The resource to get working copy path.
	 * @return The resource working copy path.
	 */
	public String getWorkingCopyPath(AbstractResource resource) {
		// Check if resource working copy path is already cached
		if (this.workingCopyPath==null) {
			// Check if resource has parent
			if (resource.getParent()==null||!(resource.getParent() instanceof SvnResource)) {
				// Set resource working copy path as name
				this.workingCopyPath = resource.getName();
			} else {
				// Compose resource working copy path from parent
				this.workingCopyPath = ((SvnResource) resource.getParent()).getWorkingCopyPath()+"/"+resource.getName();
			}
		}
		// Return resource working copy path
		return this.workingCopyPath;
	}
}