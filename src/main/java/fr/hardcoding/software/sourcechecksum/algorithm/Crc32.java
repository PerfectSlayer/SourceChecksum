package fr.hardcoding.software.sourcechecksum.algorithm;

import java.security.MessageDigest;
import java.util.zip.CRC32;

/**
 * This class is a message digest for CRC32 algorithm.
 * 
 * @author Bruce BUJON
 *
 */
public class Crc32 extends MessageDigest {
	/** The build-in CRC computer. */
	private final CRC32 crc;

	/**
	 * Constructor.
	 */
	public Crc32() {
		super("CRC-32");
		// Create CRC computer
		this.crc = new CRC32();
	}

	@Override
	protected void engineUpdate(byte input) {
		this.crc.update(input);
	}

	@Override
	protected void engineUpdate(byte[] input, int offset, int len) {
		this.crc.update(input, offset, len);
	}

	@Override
	protected byte[] engineDigest() {
		// Get CRC value
		long value = this.crc.getValue();
		// Convert value to byte array
		byte[] bytes = new byte[4];
		bytes[0] = (byte) ((value&0xFF000000)>>24);
		bytes[1] = (byte) ((value&0x00FF0000)>>16);
		bytes[2] = (byte) ((value&0x0000FF00)>>8);
		bytes[3] = (byte) ((value&0x000000FF)>>0);
		// Return resulting hash value
		return bytes;
	}

	@Override
	protected void engineReset() {
		this.crc.reset();
	}
}