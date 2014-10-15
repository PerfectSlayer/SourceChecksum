package fr.hardcoding.software.sourcechecksum.resource.svn;

/**
 * This interface represents a Subversion specific resource.
 * 
 * @author Bruce BUJON
 *
 */
public interface SvnResource {
	/**
	 * Get the resource revision.
	 * 
	 * @return The resource revision (<code>-1</code> if no revision is specified).
	 */
	public long getRevision();

	/**
	 * Set the resource revision.
	 * 
	 * @param revision
	 *            The resource revision to set (<code>-1</code> if no revision is specified).
	 */
	public void setRevision(long revision);

	/**
	 * Get the resource working copy path.
	 * 
	 * @return The resource working copy path.
	 */
	public String getWorkingCopyPath();
}