package net.eads.astrium.it3s;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.eads.astrium.it3s.sourcechecksum.ChecksumException;
import net.eads.astrium.it3s.sourcechecksum.ChecksumTool;
import net.eads.astrium.it3s.sourcechecksum.algorithm.ChecksumAlgorithm;
import net.eads.astrium.it3s.sourcechecksum.generator.FsChecksumGenerator;
import net.eads.astrium.it3s.sourcechecksum.listener.ConsoleOutputListener;
import net.eads.astrium.it3s.sourcechecksum.resource.AbstractDirectory;
import net.eads.astrium.it3s.sourcechecksum.resource.AbstractFile;
import net.eads.astrium.it3s.sourcechecksum.resource.AbstractResource;

/**
 * This class is a test case for the checksum tools.
 */
public class ChecksumToolTestCase extends TestCase {
	/*
	 * Static CRC-32 checksum values.
	 */
	/** The a.png CRC-32 checksum value. */
	private static final byte[] A_PNG_CRC32_CHECKSUM = new byte[] { (byte) 0xbe, (byte) 0x99, 0x04, 0x3d };
	/** The a.txt CRC-32 checksum value. */
	private static final byte[] A_TXT_CRC32_CHECKSUM = new byte[] { 0x22, (byte) 0xd0, 0x19, (byte) 0x88 };
	/** The aa.txt CRC-32 checksum value. */
	private static final byte[] AA_TXT_CRC32_CHECKSUM = new byte[] { (byte) 0x96, (byte) 0x89, 0x73, (byte) 0xc4 };
	/** The b.txt CRC-32 checksum value. */
	private static final byte[] B_TXT_CRC32_CHECKSUM = new byte[] { 0x26, 0x22, 0x31, 0x67 };
	/*
	 * Static MD5 checksum values.
	 */
	/** The a.png MD5 checksum value. */
	private static final byte[] A_PNG_MD5_CHECKSUM = new byte[] { 0x54, (byte) 0xa3, (byte) 0xf5, (byte) 0xf1, 0x0f, 0x1c, 0x6c, 0x3e, 0x7a, (byte) 0x97,
			(byte) 0xfa, (byte) 0xed, 0x66, (byte) 0xe5, 0x1a, 0x63 };
	/** The a.txt MD5 checksum value. */
	private static final byte[] A_TXT_MD5_CHECKSUM = new byte[] { 0x3c, (byte) 0x8d, (byte) 0xc8, (byte) 0xcf, 0x3b, 0x14, (byte) 0x90, 0x6b, (byte) 0xe0,
			0x68, 0x3a, (byte) 0xf2, (byte) 0xf4, (byte) 0xd8, (byte) 0xc0, 0x45 };
	/** The aa.txt MD5 checksum value. */
	private static final byte[] AA_TXT_MD5_CHECKSUM = new byte[] { (byte) 0xc7, 0x3e, 0x2a, 0x06, 0x5e, 0x42, (byte) 0xe7, 0x0c, (byte) 0x96, (byte) 0xe3,
			0x7c, 0x45, 0x62, 0x2b, 0x05, (byte) 0xbf };
	/** The b.txt MD5 checksum value. */
	private static final byte[] B_TXT_MD5_CHECKSUM = new byte[] { (byte) 0xe0, (byte) 0xae, (byte) 0xa9, (byte) 0x83, (byte) 0x83, 0x6a, 0x7a, (byte) 0xe2,
			(byte) 0xa3, 0x1b, (byte) 0xbf, 0x07, (byte) 0xc3, (byte) 0x95, 0x6a, 0x0a };
	/*
	 * Static SHA-256 checksum values.
	 */
	/** The a.png SHA-256 checksum value. */
	private static final byte[] A_PNG_SHA256_CHECKSUM = new byte[] { 0x37, 0x28, (byte) 0x8a, 0x2f, 0x27, 0x60, (byte) 0x81, (byte) 0x9b, (byte) 0xf2,
			(byte) 0xb1, 0x14, (byte) 0x84, (byte) 0xdf, (byte) 0xfb, (byte) 0x92, 0x76, (byte) 0xe9, (byte) 0xcf, 0x79, 0x36, (byte) 0x8d, (byte) 0x82, 0x08,
			0x39, (byte) 0x9b, 0x0f, (byte) 0xad, 0x65, 0x38, (byte) 0xbc, 0x17, (byte) 0x95 };
	/** The a.txt SHA-256 checksum value. */
	private static final byte[] A_TXT_SHA256_CHECKSUM = new byte[] { 0x2f, 0x10, (byte) 0xd6, 0x44, 0x13, 0x73, (byte) 0xdb, 0x1c, 0x12, (byte) 0xe3,
			(byte) 0xa8, 0x43, 0x32, (byte) 0xb0, 0x71, 0x64, 0x41, (byte) 0x81, 0x08, 0x6b, 0x54, 0x45, (byte) 0xf9, (byte) 0x9e, 0x6d, 0x21, (byte) 0xee,
			0x71, (byte) 0xf5, 0x31, (byte) 0xd8, (byte) 0xd5 };
	/** The aa.txt SHA-256 checksum value. */
	private static final byte[] AA_TXT_SHA256_CHECKSUM = new byte[] { (byte) 0xa7, (byte) 0x88, 0x46, (byte) 0xa9, 0x58, 0x33, 0x25, (byte) 0xa1, 0x79, 0x10,
			0x3c, (byte) 0xce, 0x1b, (byte) 0xc6, 0x24, (byte) 0x9d, 0x38, (byte) 0x8a, (byte) 0x88, 0x4e, (byte) 0xf6, 0x09, (byte) 0xcb, 0x32, (byte) 0xd7,
			(byte) 0x93, 0x0c, 0x4f, 0x63, 0x1f, 0x72, 0x4c };
	/** The b.txt SHA-256 checksum value. */
	private static final byte[] B_TXT_SHA256_CHECKSUM = new byte[] { (byte) 0xae, 0x03, (byte) 0xa2, (byte) 0xb1, 0x41, 0x52, 0x0b, (byte) 0xde, 0x4c,
			(byte) 0xad, 0x2c, 0x48, 0x61, 0x2e, 0x42, 0x18, 0x71, (byte) 0x83, 0x64, 0x38, (byte) 0x9b, 0x72, 0x24, (byte) 0xea, (byte) 0xf0, 0x55,
			(byte) 0x8e, (byte) 0xf1, 0x3b, 0x5d, (byte) 0x8b, (byte) 0xcd };

