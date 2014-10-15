package fr.hardcoding.software.sourcechecksum.algorithm;

import java.security.Provider;

/**
 * This class represents a custom security provider to provide CRC32 algorithm.
 * 
 * @author Bruce BUJON
 *
 */
public class CustomSecurityProvider extends Provider {
	/** Serialization id. */
	private static final long serialVersionUID = -807512265627374571L;

	/**
	 * Constructor.
	 */
	public CustomSecurityProvider() {
		super("CRC securtiy provider", 1, "Checksum tool");
		this.put("MessageDigest.CRC-32", "fr.hardcoding.software.sourcechecksum.algorithm.Crc32");
	}
}