package fr.hardcoding.software.sourcechecksum.generator;

import java.nio.file.PathMatcher;
import java.util.List;

import fr.hardcoding.software.sourcechecksum.ChecksumException;
import fr.hardcoding.software.sourcechecksum.algorithm.ChecksumAlgorithm;
import fr.hardcoding.software.sourcechecksum.listener.ChecksumListener;
import fr.hardcoding.software.sourcechecksum.resource.AbstractDirectory;

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
	 * @param ignoreList
	 *            The list of path matcher to check for ignoring resource.
	 * @param listener
	 *            The listener to notify computation progress.
	 * @return The root directory of resources.
	 * @throws ChecksumException
	 *             Throws exception if checksums could not be computed.
	 */
	public AbstractDirectory compute(ChecksumAlgorithm algorithm, List<PathMatcher> ignoreList, ChecksumListener listener) throws ChecksumException;
}