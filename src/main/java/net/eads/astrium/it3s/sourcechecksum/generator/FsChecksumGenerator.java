package net.eads.astrium.it3s.sourcechecksum.generator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.eads.astrium.it3s.sourcechecksum.ChecksumException;
import net.eads.astrium.it3s.sourcechecksum.algorithm.ChecksumAlgorithm;
import net.eads.astrium.it3s.sourcechecksum.listener.ChecksumListener;
import net.eads.astrium.it3s.sourcechecksum.resource.AbstractDirectory;
import net.eads.astrium.it3s.sourcechecksum.resource.AbstractResource;
import net.eads.astrium.it3s.sourcechecksum.resource.fs.FsDirectory;
import net.eads.astrium.it3s.sourcechecksum.resource.fs.FsFile;

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
	private final File path;
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
	public FsChecksumGenerator(File path) throws ChecksumException {
		// Check if path is a directory
		if (!path.isDirectory())
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
		ListResult listResult = null;
		try {
			listResult = FsChecksumGenerator.listFile(this.path);
		} catch (IOException exception) {
			throw new ChecksumException("Unable to list file to compute checksums.", exception);
		}
		// Get root and file counter of the file system
		FsDirectory rootDirectory = listResult.directory;
		this.fileCounter = listResult.fileCounter;
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
		// Declare output stream
		DigestOutputStream digestOutputStream = null;
		try {
			// Create output stream with digest decorator
			OutputStream outputStream = new ByteArrayOutputStream();
			digestOutputStream = new DigestOutputStream(outputStream, digest);
			// Get file content
			try {
				// Copy file content to output stream
				FsChecksumGenerator.copyFile(file.getFile(), digestOutputStream);
			} catch (IOException exception) {
				throw new ChecksumException("Unable to get file content for \""+file.getPath()+"\".", exception);
			}
			// Compute digest
			byte[] digestBytes = digestOutputStream.getMessageDigest().digest();
			// Store digest to file
			file.setChecksum(digestBytes);
		} finally {
			// Check stream initialization
			if (digestOutputStream!=null) {
				try {
					// Close the stream
					digestOutputStream.close();
				} catch (IOException exception) {
				}
			}
		}
	}

	/**
	 * Copy a file content to an {@link OutputStream}.
	 * 
	 * @param sourceFile
	 *            The source file to copy content.
	 * @param destinationStream
	 *            The destination stream to put file content.
	 * @throws IOException
	 *             Throws exception if the file content could not be copied in the stream.
	 */
	private static void copyFile(File sourceFile, OutputStream destinationStream) throws IOException {
		// Declare input stream
		InputStream stream = null;
		// Create stream buffer
		byte[] buffer = new byte[1024];
		try {
			// Create file input stream
			stream = new FileInputStream(sourceFile);
			int read;
			// Read input stream content
			while ((read = stream.read(buffer))!=-1) {
				// Write stream content to output stream
				destinationStream.write(buffer, 0, read);
			}
		} finally {
			// Check reader initialization
			if (stream!=null) {
				try {
					// Close the reader
					stream.close();
				} catch (IOException exception) {
				}
			}
		}
	}

	/**
	 * List content of a directory.
	 * 
	 * @param directory
	 *            The directory to list.
	 * @return The listing result.
	 * @throws IOException
	 *             Throws exception if the directory could not be listed.
	 */
	private static ListResult listFile(File directory) throws IOException {
		// Check directory
		if (!directory.isDirectory())
			throw new IOException("The path to list is not a directory.");
		// Create a list result
		ListResult result = new ListResult();
		// Create list result directory
		result.directory = new FsDirectory(directory);
		// Process each child file
		for (File childFile : directory.listFiles()) {
			// Check file type
			if (childFile.isDirectory()) {
				// Recursively list child directory
				ListResult childResult = FsChecksumGenerator.listFile(childFile);
				// Add child directory
				result.directory.addChild(childResult.directory);
				// Add child counter
				result.fileCounter += childResult.fileCounter;
			} else {
				// Create and add child file
				FsFile file = new FsFile(childFile);
				result.directory.addChild(file);
				// Increment file counter
				result.fileCounter++;
			}
		}
		// Return list result
		return result;
	}

	/**
	 * This class is a helper class for {@link FsChecksumGenerator#listFile(File)} method.
	 * 
	 * @author Bruce BUJON
	 *
	 */
	private static class ListResult {
		/** The listed directory. */
		private FsDirectory directory;
		/** The file counter. */
		private int fileCounter;
	}
}