package net.eads.astrium.it3s.sourcechecksum.difference;

/**
 * This class represents a file difference type.
 * 
 * @author Bruce BUJON
 *
 */
public enum FileDifferenceType {
	/** The left only resource difference type. */
	LEFT_ONLY,
	/** The different resources difference type. */
	DIFFERENT,
	/** The right only resource difference type. */
	RIGHT_ONLY,
}