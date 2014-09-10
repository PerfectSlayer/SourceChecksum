package net.eads.astrium.it3s.sourcechecksum.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class represents an abstract directory.
 * 
 * @author Bruce BUJON
 * 
 */
public abstract class AbstractDirectory extends AbstractResource {
	/** The resource children. */
	protected final List<AbstractResource> children;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            The directory name.
	 */
	public AbstractDirectory(String name) {
		super(name);
		this.children = new ArrayList<>(); // TODO sorted implementation
	}

	/**
	 * Add a child resource.
	 * 
	 * @param resource
	 *            The child resource to add.
	 */
	public void addChild(AbstractResource resource) {
		// Set resource parent
		resource.parent = this;
		// Add resource to children
		this.children.add(resource);
	}

	/**
	 * Get the resource children.
	 * 
	 * @return The resource children.
	 */
	public List<AbstractResource> getChildren() {
		return Collections.unmodifiableList(this.children);
	}
}