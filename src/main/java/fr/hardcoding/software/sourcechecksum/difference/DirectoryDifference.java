package fr.hardcoding.software.sourcechecksum.difference;

import java.util.ArrayList;
import java.util.List;

import fr.hardcoding.software.sourcechecksum.resource.AbstractDirectory;

/**
 * This class represents a directory difference.
 * 
 * @author Bruce BUJON
 *
 */
public class DirectoryDifference extends AbstractDifference {
	/** The child resource differences. */
	private final List<AbstractDifference> differences;

	/**
	 * Constructor.
	 * 
	 * @param leftDirectory
	 *            The difference related left directory (may be <code>null</code> if missing from the left part).
	 * @param rightDirectory
	 *            The difference related right directory (may be <code>null</code> if missing from the left part).
	 */
	public DirectoryDifference(AbstractDirectory leftDirectory, AbstractDirectory rightDirectory) {
		super(leftDirectory, rightDirectory);
		// Create difference collection
		this.differences = new ArrayList<>();
	}

	/**
	 * Check if directory has differences.
	 * 
	 * @return <code>true</code> if directory has differences, <code>false</code> otherwise.
	 */
	public boolean hasDifference() {
		return !this.differences.isEmpty();
	}

	/**
	 * Add a resource difference to the directory difference.
	 * 
	 * @param difference
	 *            The resource difference to add.
	 */
	public void addDifference(AbstractDifference difference) {
		this.differences.add(difference);
	}

	/**
	 * Get the directory differences.
	 * 
	 * @return The directory differences.
	 */
	public List<AbstractDifference> getDifferences() {
		return this.differences;
	}

	/**
	 * Get the left directory related to the difference.
	 * 
	 * @return The left directory related to the difference (may be <code>null</code> if missing from the left part).
	 */
	public AbstractDirectory getLeftDirectory() {
		return (AbstractDirectory) this.leftResource;
	}

	/**
	 * Get the right directory related to the difference.
	 * 
	 * @return The right directory related to the difference (may be <code>null</code> if missing from the left part).
	 */
	public AbstractDirectory getRightDirectory() {
		return (AbstractDirectory) this.rightResource;
	}
}