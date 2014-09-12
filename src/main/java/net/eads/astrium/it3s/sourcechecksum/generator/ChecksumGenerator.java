package net.eads.astrium.it3s.sourcechecksum.generator;

import net.eads.astrium.it3s.sourcechecksum.ChecksumException;
import net.eads.astrium.it3s.sourcechecksum.listener.ChecksumListener;
import net.eads.astrium.it3s.sourcechecksum.resource.AbstractDirectory;

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
	 * @param listener
	 *            The listener to notify computation progress.
	 * @return The root directory of resources.
	 * @throws ChecksumException
	 *             Throws exception if checksums could not be computed.
	 */
	public AbstractDirectory compute(ChecksumListener listener) throws ChecksumException;
}