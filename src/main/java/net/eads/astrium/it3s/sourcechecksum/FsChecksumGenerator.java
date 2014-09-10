package net.eads.astrium.it3s.sourcechecksum;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.eads.astrium.it3s.sourcechecksum.listener.ChecksumListener;
import net.eads.astrium.it3s.sourcechecksum.listener.ConsoleOutputListener;
import net.eads.astrium.it3s.sourcechecksum.resource.AbstractDirectory;
import net.eads.astrium.it3s.sourcechecksum.resource.AbstractFile;
import net.eads.astrium.it3s.sourcechecksum.resource.AbstractResource;
import net.eads.astrium.it3s.sourcechecksum.resource.fs.FsDirectory;
import net.eads.astrium.it3s.sourcechecksum.resource.fs.FsFile;

/**
 * This class is the main checksum generator program.
 */
public class FsChecksumGenerator {
	/** The digest algorithm name. */
	private static final String DIGEST_ALGORITHM = "SHA-256"; // SHA-256, MD5, CRC32
	/** The number of executors for checksum computation. */
	private static final int NBR_EXECUTORS = 30;
	/** The worker to notify progress. */
	private final ChecksumListener listener;
	/** The output file to store checksums. */
	private final File outputFile;
	/*
	 * Progress related.
	 */
	/** The break status (<code>true</code> if the process should break, <code>false</code> otherwise). */
	private boolean shouldBreak;
	/** The file counter of computed checksum. */
	private volatile int progressCounter;
	/** The file counter to compute checksum. */
	private volatile int fileCounter;

	/**
	 * Constructor.
	 * 
	 * @param path
	 *            The path to compute checksum (must be a directory).
	 * @param outputFile
	 *            The output file to store checksums.
	 * @param listener
	 *            The listener to notify progress.
	 */
	public FsChecksumGenerator(Path path, File outputFile, ChecksumListener listener) {
		// Store output file and listener
		this.outputFile = outputFile;
		this.listener = listener;
		try {
			long time = System.nanoTime();
			/*
			 * List files.
			 */
			// Initialize progress
			this.shouldBreak = false;
			this.fileCounter = 0;
			// Notify worker
			this.listener.onStart();
			// List directories and files
			FsFileVisitor fileVisitor = new FsFileVisitor();
			try {
				Files.walkFileTree(path, fileVisitor);
			} catch (IOException exception) {
				throw new ChecksumException("Unable to list file to compute checksums.", exception);
			}
			// Get root and file counter of the file system
			FsDirectory rootDirectory = fileVisitor.getRoot();
			this.fileCounter = fileVisitor.getFileCounter();
			System.out.println(this.fileCounter+" files");
			/*
			 * Compute checksums.
			 */
			// Initialize progress counter
			this.shouldBreak = false;
			this.progressCounter = 0;
			// Notify worker
			this.listener.onProgress(0);
			// Create executer service
			ExecutorService executorService = Executors.newFixedThreadPool(FsChecksumGenerator.NBR_EXECUTORS);
			// Process root directory
			this.processDirectory(executorService, rootDirectory);
			// Await terminaison
			try {
				executorService.shutdown();
				executorService.awaitTermination(1, TimeUnit.DAYS);
			} catch (InterruptedException exception) {
				throw new ChecksumException("Checksum computation did not end in time.", exception);
			}
			// Check if process has broke
			if (this.shouldBreak)
				return;
			// Output resource checksums
			this.outputResourceChecksums(rootDirectory);
			// Notify worker
			this.listener.onDone();
			// TODO
			time = (System.nanoTime()-time)/1000000000;
			if (time == 0)
				time = 1;
			System.out.println("Debug: "+this.fileCounter+" hashs in "+time+"secs ("+this.fileCounter/time+" hashs/secs)");
		} catch (ChecksumException exception) {
			// Notify worker
			this.listener.onError(exception);
		}
	}

	/**
	 * Process a directory.
	 * 
	 * @param executorService
	 *            The executor service to get executors.
	 * @param directory
	 *            The directory to proceed.
	 * @throws ChecksumException
	 *             Throws exception if a checksum could not be computed.
	 */
	public void processDirectory(ExecutorService executorService, FsDirectory directory) throws ChecksumException {
		// Process each child resource
		for (AbstractResource resource : directory.getChildren()) {
			// Check if should break
			if (this.shouldBreak)
				break;
			// Check resource type
			if (resource instanceof FsDirectory)
				// Recursively process directory
				this.processDirectory(executorService, (FsDirectory) resource);
			else if (resource instanceof FsFile)
				// Prepare file
				this.prepareFile(executorService, (FsFile) resource);
		}
	}

