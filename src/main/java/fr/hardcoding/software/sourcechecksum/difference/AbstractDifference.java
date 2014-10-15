package fr.hardcoding.software.sourcechecksum.difference;

import fr.hardcoding.software.sourcechecksum.resource.AbstractResource;

/**
 * This class represents a resource difference.
 * 
 * @author Bruce BUJON
 *
 */
public abstract class AbstractDifference {
	/** The left resource related to the difference (may be <code>null</code> if missing from the left part). */
	protected final AbstractResource leftResource;
	/** The right resource related to the difference (may be <code>null</code> if missing from the right part). */
	protected final AbstractResource rightResource;

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
}