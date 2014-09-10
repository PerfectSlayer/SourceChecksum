package net.eads.astrium.it3s.sourcechecksum.resource.svn;

import net.eads.astrium.it3s.sourcechecksum.resource.AbstractFile;

/**
 * This class represents a file system file.
 * 
 * @author Bruce BUJON
 * 
 */
public class SvnFile extends AbstractFile implements SvnResource {
	/** The Subversion resource attributes. */
	private SvnResourceAttributes attributes;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            The file name.
	 */
	public SvnFile(String name) {
		super(name);
		// Create Subversion resource attributes
		this.attributes = new SvnResourceAttributes();
	}

	/*
	 * SVN Resource.
	 */

	@Override
	public long getRevision() {
		return this.attributes.getRevision();
	}

	@Override
	public void setRevision(long revision) {
		this.attributes.setRevision(revision);
	}

	@Override
	public String getWorkingCopyPath() {
		return this.attributes.getWorkingCopyPath(this);
	}
}