package net.eads.astrium.it3s.sourcechecksum.resource.svn;

import net.eads.astrium.it3s.sourcechecksum.resource.AbstractDirectory;
import net.eads.astrium.it3s.sourcechecksum.resource.AbstractResource;

/**
 * This class represents a versioned directory.
 * 
 * @author Bruce BUJON
 * 
 */
public class SvnDirectory extends AbstractDirectory implements SvnResource {
	/** The Subversion resource attributes. */
	private SvnResourceAttributes attributes;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            The directory name.
	 */
	public SvnDirectory(String name) {
		super(name);
		// Create Subversion resource attributes
		this.attributes = new SvnResourceAttributes();
	}

	/**
	 * Add a child resource.
	 * 
	 * @param resource
	 *            The child resource to add.
	 */
	public void addChild(AbstractResource resource) {
		// Delegate add child resource
		super.addChild(resource);
		// Set resource revision
		if (resource instanceof SvnResource&&this.getRevision()!=-1&&((SvnResource) resource).getRevision()==-1)
			((SvnDirectory) resource).setRevision(this.getRevision());
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