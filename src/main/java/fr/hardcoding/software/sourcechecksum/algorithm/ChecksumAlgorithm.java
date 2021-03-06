package fr.hardcoding.software.sourcechecksum.algorithm;

import java.security.Security;

/**
 * This enumeration represents available algorithms to compute checksums.
 * 
 * @author Bruce BUJON
 *
 */
public enum ChecksumAlgorithm {
	/** The CRC32 algorithm. */
	CRC32("CRC-32"),
	/** The MD5 algorithm. */
	MD5("MD5"),
	/** The SHA-256 algorithm. */
	SHA256("SHA-256");
	
	static {
		// Add custom security provider
		Security.addProvider(new CustomSecurityProvider());
	}

	/** The algorithm name. */
	private final String name;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            The algorithm name.
	 */
	private ChecksumAlgorithm(String name) {
		this.name = name;
	}

	/**
	 * Get the algorithm name.
	 * 
	 * @return The algorithm name.
	 */
	public String getName() {
		return this.name;
	}
}