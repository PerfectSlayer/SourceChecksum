package fr.hardcoding.software.sourcechecksum.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import fr.hardcoding.software.sourcechecksum.ChecksumTool;

/**
 * This class represents an abstract directory.
 * 
 * @author Bruce BUJON
 * 
 */
public abstract class AbstractDirectory extends AbstractResource {
	/** The child comparator. */
	protected static final Comparator<AbstractResource> CHILD_COMPARATOR = new Comparator<AbstractResource>() {
		@Override
		public int compare(AbstractResource resource1, AbstractResource resource2) {
			return ChecksumTool.compareResource(resource1, resource2);
		}
	};
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
		this.children = new ArrayList<AbstractResource>();
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

	/**
	 * Recursively sort the child resources.
	 */
	public void sort() {
		Collections.sort(this.children, AbstractDirectory.CHILD_COMPARATOR);
		// Recursively sort each directory
		for (AbstractResource child : this.children) {
			// Check child type
			if (!(child instanceof AbstractDirectory))
				continue;
			// Sort child directory
			((AbstractDirectory) child).sort();
		}
	}

	@Override
	public String toString() {
		return "Directory "+this.getName();
	}
}