package net.eads.astrium.it3s.sourcechecksum;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.util.Arrays;
import java.util.Iterator;

import net.eads.astrium.it3s.sourcechecksum.algorithm.ChecksumAlgorithm;
import net.eads.astrium.it3s.sourcechecksum.algorithm.CustomSecurityProvider;
import net.eads.astrium.it3s.sourcechecksum.generator.ChecksumGenerator;
import net.eads.astrium.it3s.sourcechecksum.generator.FsChecksumGenerator;
import net.eads.astrium.it3s.sourcechecksum.generator.SvnChecksumGenerator;
import net.eads.astrium.it3s.sourcechecksum.listener.ChecksumListener;
import net.eads.astrium.it3s.sourcechecksum.listener.ConsoleOutputListener;
import net.eads.astrium.it3s.sourcechecksum.resource.AbstractDirectory;
import net.eads.astrium.it3s.sourcechecksum.resource.AbstractFile;
import net.eads.astrium.it3s.sourcechecksum.resource.AbstractResource;
import net.eads.astrium.it3s.sourcechecksum.resource.svn.SvnResource;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

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
		Option listOption = OptionBuilder.withLongOpt("list").withDescription("Compute checksums").create();
		// Create diff option
		Option diffOption = OptionBuilder.withLongOpt("diff").withDescription("Compute version differences").create();
		// Create mode option group
		OptionGroup modeGroup = new OptionGroup();
		modeGroup.setRequired(true);
		modeGroup.addOption(diffOption);
		modeGroup.addOption(listOption);
		options.addOptionGroup(modeGroup);
		// Create path option
		Option pathOption = OptionBuilder.withLongOpt("path").withDescription("The paths to compute checksums or differences.").hasArgs(2).create();
		options.addOption(pathOption);
		// Create repository option
		Option repositoryOption = OptionBuilder.withLongOpt("repository").withDescription("The repository to compute checksums or differences.").hasArg(true)
				.create();
		options.addOption(repositoryOption);
		// Create URL option
		Option urlOption = OptionBuilder.withLongOpt("url").withDescription("The URLs of the repository to compute checksums or differences.").hasArgs(2)
				.create();
		options.addOption(urlOption);
		// Create user option
		Option userOption = OptionBuilder.withLongOpt("user").withDescription("The Subversion user name.").hasArg(true).create();
		options.addOption(userOption);
		// Create password option
		Option passwdOption = OptionBuilder.withLongOpt("passwd").withDescription("The Subversion user password.").hasArg(true).create();
		options.addOption(passwdOption);
		// Create algorithm option
		Option algorithOption = OptionBuilder.withLongOpt("algorithm").withDescription("The checksum algorithm to use (CRC32, MD5 or SHA256 (default))")
				.hasArg(true).create();
		options.addOption(algorithOption);
		// Create output option
		Option outputOption = OptionBuilder.withLongOpt("output").withDescription("The result output file.").hasArg(true).isRequired(true).create();
		options.addOption(outputOption);
		// Check CLI parameters
		if (args.length==0) {
			// Print help
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.printHelp("checksums", options);
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
		// Add custom security provider
		Security.addProvider(new CustomSecurityProvider());
		// Get checksum algorithm
		ChecksumAlgorithm algorithm = null;
		try {
			algorithm = ChecksumAlgorithm.valueOf(commandLine.getOptionValue("algorithm", "SHA256").toUpperCase());
		} catch (IllegalArgumentException exception) {
			// Notify user then exit
			System.err.println("Invalid algorimthm parameter.");
			System.exit(0);
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
					checksumGenerator = ChecksumTool.buildFsChecksumGenerator(path);
				} catch (ChecksumException exception) {
					// Notify user then exit
					listener.onError(exception);
					System.exit(0);
				}
			} else if (commandLine.hasOption("repository")&&commandLine.hasOption("url")) {
				// Create checksum generator on Subversion
				try {
					// Get checksum generator parameters
					String repository = commandLine.getOptionValue("repository");
					String url = commandLine.getOptionValue("url");
					String user = commandLine.getOptionValue("user");
					// Get user password
					String passwd;
					if (commandLine.hasOption("passwd")) {
						passwd = commandLine.getOptionValue("passwd");
					} else {
						passwd = new String(ChecksumTool.readPasswd());
					}
					// Create checksum generator
					checksumGenerator = ChecksumTool.buildSvnChecksumGenerator(repository, url, user, passwd);
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
			try {
				// Compute checksums
				AbstractDirectory directory = checksumGenerator.compute(algorithm, listener);
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
					leftChecksumGenerator = ChecksumTool.buildFsChecksumGenerator(leftPath);
					rightChecksumGenerator = ChecksumTool.buildFsChecksumGenerator(rightPath);
				} catch (ChecksumException exception) {
					// Notify user then exit
					listener.onError(exception);
					System.exit(0);
				}
			} else if (commandLine.hasOption("repository")&&commandLine.hasOption("url")) {
				// Create checksum generator on Subversion
				try {
					// Get repository parameters for checksum generators
					String repository = commandLine.getOptionValue("repository");
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
					leftChecksumGenerator = ChecksumTool.buildSvnChecksumGenerator(repository, urls[0], user, passwd);
					rightChecksumGenerator = ChecksumTool.buildSvnChecksumGenerator(repository, urls[1], user, passwd);
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
			try {
				// Compute checksums
				AbstractDirectory leftDirectory = leftChecksumGenerator.compute(algorithm, listener);
				AbstractDirectory rightDirectory = rightChecksumGenerator.compute(algorithm, listener);
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
				String leftSubName = leftName.substring(0, leftNameIndex-1);
				String rightSubName = rightName.substring(0, rightNameIndex-1);
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
				String leftSubName = leftNameIndex>0 ? leftName.substring(0, leftNameIndex-1) : leftName;
				String rightSubName = rightNameIndex>0 ? rightName.substring(0, rightNameIndex-1) : rightName;
				return leftSubName.compareTo(rightSubName);
			}
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
	 * @param repository
	 *            The Subversion repository to compute checksums.
	 * @param url
	 *            The URL to compute checksums.
	 * @param user
	 *            The Subversion user name.
	 * @param passwd
	 *            The Subversion user password.
	 * @return The built Subversion checksum generator.
	 * @throws ChecksumException
	 *             Throws exception if the generator could not be built.
	 */
	protected static ChecksumGenerator buildSvnChecksumGenerator(String repository, String url, String user, String passwd) throws ChecksumException {
		// Check repository leading slash
		if (repository.charAt(repository.length()-1)=='/')
			// Remove leading slash
			repository = repository.substring(0, repository.length()-1);
		// Check URL leading slash
		if (url.charAt(url.length()-1)=='/')
			url = url.substring(0, url.length()-1);
		// Create new checksum generator
		return new SvnChecksumGenerator(repository, url, user, passwd);
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
	protected static void outputResourceChecksum(AbstractResource resource, File outputFile) throws ChecksumException {
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
	protected static void outputDiffResourceChecksum(AbstractDirectory leftDirectory, AbstractDirectory rightDirectory, File outputFile)
			throws ChecksumException {
		// Create an output writer
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath())) {
			// Output resource on the writer
			ChecksumTool.outputDiffResourceChecksum(writer, leftDirectory, rightDirectory);
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
	 * @param leftDirectory
	 *            The left resource to output checksum.
	 * @param rightDirectory
	 *            The right resource to output checksum.
	 * @throws IOException
	 *             Throws exception if the checksum could not be output.
	 */
	protected static void outputDiffResourceChecksum(BufferedWriter writer, AbstractDirectory leftDirectory, AbstractDirectory rightDirectory)
			throws IOException {

		Iterator<AbstractResource> leftResourceIterator = leftDirectory==null ? new EmptyIterator<AbstractResource>() : leftDirectory.getChildren().iterator();
		Iterator<AbstractResource> rightResourceIterator = rightDirectory==null ? new EmptyIterator<AbstractResource>() : rightDirectory.getChildren()
				.iterator();

		AbstractResource leftResource = null;
		AbstractResource rightResource = null;

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
					// Output left directory
					ChecksumTool.outputDiffResourceChecksum(writer, (AbstractDirectory) leftResource, null);
				} else if (leftResource instanceof AbstractFile) {
					// Output left file
					AbstractFile leftFile = (AbstractFile) leftResource;
					StringBuilder stringBuilder = new StringBuilder();
					for (byte b : leftFile.getChecksum())
						stringBuilder.append(String.format("%02x", b));
					stringBuilder.append('\t');
					stringBuilder.append(leftFile instanceof SvnResource ? ((SvnResource) leftFile).getWorkingCopyPath() : leftFile.getPath());
					stringBuilder.append('\t');
					stringBuilder.append('\t');
					writer.write(stringBuilder.toString());
					writer.newLine();
				}
				// Clear resource
				leftResource = null;
			} else if (compare>0) {
				if (rightResource instanceof AbstractDirectory) {
					// Output left directory
					ChecksumTool.outputDiffResourceChecksum(writer, null, (AbstractDirectory) rightResource);
				} else if (rightResource instanceof AbstractFile) {
					// Output right file
					AbstractFile rightFile = (AbstractFile) rightResource;
					StringBuilder stringBuilder = new StringBuilder();
					stringBuilder.append('\t');
					stringBuilder.append('\t');
					for (byte b : rightFile.getChecksum())
						stringBuilder.append(String.format("%02x", b));
					stringBuilder.append('\t');
					stringBuilder.append(rightFile instanceof SvnResource ? ((SvnResource) rightFile).getWorkingCopyPath() : rightFile.getPath());
					writer.write(stringBuilder.toString());
					writer.newLine();
				}
				// Clear resource
				rightResource = null;
			} else if (compare==0) {
				if (leftResource instanceof AbstractDirectory&&rightDirectory instanceof AbstractDirectory) {
					// Recursively output directory content
					ChecksumTool.outputDiffResourceChecksum(writer, (AbstractDirectory) leftResource, (AbstractDirectory) rightResource);
				} else if (leftResource instanceof AbstractFile&&rightResource instanceof AbstractFile) {
					// Compare file checksums
					AbstractFile leftFile = (AbstractFile) leftResource;
					AbstractFile rightFile = (AbstractFile) rightResource;
					if (!Arrays.equals(leftFile.getChecksum(), rightFile.getChecksum())) {
						// Output file checksums
						StringBuilder stringBuilder = new StringBuilder();
						for (byte b : leftFile.getChecksum())
							stringBuilder.append(String.format("%02x", b));
						stringBuilder.append('\t');
						stringBuilder.append(leftFile instanceof SvnResource ? ((SvnResource) leftFile).getWorkingCopyPath() : leftFile.getPath());
						stringBuilder.append('\t');
						for (byte b : rightFile.getChecksum())
							stringBuilder.append(String.format("%02x", b));
						stringBuilder.append('\t');
						stringBuilder.append(rightFile instanceof SvnResource ? ((SvnResource) rightFile).getWorkingCopyPath() : rightFile.getPath());
						writer.write(stringBuilder.toString());
						writer.newLine();
					}
				}
				// Check resources
				leftResource = null;
				rightResource = null;
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