	/**
	 * Create the suite of tests.
	 * 
	 * @return The created suite of tests.
	 */
	public static Test suite() {
		// Return test suite
		return new TestSuite(ChecksumToolTestCase.class);
	}

	/**
	 * Constructor.
	 *
	 * @param testName
	 *            The name of the test case.
	 */
	public ChecksumToolTestCase(String testName) {
		super(testName);
	}

	/**
	 * Test file system checksum generator.
	 */
	public void testFsChecksumGenerator() {
		/*
		 * Create checksum generator.
		 */
		// Create paths to compute checksums
		Path leftPath = Paths.get("src", "test", "resources", "files", "left");
		Path rightPath = Paths.get("src", "test", "resources", "files", "right");
		// Create output listener
		ConsoleOutputListener listener = new ConsoleOutputListener();
		// Declare checksum generators
		FsChecksumGenerator leftChecksumGenerator = null;
		FsChecksumGenerator rightChecksumGenerator = null;
		try {
			// Create checksum generators
			leftChecksumGenerator = new FsChecksumGenerator(leftPath);
			rightChecksumGenerator = new FsChecksumGenerator(rightPath);
		} catch (ChecksumException exception) {
			fail("Unable to create checksum generators.");
		}
		// Declare left directory
		AbstractDirectory leftDirectory = null;
		/*
		 * Check each checksum for CRC-32 algorithm.
		 */
		try {
			// Compute checksums
			leftDirectory = leftChecksumGenerator.compute(ChecksumAlgorithm.CRC32, listener);
		} catch (ChecksumException exception) {
			fail("Unable to compute CRC-32 checksums.");
		}
		// Check number of child
		List<AbstractResource> children = leftDirectory.getChildren();
		assertEquals(4, children.size());
		// Check each child checksum
		for (AbstractResource child : children) {
			// Check child type
			if (!(child instanceof AbstractFile))
				fail("The resource "+child.getName()+" is not a file.");
			AbstractFile file = (AbstractFile) child;
			// Check child name
			switch (child.getName()) {
			case "a.png":
				assertTrue(Arrays.equals(file.getChecksum(), A_PNG_CRC32_CHECKSUM));
				break;
			case "a.txt":
				assertTrue(Arrays.equals(file.getChecksum(), A_TXT_CRC32_CHECKSUM));
				break;
			case "aa.txt":
				assertTrue(Arrays.equals(file.getChecksum(), AA_TXT_CRC32_CHECKSUM));
				break;
			case "b.txt":
				assertTrue(Arrays.equals(file.getChecksum(), B_TXT_CRC32_CHECKSUM));
				break;
			default:
				fail("The resource "+child.getName()+" should not be computed.");
			}
		}
		/*
		 * Check each checksum for MD5 algorithm.
		 */
		try {
			// Compute checksums
			leftDirectory = leftChecksumGenerator.compute(ChecksumAlgorithm.MD5, listener);
		} catch (ChecksumException exception) {
			fail("Unable to compute CRC-32 checksums.");
		}
		// Check number of child
		children = leftDirectory.getChildren();
		assertEquals(4, children.size());
		// Check each child checksum
		for (AbstractResource child : children) {
			// Check child type
			if (!(child instanceof AbstractFile))
				fail("The resource "+child.getName()+" is not a file.");
			AbstractFile file = (AbstractFile) child;
			// Check child name
			switch (child.getName()) {
			case "a.png":
				assertTrue(Arrays.equals(file.getChecksum(), A_PNG_MD5_CHECKSUM));
				break;
			case "a.txt":
				assertTrue(Arrays.equals(file.getChecksum(), A_TXT_MD5_CHECKSUM));
				break;
			case "aa.txt":
				assertTrue(Arrays.equals(file.getChecksum(), AA_TXT_MD5_CHECKSUM));
				break;
			case "b.txt":
				assertTrue(Arrays.equals(file.getChecksum(), B_TXT_MD5_CHECKSUM));
				break;
			default:
				fail("The resource "+child.getName()+" should not be computed.");
			}
		}
		/*
		 * Check each checksum for SHA-256 algorithm.
		 */
		try {
			// Compute checksums
			leftDirectory = leftChecksumGenerator.compute(ChecksumAlgorithm.SHA256, listener);
		} catch (ChecksumException exception) {
			fail("Unable to compute CRC-32 checksums.");
		}
		// Check number of child
		children = leftDirectory.getChildren();
		assertEquals(4, children.size());
		// Check each child checksum
		for (AbstractResource child : children) {
			// Check child type
			if (!(child instanceof AbstractFile))
				fail("The resource "+child.getName()+" is not a file.");
			AbstractFile file = (AbstractFile) child;
			// Check child name
			switch (child.getName()) {
			case "a.png":
				assertTrue(Arrays.equals(file.getChecksum(), A_PNG_SHA256_CHECKSUM));
				break;
			case "a.txt":
				assertTrue(Arrays.equals(file.getChecksum(), A_TXT_SHA256_CHECKSUM));
				break;
			case "aa.txt":
				assertTrue(Arrays.equals(file.getChecksum(), AA_TXT_SHA256_CHECKSUM));
				break;
			case "b.txt":
				assertTrue(Arrays.equals(file.getChecksum(), B_TXT_SHA256_CHECKSUM));
				break;
			default:
				fail("The resource "+child.getName()+" should not be computed.");
			}
		}
		/*
		 * Check resource sort.
		 */
		// Sort resource
		leftDirectory.sort();
		// Get sorted children
		children = leftDirectory.getChildren();
		// Check child order
		assertEquals("a.png", children.get(0).getName());
		assertEquals("a.txt", children.get(1).getName());
		assertEquals("aa.txt", children.get(2).getName());
		assertEquals("b.txt", children.get(3).getName());
		/*
		 * Check diff algorithm.
		 */
		// Declare temporary output file
		Path outputPath = null;
		try {
			outputPath = Files.createTempFile("test", ".tmp");
		} catch (IOException exception) {
			fail("Unable to create temporary output file.");
		}
		// Declare right directory
		AbstractDirectory rightDirectory = null;
		try {
			// Compute checksums
			rightDirectory = rightChecksumGenerator.compute(ChecksumAlgorithm.SHA256, listener);
		} catch (ChecksumException exception) {
			fail("Unable to compute CRC-32 checksums.");
		}
		try {
			// Output diff checksums
			ChecksumTool.outputDiffResourceChecksum(leftDirectory, rightDirectory, outputPath.toFile());
		} catch (ChecksumException exception) {
			fail("Unable to output diff checksums.");
		}
		// Check output content
		try {
			Iterator<String> outputLines = Files.lines(outputPath).iterator();
			assertEquals("37288a2f2760819bf2b11484dffb9276e9cf79368d8208399b0fad6538bc1795	left/a.png		", outputLines.next());
			assertEquals("a78846a9583325a179103cce1bc6249d388a884ef609cb32d7930c4f631f724c	left/aa.txt"
					+"	56b6e44738b581b82279affe6ed52e63ecfda07fe4597f9b81faeff59dc6695a	right/aa.txt", outputLines.next());
			assertEquals("		cc7416e74afdbb91c97a0b2219d61c354f3a56fe4d3f3652df8cbbda370ebf98	right/c.txt", outputLines.next());
			assertEquals(false, outputLines.hasNext());
		} catch (IOException exception) {
			fail("Unable to read output diff checksums.");
		}
	}
}