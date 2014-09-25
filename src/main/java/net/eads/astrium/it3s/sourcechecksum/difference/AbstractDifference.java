package net.eads.astrium.it3s.sourcechecksum.difference;

import net.eads.astrium.it3s.sourcechecksum.resource.AbstractResource;

/**
 * This class represents a resource difference.
 * 
 * @author Bruce BUJON
 *
 */
public abstract class AbstractDifference {
	/** The left resource related to the difference (may be <code>null</code> if missing from the left part). */
	private final AbstractResource leftResource;
	/** The right resource related to the difference (may be <code>null</code> if missing from the right part). */
	private final AbstractResource rightResource;

	/**
	 * Constructor.
	 * 
	 * @param leftResource
	 *            The left resource related to the difference.
	 * @param rightResource
	 *            The right resource related to the difference.
	 */
	public AbstractDifference(AbstractResource leftResource, AbstractResource rightResource) {
		// Save difference resources
		this.leftResource = leftResource;
		this.rightResource = rightResource;
	}

	/**
	 * Get the left resource related to the difference.
	 * 
	 * @return The left resource related to the difference (may be <code>null</code> if missing from the left part).
	 */
	public AbstractResource getLeftResource() {
		return this.leftResource;
	}

	/**
	 * Get the right resource related to the difference.
	 * 
	 * @return The right resource related to the difference (may be <code>null</code> if missing from the left part).
	 */
	public AbstractResource getRightResource() {
		return this.rightResource;
	}
}