package fr.hardcoding.software.sourcechecksum;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.hardcoding.software.sourcechecksum.algorithm.ChecksumAlgorithm;
import fr.hardcoding.software.sourcechecksum.difference.AbstractDifference;
import fr.hardcoding.software.sourcechecksum.difference.DirectoryDifference;
import fr.hardcoding.software.sourcechecksum.difference.FileDifference;
import fr.hardcoding.software.sourcechecksum.difference.FileDifferenceType;
import fr.hardcoding.software.sourcechecksum.generator.ChecksumGenerator;
import fr.hardcoding.software.sourcechecksum.generator.FsChecksumGenerator;
import fr.hardcoding.software.sourcechecksum.generator.SvnChecksumGenerator;
import fr.hardcoding.software.sourcechecksum.listener.ChecksumListener;
import fr.hardcoding.software.sourcechecksum.listener.ConsoleOutputListener;
import fr.hardcoding.software.sourcechecksum.resource.AbstractDirectory;
import fr.hardcoding.software.sourcechecksum.resource.AbstractFile;
import fr.hardcoding.software.sourcechecksum.resource.AbstractResource;
import fr.hardcoding.software.sourcechecksum.resource.svn.SvnResource;

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
		// Create checksum listener
		ChecksumListener listener = new ConsoleOutputListener();
		/*
		 * Parse parameters.
		 */
		// Create options declaration
		Options options = new Options();
		// Create list option
		OptionBuilder.withLongOpt("list");
		OptionBuilder.withDescription("Compute checksums");
		Option listOption = OptionBuilder.create();
		// Create diff option
		OptionBuilder.withLongOpt("diff");
		OptionBuilder.withDescription("Compute version differences");
		Option diffOption = OptionBuilder.create();
		// Create mode option group
		OptionGroup modeGroup = new OptionGroup();
		modeGroup.setRequired(true);
		modeGroup.addOption(diffOption);
		modeGroup.addOption(listOption);
		options.addOptionGroup(modeGroup);
		// Create path option
		OptionBuilder.withLongOpt("path");
		OptionBuilder.withDescription("The paths to compute checksums or differences");
		OptionBuilder.hasArgs(2);
		Option pathOption = OptionBuilder.create();
		options.addOption(pathOption);
		// Create URL option
		OptionBuilder.withLongOpt("url");
		OptionBuilder.withDescription("The URLs of Subversion resources to compute checksums or differences");
		OptionBuilder.hasArgs(2);
		Option urlOption = OptionBuilder.create();
		options.addOption(urlOption);
		// Create user option
		OptionBuilder.withLongOpt("user");
		OptionBuilder.withDescription("The Subversion user name");
		OptionBuilder.hasArg(true);
		Option userOption = OptionBuilder.create();
		options.addOption(userOption);
		// Create password option
		OptionBuilder.withLongOpt("password");
		OptionBuilder.withDescription("The Subversion user password");
		OptionBuilder.hasArg(true);
		Option passwdOption = OptionBuilder.create();
		options.addOption(passwdOption);
		// Create algorithm option
		OptionBuilder.withLongOpt("algorithm");
		OptionBuilder.withDescription("The checksum algorithm to use (CRC32, MD5 or SHA256 (default))");
		OptionBuilder.hasArg(true);
		Option algorithOption = OptionBuilder.create();
		options.addOption(algorithOption);
		// Create ignore globs option
		OptionBuilder.withLongOpt("ignore");
		OptionBuilder.withDescription("The globs patterns to ignore (semicolon separated list)");
		OptionBuilder.hasArg(true);
		Option ignoreGlobsOption = OptionBuilder.create();
		// Create ignore file option
		OptionBuilder.withLongOpt("ignoreFile");
		OptionBuilder.withDescription("The file with glob patterns to ignore (new line separated file)");
		OptionBuilder.hasArg(true);
		Option ignoreFileOption = OptionBuilder.create();
		// Create ignore group options
		OptionGroup ignoreGroup = new OptionGroup();
		ignoreGroup.addOption(ignoreGlobsOption);
		ignoreGroup.addOption(ignoreFileOption);
		options.addOptionGroup(ignoreGroup);
		// Create output option
		OptionBuilder.withLongOpt("output");
		OptionBuilder.withDescription("The result output file");
		OptionBuilder.hasArg(true);
		OptionBuilder.isRequired(true);
		Option outputOption = OptionBuilder.create();
		options.addOption(outputOption);
		// Check CLI parameters
		if (args.length==0) {
			// Print help
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.printHelp("java -jar source-checksum [parameters]", options);
			// Exit
			System.exit(0);
		}
		// Create command line parse
		CommandLineParser commandLineParser = new BasicParser();
		CommandLine commandLine = null;
		// Parse command line
		try {
			commandLine = commandLineParser.parse(options, args);
		} catch (ParseException exception) {
			// Notify user then exit
			listener.onError(exception);
			System.exit(0);
		}
		/*
		 * Start tool.
		 */
		// Get checksum algorithm
		ChecksumAlgorithm algorithm = null;
		try {
			algorithm = ChecksumAlgorithm.valueOf(commandLine.getOptionValue("algorithm", "SHA256").toUpperCase());
		} catch (IllegalArgumentException exception) {
			// Notify user then exit
			System.err.println("Invalid algorimthm parameter.");
			System.exit(0);
		}
		// Get the ignore list
		final List<PathMatcher> ignoreList = new ArrayList<>();
		FileSystem defaultFileSystem = FileSystems.getDefault();
		if (commandLine.hasOption("ignore")) {
			// Get ignore paths from ignore parameter
			String[] ignorePatterns = commandLine.getOptionValue("ignore").split(";");
			// Parse each ignore pattern
			for (String ignorePattern : ignorePatterns) {
				ignoreList.add(defaultFileSystem.getPathMatcher("glob:"+ignorePattern));
			}
		} else if (commandLine.hasOption("ignoreFile")) {
			// Get ignore file path
			Path ignoreFilePath = Paths.get(commandLine.getOptionValue("ignoreFile"));
			try {
				// Parse each line of ignore file as pattern
				Files.lines(ignoreFilePath).forEach(pattern -> ignoreList.add(defaultFileSystem.getPathMatcher("glob:"+pattern)));
			} catch (IOException exception) {
				// Notify user then exit
				listener.onError(new Exception("Unable to read ignore file.", exception));
				System.exit(0);
			}
		}
		// Get the output file
		File outputFile = new File(commandLine.getOptionValue("output"));
		// Check mode
		if (commandLine.hasOption("list")) {
			// Declare checksum generator
			ChecksumGenerator checksumGenerator = null;
			// Check target parameter
			if (commandLine.hasOption("path")) {
				// Create checksum generator on file system
				try {
					Path path = Paths.get(commandLine.getOptionValue("path"));
					checksumGenerator = new FsChecksumGenerator(path);
				} catch (ChecksumException exception) {
					// Notify user then exit
					listener.onError(exception);
					System.exit(0);
				}
			} else if (commandLine.hasOption("url")) {
				// Create checksum generator on Subversion
				try {
					// Get checksum generator parameters
					String url = commandLine.getOptionValue("url");
					String user = commandLine.getOptionValue("user");
					// Get user password
					String passwd;
					if (commandLine.hasOption("password")) {
						passwd = commandLine.getOptionValue("password");
					} else {
						passwd = new String(ChecksumTool.readPasswd());
					}
					// Create checksum generator
					checksumGenerator = new SvnChecksumGenerator(url, user, passwd);
				} catch (ChecksumException exception) {
					// Notify user then exit
					listener.onError(exception);
					System.exit(0);
				}
			} else {
				// Notify user then exit
				System.err.println("Missing path or url parameters.");
				System.exit(0);
			}
			try {
				// Compute checksums
				AbstractDirectory directory = checksumGenerator.compute(algorithm, ignoreList, listener);
				// Output checksums
				ChecksumTool.outputResourceChecksum(directory, outputFile);
			} catch (ChecksumException exception) {
				// Notify listener on error
				listener.onError(exception);
			}
		} else if (commandLine.hasOption("diff")) {
			// Declare checksum generators
			ChecksumGenerator leftChecksumGenerator = null;
			ChecksumGenerator rightChecksumGenerator = null;
			// Check target parameter
			if (commandLine.hasOption("path")) {
				// Create checksum generator on file system
				try {
					// Get paths from command lines
					String[] paths = commandLine.getOptionValues("path");
					if (paths.length!=2) {
						// Notify user then exit
						System.err.println("Missing the two paths for diffing.");
						System.exit(0);
					}
					// Create paths for checksum generators
					Path leftPath = Paths.get(paths[0]);
					Path rightPath = Paths.get(paths[1]);
					// Create checksum generators
					leftChecksumGenerator = new FsChecksumGenerator(leftPath);
					rightChecksumGenerator = new FsChecksumGenerator(rightPath);
				} catch (ChecksumException exception) {
					// Notify user then exit
					listener.onError(exception);
					System.exit(0);
				}
			} else if (commandLine.hasOption("url")) {
				// Create checksum generator on Subversion
				try {
					// Get user parameter for checksum generators
					String user = commandLine.getOptionValue("user");
					// Get user password
					String passwd;
					if (commandLine.hasOption("passwd")) {
						passwd = commandLine.getOptionValue("passwd");
					} else {
						passwd = new String(ChecksumTool.readPasswd());
					}
					// Get URLs for checksum generators
					String urls[] = commandLine.getOptionValues("url");
					if (urls.length!=2) {
						// Notify user then exit
						System.err.println("Missing the two URLs for diffing.");
						System.exit(0);
					}
					// Create checksum generators
					leftChecksumGenerator = new SvnChecksumGenerator(urls[0], user, passwd);
					rightChecksumGenerator = new SvnChecksumGenerator(urls[1], user, passwd);
				} catch (ChecksumException exception) {
					// Notify user then exit
					listener.onError(exception);
					System.exit(0);
				}
			} else {
				// Notify user then exit
				System.err.println("Missing path or url parameters.");
				System.exit(0);
			}
			try {
				// Compute checksums
				AbstractDirectory leftDirectory = leftChecksumGenerator.compute(algorithm, ignoreList, listener);
				AbstractDirectory rightDirectory = rightChecksumGenerator.compute(algorithm, ignoreList, listener);
				// Sort directories
				leftDirectory.sort();
				rightDirectory.sort();
				// Output checksums
				ChecksumTool.outputDiffResourceChecksum(leftDirectory, rightDirectory, outputFile);
			} catch (ChecksumException exception) {
				// Notify listener on error
				listener.onError(exception);
			}
		}
	}

	/**
	 * Compare two resources.
	 * 
	 * @param resource1
	 *            The first resource to compare.
	 * @param resource2
	 *            The second resource to compare.
	 * @return A strictly negative number if first resource is before the second one, a strictly positive number if the second resource is before the first one,
	 *         <code>0</code> if resources are equals.
	 */
	public static int compareResource(AbstractResource resource1, AbstractResource resource2) {
		// Check null cases
		if (resource1==null&&resource2==null)
			return 0;
		else if (resource1==null&&resource2!=null)
			return 1;
		else if (resource1!=null&&resource2==null)
			return -1;
		// Check types
		if (resource1 instanceof AbstractDirectory&&resource2 instanceof AbstractDirectory) {
			return resource1.getName().compareTo(resource2.getName());
		} else if (resource1 instanceof AbstractDirectory&&!(resource2 instanceof AbstractDirectory)) {
			return -1;
		} else if (!(resource1 instanceof AbstractDirectory)&&resource2 instanceof AbstractDirectory) {
			return 1;
		} else {
			// Check name extensions
			String leftName = resource1.getName();
			String rightName = resource2.getName();
			// Compute dot indexes
			int leftNameIndex = leftName.lastIndexOf('.');
			int rightNameIndex = rightName.lastIndexOf('.');
			// Check dot presence
			if (leftNameIndex==-1&&rightNameIndex==-1) {
				// Compare without extension
				return resource1.getName().compareTo(resource2.getName());
			} else if (leftNameIndex>0&&rightNameIndex>0) {
				// Get sub names
				String leftSubName = leftName.substring(0, leftNameIndex);
				String rightSubName = rightName.substring(0, rightNameIndex);
				// Compare without extension
				int compare = leftSubName.compareTo(rightSubName);
				if (compare==0) {
					// Compare extension
					leftSubName = leftName.substring(leftNameIndex+1);
					rightSubName = rightName.substring(rightNameIndex+1);
					return leftSubName.compareTo(rightSubName);
				} else {
					return compare;
				}
			} else {
				// Compare mixing extension presence
				String leftSubName = leftNameIndex>0 ? leftName.substring(0, leftNameIndex) : leftName;
				String rightSubName = rightNameIndex>0 ? rightName.substring(0, rightNameIndex) : rightName;
				return leftSubName.compareTo(rightSubName);
			}
		}
	}

	/**
	 * Compute differences between two directories.
	 * 
	 * @param leftDirectory
	 *            The left resource to compute differences.
	 * @param rightDirectory
	 *            The right resource to compute differences.
	 * @return The directory difference.
	 */
	public static DirectoryDifference computeDifferences(AbstractDirectory leftDirectory, AbstractDirectory rightDirectory) {
		// Create resource iterator on directories
		Iterator<AbstractResource> leftResourceIterator = leftDirectory==null ? new EmptyIterator<AbstractResource>() : leftDirectory.getChildren().iterator();
		Iterator<AbstractResource> rightResourceIterator = rightDirectory==null ? new EmptyIterator<AbstractResource>() : rightDirectory.getChildren()
				.iterator();
		// Declare resources to compare
		AbstractResource leftResource = null;
		AbstractResource rightResource = null;
		// Declare resource difference for the compared directory
		DirectoryDifference directoryDifference = new DirectoryDifference(leftDirectory, rightDirectory);
		// Generate output while remains resources
		while (leftResourceIterator.hasNext()||rightResourceIterator.hasNext()) {
			/*
			 * Take new resources.
			 */
			// Check if left must be taken
			if (leftResource==null&&leftResourceIterator.hasNext())
				leftResource = leftResourceIterator.next();
			// Check if right must be taken
			if (rightResource==null&&rightResourceIterator.hasNext())
				rightResource = rightResourceIterator.next();
			/*
			 * Compare resources.
			 */
			// Compare resource
			int compare = ChecksumTool.compareResource(leftResource, rightResource);
			// Check resource equality
			if (compare<0) {
				if (leftResource instanceof AbstractDirectory) {
					// Compute and add left directory differences
					DirectoryDifference leftDirectoryDifference = ChecksumTool.computeDifferences((AbstractDirectory) leftResource, null);
					// Check if left directory has differences
					if (leftDirectoryDifference.hasDifference())
						directoryDifference.addDifference(leftDirectoryDifference);
				} else if (leftResource instanceof AbstractFile) {
					// Create and add left only difference
					AbstractDifference leftOnlyDifference = new FileDifference(FileDifferenceType.LEFT_ONLY, (AbstractFile) leftResource, null);
					directoryDifference.addDifference(leftOnlyDifference);
				}
				// Clear resource
				leftResource = null;
			} else if (compare>0) {
				if (rightResource instanceof AbstractDirectory) {
					// Compute and add right directory differences
					DirectoryDifference rightDirectoryDifference = ChecksumTool.computeDifferences(null, (AbstractDirectory) rightResource);
					// Check if right directory has differences
					if (rightDirectoryDifference.hasDifference())
						directoryDifference.addDifference(rightDirectoryDifference);
				} else if (rightResource instanceof AbstractFile) {
					// Create and add right only difference
					AbstractDifference rightOnlyDifference = new FileDifference(FileDifferenceType.RIGHT_ONLY, null, (AbstractFile) rightResource);
					directoryDifference.addDifference(rightOnlyDifference);
				}
				// Clear resource
				rightResource = null;
			} else if (compare==0) {
				if (leftResource instanceof AbstractDirectory&&rightDirectory instanceof AbstractDirectory) {
					// Recursively compute directory differences
					DirectoryDifference subDirectoryDifference = ChecksumTool.computeDifferences((AbstractDirectory) leftResource,
							(AbstractDirectory) rightResource);
					// Check if sub-directory has differences
					if (subDirectoryDifference.hasDifference())
						directoryDifference.addDifference(subDirectoryDifference);
				} else if (leftResource instanceof AbstractFile&&rightResource instanceof AbstractFile) {
					// Compare file checksums
					AbstractFile leftFile = (AbstractFile) leftResource;
					AbstractFile rightFile = (AbstractFile) rightResource;
					if (!Arrays.equals(leftFile.getChecksum(), rightFile.getChecksum())) {
						// Create and add different difference
						AbstractDifference differentDifference = new FileDifference(FileDifferenceType.DIFFERENT, (AbstractFile) leftResource,
								(AbstractFile) rightResource);
						directoryDifference.addDifference(differentDifference);
					}
				}
				// Clear resources
				leftResource = null;
				rightResource = null;
			}
		}
		// Return directory difference
		return directoryDifference;
	}

	/**
	 * Output resource checksum.
	 * 
	 * @param resource
	 *            The resource to output checksum.
	 * @param outputFile
	 *            The output file to store checksums.
	 * @throws ChecksumException
	 *             Throws exception if the checksums could not be output.
	 */
	public static void outputResourceChecksum(AbstractResource resource, File outputFile) throws ChecksumException {
		// Create an output writer
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath())) {
			// Output resource on the writer
			ChecksumTool.outputResourceChecksum(writer, resource);
		} catch (IOException exception) {
			throw new ChecksumException("Unable to write checksum file.", exception);
		}
	}

	/**
	 * Output different resource checksums.
	 * 
	 * @param leftDirectory
	 *            The left resource to output checksum.
	 * @param rightDirectory
	 *            The right resource to output checksum.
	 * @param outputFile
	 *            The output file to store checksums.
	 * @throws ChecksumException
	 *             Throws exception if the checksums could not be output.
	 */
	public static void outputDiffResourceChecksum(AbstractDirectory leftDirectory, AbstractDirectory rightDirectory, File outputFile) throws ChecksumException {
		// Create an output writer
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath())) {
			// Compute differences
			DirectoryDifference directoryDifference = ChecksumTool.computeDifferences(leftDirectory, rightDirectory);
			// Output differences on the writer
			ChecksumTool.outputDiffResourceChecksum(writer, directoryDifference);
		} catch (IOException exception) {
			throw new ChecksumException("Unable to write checksum file.", exception);
		}
	}

	/**
	 * Output resource checksum.
	 * 
	 * @param writer
	 *            The writer to output checksum.
	 * @param resource
	 *            The resource to output checksum.
	 * @throws IOException
	 *             Throws exception if the checksum could not be output.
	 */
	protected static void outputResourceChecksum(BufferedWriter writer, AbstractResource resource) throws IOException {
		// Check directory resource type
		if (resource instanceof AbstractDirectory) {
			// Output each child of directory
			for (AbstractResource child : ((AbstractDirectory) resource).getChildren())
				ChecksumTool.outputResourceChecksum(writer, child);
			// Return
			return;
		}
		// Check file resource type
		else if (resource instanceof AbstractFile) {
			// Get file resource
			AbstractFile file = (AbstractFile) resource;
			// Get file checksum bytes
			byte[] checksumBytes = file.getChecksum();
			if (checksumBytes==null)
				return;
			// Create hash string representation
			StringBuilder stringBuilder = new StringBuilder();
			for (byte b : file.getChecksum())
				stringBuilder.append(String.format("%02x", b));
			// Append file working copy path
			stringBuilder.append('\t');
			stringBuilder.append(file instanceof SvnResource ? ((SvnResource) file).getWorkingCopyPath() : file.getPath());
			// Append buffer to output
			writer.write(stringBuilder.toString());
			writer.newLine();
		}
	}

	/**
	 * Output different resource checksum.
	 * 
	 * @param writer
	 *            The writer to output checksum.
	 * @param directoryDifference
	 *            The directory differences to output.
	 * @throws IOException
	 *             Throws exception if the checksum could not be output.
	 */
	protected static void outputDiffResourceChecksum(BufferedWriter writer, DirectoryDifference directoryDifference) throws IOException {
		// Output each directory difference
		for (AbstractDifference difference : directoryDifference.getDifferences()) {
			// Check difference type
			if (difference instanceof DirectoryDifference) {
				// Recursively output sub-directory difference
				ChecksumTool.outputDiffResourceChecksum(writer, (DirectoryDifference) difference);
			} else if (difference instanceof FileDifference) {
				FileDifference fileDifference = (FileDifference) difference;
				// Get left and right related files
				AbstractFile leftFile = fileDifference.getLeftFile();
				AbstractFile rightFile = fileDifference.getRightFile();
				// Create output string builder
				StringBuilder stringBuilder = new StringBuilder();
				switch (fileDifference.getType()) {
				case LEFT_ONLY:
					// Output left file checksum
					for (byte b : leftFile.getChecksum())
						stringBuilder.append(String.format("%02x", b));
					stringBuilder.append('\t');
					stringBuilder.append(leftFile instanceof SvnResource ? ((SvnResource) leftFile).getWorkingCopyPath() : leftFile.getPath());
					stringBuilder.append('\t');
					stringBuilder.append('\t');
					break;
				case DIFFERENT:
					// Output file checksums
					for (byte b : leftFile.getChecksum())
						stringBuilder.append(String.format("%02x", b));
					stringBuilder.append('\t');
					stringBuilder.append(leftFile instanceof SvnResource ? ((SvnResource) leftFile).getWorkingCopyPath() : leftFile.getPath());
					stringBuilder.append('\t');
					for (byte b : rightFile.getChecksum())
						stringBuilder.append(String.format("%02x", b));
					stringBuilder.append('\t');
					stringBuilder.append(rightFile instanceof SvnResource ? ((SvnResource) rightFile).getWorkingCopyPath() : rightFile.getPath());
					break;
				case RIGHT_ONLY:
					// Output right file checksums
					stringBuilder.append('\t');
					stringBuilder.append('\t');
					for (byte b : rightFile.getChecksum())
						stringBuilder.append(String.format("%02x", b));
					stringBuilder.append('\t');
					stringBuilder.append(rightFile instanceof SvnResource ? ((SvnResource) rightFile).getWorkingCopyPath() : rightFile.getPath());
					break;
				}
				// Append the file difference
				writer.write(stringBuilder.toString());
				writer.newLine();
			}
		}
	}

	/**
	 * Read user password.
	 * 
	 * @return The read user password.
	 */
	private static String readPasswd() {
		// Notify user
		System.out.print("Input Subversion user password: ");
		// Check system console
		if (System.console()==null) {
			// Create reader from standard input
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
				return reader.readLine();
			} catch (IOException exception) {
				return "";
			}
		} else {
			// Read from console
			return new String(System.console().readPassword());
		}
	}

	/**
	 * This class represents an always empty iterator.
	 * 
	 * @author Bruce BUJON
	 *
	 * @param <E>
	 *            The type of elements of iterator.
	 */
	private static class EmptyIterator<E> implements Iterator<E> {
		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public E next() {
			return null;
		}
	}
}