package net.eads.astrium.it3s.sourcechecksum;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.eads.astrium.it3s.sourcechecksum.generator.ChecksumGenerator;
import net.eads.astrium.it3s.sourcechecksum.generator.FsChecksumGenerator;
import net.eads.astrium.it3s.sourcechecksum.listener.ChecksumListener;
import net.eads.astrium.it3s.sourcechecksum.listener.ConsoleOutputListener;
import net.eads.astrium.it3s.sourcechecksum.resource.AbstractDirectory;
import net.eads.astrium.it3s.sourcechecksum.resource.AbstractFile;
import net.eads.astrium.it3s.sourcechecksum.resource.AbstractResource;

/**
 * This class is the main application entry point.
 * 
 * @author Bruce BUJON
 *
 */
public class ChecksumTool {

	/**
	 * The main procedure.
	 * 
	 * @param args
	 *            The CLI parameters.
	 */
	public static void main(String[] args) {
		/*
		 * Parse parameters.
		 */
		// Declare parameters
		boolean listMode = false;
		boolean diffMode = false;
		// Declare file system path parameter
		Path path = null;
		// Declare repository and URL parameters
		String repository = null;
		String url = null;
		// Declare output file parameter
		File outputFile = null;
		// Parse each parameter
		for (int index = 0; index<args.length; index++) {
			// Get next argument
			String arg = args[index];
			// Check list mode
			if (arg.equals("--list")) {
				listMode = true;
			}
			// Check diff mode
			else if (arg.equals("--diff")) {
				diffMode = true;
			}
			// Check path
			else if (arg.equals("--path")) {
				// Get next argument as path
				if (++index<args.length)
					path = Paths.get(args[index]);
			}
			// Check repository
			else if (arg.equals("--repository")) {
				// Get next argument as repository
				if (++index<args.length)
					repository = args[index];
			}
			// Check URL
			else if (arg.equals("--url")) {
				// Get next argument as URL
				if (++index<args.length)
					url = args[index];
			}
			// Check output file
			else if (arg.equals("--output")) {
				// Get next argument as output file
				if (++index<args.length)
					outputFile = new File(args[index]);
			}
		}

		/*
		 * Start tool.
		 */
		// Create checksum listener
		ChecksumListener listener = new ConsoleOutputListener();
		// Check mode
		if (listMode) {
			// Declare checksum generator
			ChecksumGenerator checksumGenerator = null;
			// Check path parameter
			if (path!=null) {
				// Create checksum generator on file system
				try {
					checksumGenerator = ChecksumTool.buildFsChecksumGenerator(path);
				} catch (ChecksumException exception) {
					// Notify user then exit
					listener.onError(exception);
					System.exit(0);
				}
			} else if (repository!=null&&url!=null) {
				// Create checksum generator on Subversion
				try {
					checksumGenerator = ChecksumTool.buildSvnChecksumGenerator(path); // TODO
				} catch (ChecksumException exception) {
					// Notify user then exit
					listener.onError(exception);
					System.exit(0);
				}
			} else {
				// Notify user then exit
				System.err.println("Missing path or repository and url parameters.");
				System.exit(0);
			}
			// Check output file parameter
			if (outputFile==null) {
				// Notify user then exit
				System.err.println("Missing output parameter.");
				System.exit(0);
			}
			//
			try {
				// Compute checksums
				AbstractDirectory directory = checksumGenerator.compute(listener);
				// Output checksums
				ChecksumTool.outputResourceChecksums(directory, outputFile);
			} catch (ChecksumException exception) {
				// Notify listener on error
				listener.onError(exception);
			}
		} else if (diffMode) {
			
		} else {
			// Notify user then exit
			System.err.println("Missing list or diff parameter.");
			System.exit(0);
		}
	}

	/**
	 * Create file system checksum generator.
	 * 
	 * @param path
	 *            The path to compute checksums.
	 * @return The built file system checksum generator.
	 * @throws ChecksumException
	 *             Throws exception if the generator could not be built.
	 */
	protected static ChecksumGenerator buildFsChecksumGenerator(Path path) throws ChecksumException {
		return new FsChecksumGenerator(path);
	}

	/**
	 * Create Subversion system checksum generator.
	 * 
	 * @param path
	 *            The path to compute checksums.
	 * @return The built file system checksum generator.
	 * @throws ChecksumException
	 *             Throws exception if the generator could not be built.
	 */
	protected static ChecksumGenerator buildSvnChecksumGenerator(Path path) throws ChecksumException {
		return new FsChecksumGenerator(path);
	}

	/**
	 * Output resource checksums.
	 * 
	 * @param resource
	 *            The resource to output checksum.
	 * @param outputFile
	 *            The output file to store checksums.
	 * @throws ChecksumException
	 *             Throws exception if the checksums could not be output.
	 */
	protected static void outputResourceChecksums(AbstractResource resource, File outputFile) throws ChecksumException {
		// Create an output writer
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath())) {
			// Output resource on the writer
			ChecksumTool.outputResource(writer, resource);
		} catch (IOException exception) {
			throw new ChecksumException("Unable to write checksum file.", exception);
		}
	}

	/**
	 * Output resource checksums.
	 * 
	 * @param writer
	 *            The writer to output checksums.
	 * @param resource
	 *            The resource to output checksum.
	 * @throws IOException
	 *             Throws exception if the checksum could not be output.
	 */
	protected static void outputResource(BufferedWriter writer, AbstractResource resource) throws IOException {
		// Check directory resource type
		if (resource instanceof AbstractDirectory) {
			// Output each child of directory
			for (AbstractResource child : ((AbstractDirectory) resource).getChildren())
				ChecksumTool.outputResource(writer, child);
			// Return
			return;
		}
		// Check file resource type
		else if (resource instanceof AbstractFile) {
			// Get file resource
			AbstractFile file = (AbstractFile) resource;
			// Get file checksum bytes
			// TODO
			byte[] checksumBytes = file.getChecksum();
			if (checksumBytes==null)
				return;
			// Create hash string representation
			StringBuilder stringBuilder = new StringBuilder();
			for (byte b : file.getChecksum())
				stringBuilder.append(String.format("%02x", b));
			// Append file working copy path
			stringBuilder.append('\t');
			stringBuilder.append(file.getPath());
			// Append buffer to output
			writer.write(stringBuilder.toString());
			writer.newLine();
		}
	}
}