	/**
	 * Process a file.
	 * 
	 * @param executorService
	 *            The executor service to get executors.
	 * @param file
	 *            The file to proceed.
	 * @throws ChecksumException
	 *             Throws exception if the checksum could not be computed.
	 */
	public void prepareFile(ExecutorService executorService, final FsFile file) throws ChecksumException {
		// Submit a new task to process file
		executorService.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				// Check if should break
				if (FsChecksumGenerator.this.shouldBreak)
					return null;
				try {
					// Process file
					FsChecksumGenerator.this.processFile(file);
					// Update progress counter
					FsChecksumGenerator.this.progressCounter++;
					// Notify listener
					FsChecksumGenerator.this.listener.onProgress(FsChecksumGenerator.this.progressCounter*100/FsChecksumGenerator.this.fileCounter);
				} catch (ChecksumException exception) {
					// Break the process
					FsChecksumGenerator.this.shouldBreak = true;
					// Notify listener
					FsChecksumGenerator.this.listener.onError(exception);
				}
				// Return void
				return null;
			}
		});
	}

	/**
	 * Process a file.
	 * 
	 * @param file
	 *            The file to proceed.
	 * @throws ChecksumException
	 *             Throws exception if the checksum could not be computed.
	 */
	public void processFile(FsFile file) throws ChecksumException {
		/*
		 * Create digest output stream.
		 */
		// Create message digest
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance(FsChecksumGenerator.DIGEST_ALGORITHM);
		} catch (NoSuchAlgorithmException exception) {
			throw new ChecksumException("Unable to compute \""+FsChecksumGenerator.DIGEST_ALGORITHM+"\" checksum.", exception);
		}
		/*
		 * Get output stream for file content
		 */
		// Create output stream with digest decorator
		try (OutputStream outputStream = new ByteArrayOutputStream(); DigestOutputStream digestOutputStream = new DigestOutputStream(outputStream, digest)) {
			// Get file content
			try {
				// Copy file content to output stream
				Files.copy(file.getFile(), digestOutputStream);
			} catch (IOException exception) {
				throw new ChecksumException("Unable to get file content for \""+file.getPath()+"\".", exception);
			}
			// Compute digest
			byte[] digestBytes = digestOutputStream.getMessageDigest().digest();
			// Store digest to file
			file.setChecksum(digestBytes);
		} catch (IOException exception) {
			throw new ChecksumException("Unable to get file content for \""+file.getPath()+"\".", exception);
		}
	}

	/**
	 * Output resource checksums.
	 * 
	 * @param resource
	 *            The resource to output checksum.
	 * @throws ChecksumException
	 *             Throws exception if the checksums could not be output.
	 */
	public void outputResourceChecksums(AbstractResource resource) throws ChecksumException {
		// Create an output writer
		try (BufferedWriter writer = Files.newBufferedWriter(this.outputFile.toPath())) {
			// Output resource on the writer
			this.outputResource(writer, resource);
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
	protected void outputResource(BufferedWriter writer, AbstractResource resource) throws IOException {
		// Check directory resource type
		if (resource instanceof AbstractDirectory) {
			// Output each child of directory
			for (AbstractResource child : ((AbstractDirectory) resource).getChildren())
				this.outputResource(writer, child);
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

	/**
	 * The main procedure.
	 * 
	 * @param args
	 *            The CLI parameters.
	 */
	public static void main(String[] args) {
		if (args.length<2) {
			MainWindow mainWindow = new MainWindow();
			mainWindow.setVisible(true);
		} else {
			Path path = Paths.get(args[0]);
			File file = new File(args[1]);
			new FsChecksumGenerator(path, file, new ConsoleOutputListener());
		}
	}

	/**
	 * This class is a file visitor to create file system.
	 * 
	 * @author Bruce BUJON
	 *
	 */
	private static class FsFileVisitor extends SimpleFileVisitor<Path> {
		/** The root directory. */
		private FsDirectory root;
		/** The file counter. */
		private int fileCounter;
		/** The currently visited directory. */
		private FsDirectory currentDirectory;

		/**
		 * Get the root directory of the file system.
		 * 
		 * @return The root directory of the file system.
		 */
		public FsDirectory getRoot() {
			return this.root;
		}

		/**
		 * Get the file counter.
		 * 
		 * @return The file counter.
		 */
		public int getFileCounter() {
			return this.fileCounter;
		}

		/*
		 * File Visitor.
		 */

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			// Check if root exists
			if (this.root==null) {
				// Create root directory
				this.root = new FsDirectory(dir);
				// Save current directory
				this.currentDirectory = this.root;
			} else {
				// Create directory
				FsDirectory currentDirectory = new FsDirectory(dir);
				// Append current directory
				this.currentDirectory.addChild(currentDirectory);
				// Save current directory
				this.currentDirectory = currentDirectory;
			}
			// Continue visiting
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			// Create file
			AbstractFile currentFile = new FsFile(file);
			// Append current file
			this.currentDirectory.addChild(currentFile);
			// Increment file counter
			this.fileCounter++;
			// Continue visiting
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			// Update current directory
			this.currentDirectory = (FsDirectory) this.currentDirectory.getParent();
			// Continue visiting
			return FileVisitResult.CONTINUE;
		}
	}
}