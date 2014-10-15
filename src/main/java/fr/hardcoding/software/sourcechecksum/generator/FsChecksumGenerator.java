package fr.hardcoding.software.sourcechecksum.generator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import fr.hardcoding.software.sourcechecksum.ChecksumException;
import fr.hardcoding.software.sourcechecksum.algorithm.ChecksumAlgorithm;
import fr.hardcoding.software.sourcechecksum.listener.ChecksumListener;
import fr.hardcoding.software.sourcechecksum.resource.AbstractDirectory;
import fr.hardcoding.software.sourcechecksum.resource.AbstractFile;
import fr.hardcoding.software.sourcechecksum.resource.AbstractResource;
import fr.hardcoding.software.sourcechecksum.resource.fs.FsDirectory;
import fr.hardcoding.software.sourcechecksum.resource.fs.FsFile;

/**
 * This class is the main checksum generator program.
 */
public class FsChecksumGenerator implements ChecksumGenerator {
	/** The number of executors for checksum computation. */
	private static final int NBR_EXECUTORS = 30;
	/*
	 * Checksum computation related.
	 */
	/** The path to compute checksum. */
	private final Path path;
	/** The algorithm to use to compute checksum. */
	private ChecksumAlgorithm algorithm;
	/*
	 * Progress related.
	 */
	/** The break status (<code>true</code> if the process should break, <code>false</code> otherwise). */
	private boolean shouldBreak;
	/** The file counter of computed checksum. */
	private AtomicInteger progressCounter;
	/** The file counter to compute checksum. */
	private int fileCounter;

	/**
	 * Constructor.
	 * 
	 * @param path
	 *            The path to compute checksum (must be a directory).
	 * @throws ChecksumException
	 *             Throws exception if the generator could not be created.
	 */
	public FsChecksumGenerator(Path path) throws ChecksumException {
		// Check if path is a directory
		if (!Files.isDirectory(path))
			throw new ChecksumException("The root path is not a directory.");
		// Save path to compute checksum
		this.path = path;
	}

	/*
	 * Checksum Generator.
	 */

	@Override
	public AbstractDirectory compute(ChecksumAlgorithm algorithm, ChecksumListener listener) throws ChecksumException {
		// Save algorithm to use
		this.algorithm = algorithm;
		// Save start time
		long startTime = System.nanoTime();
		/*
		 * List files.
		 */
		// Initialize progress
		this.shouldBreak = false;
		this.fileCounter = 0;
		// Notify worker
		listener.onStart();
		// List directories and files
		FsFileVisitor fileVisitor = new FsFileVisitor();
		try {
			Files.walkFileTree(this.path, fileVisitor);
		} catch (IOException exception) {
			throw new ChecksumException("Unable to list file to compute checksums.", exception);
		}
		// Get root and file counter of the file system
		FsDirectory rootDirectory = fileVisitor.getRoot();
		this.fileCounter = fileVisitor.getFileCounter();
		listener.onDebug(this.fileCounter+" files found.");
		/*
		 * Compute checksums.
		 */
		// Initialize progress counter
		this.shouldBreak = false;
		this.progressCounter = new AtomicInteger();
		// Notify worker
		listener.onProgress(0);
		// Create executer service
		ExecutorService executorService = Executors.newFixedThreadPool(FsChecksumGenerator.NBR_EXECUTORS);
		// Process root directory
		this.processDirectory(executorService, rootDirectory, listener);
		// Await terminaison
		try {
			executorService.shutdown();
			executorService.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException exception) {
			throw new ChecksumException("Checksum computation did not end in time.", exception);
		}
		// Check if process has broke
		if (this.shouldBreak)
			throw new ChecksumException("An error occured while checksum computation.");
		// Notify worker
		listener.onDone();
		// Compute elapsed time
		long elapsedTime = (System.nanoTime()-startTime)/1000000000;
		if (elapsedTime==0)
			elapsedTime = 1;
		listener.onDebug(this.fileCounter+" hashs in "+elapsedTime+" secs ("+this.fileCounter/elapsedTime+" hashs/secs)");
		// Return root directory
		return rootDirectory;
	}

	/**
	 * Process a directory.
	 * 
	 * @param executorService
	 *            The executor service to get executors.
	 * @param directory
	 *            The directory to proceed.
	 * @param listener
	 *            The listener to notify computation progress.
	 * @throws ChecksumException
	 *             Throws exception if a checksum could not be computed.
	 */
	public void processDirectory(ExecutorService executorService, FsDirectory directory, ChecksumListener listener) throws ChecksumException {
		// Process each child resource
		for (AbstractResource resource : directory.getChildren()) {
			// Check if should break
			if (this.shouldBreak)
				break;
			// Check resource type
			if (resource instanceof FsDirectory)
				// Recursively process directory
				this.processDirectory(executorService, (FsDirectory) resource, listener);
			else if (resource instanceof FsFile)
				// Prepare file
				this.prepareFile(executorService, (FsFile) resource, listener);
		}
	}

	/**
	 * Process a file.
	 * 
	 * @param executorService
	 *            The executor service to get executors.
	 * @param file
	 *            The file to proceed.
	 * @param listener
	 *            The listener to notify computation progress.
	 * @throws ChecksumException
	 *             Throws exception if the checksum could not be computed.
	 */
	public void prepareFile(ExecutorService executorService, final FsFile file, final ChecksumListener listener) throws ChecksumException {
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
					FsChecksumGenerator.this.progressCounter.incrementAndGet();
					// Notify listener
					listener.onProgress(FsChecksumGenerator.this.progressCounter.intValue()*100/FsChecksumGenerator.this.fileCounter);
				} catch (ChecksumException exception) {
					// Break the process
					FsChecksumGenerator.this.shouldBreak = true;
					// Notify listener
					listener.onError(exception);
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
			digest = MessageDigest.getInstance(this.algorithm.getName());
		} catch (NoSuchAlgorithmException exception) {
			throw new ChecksumException("Unable to compute \""+this.algorithm+"\" checksum.", exception);
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