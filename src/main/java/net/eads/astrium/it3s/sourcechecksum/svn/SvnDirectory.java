package net.eads.astrium.it3s.sourcechecksum.svn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class represents a versioned directory.
 * 
 * @author Bruce BUJON
 * 
 */
public class SvnDirectory extends SvnResource {
	/** The resource children. */
	protected final List<SvnResource> children;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            The directory name.
	 */
	public SvnDirectory(String name) {
		super(name);
		this.children = new ArrayList<SvnResource>(); // TODO sorted implementation
	}

	/**
	 * Add a child resource.
	 * 
	 * @param resource
	 *            The child resource to add.
	 */
	public void addChild(SvnResource resource) {
		// Set resource parent
		resource.parent = this;
		// Set resource revision
		if (this.revision!=-1&&resource.revision==-1)
			resource.revision = this.revision;
		// Add resource to children
		this.children.add(resource);
	}

	/**
	 * Get the resource children.
	 * 
	 * @return The resource children.
	 */
	public List<SvnResource> getChildren() {
		return Collections.unmodifiableList(this.children);
	}
}