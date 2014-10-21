package fr.hardcoding.software.sourcechecksum.generator;

import fr.hardcoding.software.sourcechecksum.ChecksumException;
import fr.hardcoding.software.sourcechecksum.algorithm.ChecksumAlgorithm;
import fr.hardcoding.software.sourcechecksum.listener.ChecksumListener;
import fr.hardcoding.software.sourcechecksum.resource.AbstractDirectory;
import fr.hardcoding.software.sourcechecksum.resource.PathMatcher;

/**
 * This interface represents the checksum generators.
 * 
 * @author Bruce BUJON
 *
 */
public interface ChecksumGenerator {
	/**
	 * Generate the checksums.
	 * 
	 * @param algorithm
	 *            The algorithm to use to compute checksum.
	 * @param listener
	 *            The listener to notify computation progress.
	 * @param ignoreMatchers
	 *            The list of path matcher to check for ignoring resource.
	 * @return The root directory of resources.
	 * @throws ChecksumException
	 *             Throws exception if checksums could not be computed.
	 */
	public AbstractDirectory compute(ChecksumAlgorithm algorithm, ChecksumListener listener, PathMatcher... ignoreMatchers) throws ChecksumException;
}