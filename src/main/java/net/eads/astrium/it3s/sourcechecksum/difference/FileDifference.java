package net.eads.astrium.it3s.sourcechecksum.difference;

import net.eads.astrium.it3s.sourcechecksum.resource.AbstractFile;

/**
 * This class represents a file difference.
 * 
 * @author Bruce BUJON
 *
 */
public class FileDifference extends AbstractDifference {
	/** The resource difference type. */
	private final FileDifferenceType type;

	/**
	 * Constructor.
	 * 
	 * @param type
	 *            The file difference type.
	 * @param leftFile
	 *            The difference related left file (may be <code>null</code> if missing from the left part).
	 * @param rightFile
	 *            The difference related right file (may be <code>null</code> if missing from the left part).
	 */
	public FileDifference(FileDifferenceType type, AbstractFile leftFile, AbstractFile rightFile) {
		super(leftFile, rightFile);
		// Save file difference type
		this.type = type;
	}

	/**
	 * Get the resource difference type.
	 * 
	 * @return The resource difference type.
	 */
	public FileDifferenceType getType() {
		return this.type;
	}

	/**
	 * Get the left file related to the difference.
	 * 
	 * @return The left file related to the difference (may be <code>null</code> if missing from the left part).
	 */
	public AbstractFile getLeftFile() {
		return (AbstractFile) this.leftResource;
	}

	/**
	 * Get the right file related to the difference.
	 * 
	 * @return The right file related to the difference (may be <code>null</code> if missing from the left part).
	 */
	public AbstractFile getRightFile() {
		return (AbstractFile) this.rightResource;
	}
}