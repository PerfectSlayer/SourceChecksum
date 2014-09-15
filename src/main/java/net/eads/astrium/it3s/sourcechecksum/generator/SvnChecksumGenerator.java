package net.eads.astrium.it3s.sourcechecksum.generator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.eads.astrium.it3s.sourcechecksum.ChecksumException;
import net.eads.astrium.it3s.sourcechecksum.listener.ChecksumListener;
import net.eads.astrium.it3s.sourcechecksum.resource.AbstractDirectory;
import net.eads.astrium.it3s.sourcechecksum.resource.AbstractResource;
import net.eads.astrium.it3s.sourcechecksum.resource.svn.SvnDirectory;
import net.eads.astrium.it3s.sourcechecksum.resource.svn.SvnFile;
import net.eads.astrium.it3s.sourcechecksum.resource.svn.SvnResource;
import net.eads.astrium.it3s.sourcechecksum.thread.SvnClientThread;
import net.eads.astrium.it3s.sourcechecksum.thread.SvnClientThreadFactory;

import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.wc.SVNExternal;
import org.tmatesoft.svn.core.internal.wc.admin.SVNTranslator;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/**
 * This class is the main checksum generator program.
 * 
 */
public class SvnChecksumGenerator implements ChecksumGenerator {
	/** The digest algorithm name. */
	private static final String DIGEST_ALGORITHM = "SHA-256"; // SHA-256, MD5, CRC32
	/** The default Subversion options. */
	private static final ISVNOptions SVN_OPTIONS = SVNWCUtil.createDefaultOptions(true);
	/** The number of executors for checksum computation. */
	private static final int NBR_EXECUTORS = 30;
	/*
	 * Progress related.
	 */
	/** The pending directories to be listed. */
	private final Set<SvnDirectory> pendingDirectories = Collections.synchronizedSet(new HashSet<SvnDirectory>());
	/** The break status (<code>true</code> if the process should break, <code>false</code> otherwise). */
	private boolean shouldBreak;
	/** The file counter of computed checksum. */
	private volatile int progressCounter;
	/** The file counter to compute checksum. */
	private volatile int fileCounter;

	/** The Subversion repository to compute checksums. */
	private SVNRepository repository;
	/** The root directory to compute checksums. */
	private SvnDirectory rootDirectory;
	/** The Subversion client thread factory. */
	private SvnClientThreadFactory svnClientThreadFactory;

	/**
	 * Create Subversion repository.
	 * 
	 * @param url
	 *            The URL of the root of the repository.
	 * @param name
	 *            The Subversion user name.
	 * @param passwd
	 *            The Subversion user password.
	 * @return The created Subversion repository.
	 * @throws ChecksumException
	 *             Throws exception if the repository could not be created.
	 */
	public static SVNRepository createRepository(String url, String name, String passwd) throws ChecksumException {
		// Declare repository
		SVNRepository repository;
		try {
			// Initialize factory for DAV access
			SVNURL svnUrl = SVNURL.parseURIEncoded(url);
			// Create repository
			DAVRepositoryFactory.setup();
			repository = SVNRepositoryFactory.create(svnUrl, null);
			// Create authentication manager
			ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(name, passwd);
			repository.setAuthenticationManager(authManager);
			// Ensure repository is valid
			SVNNodeKind nodeKind = repository.checkPath("", -1);
			if (nodeKind==SVNNodeKind.NONE)
				throw new ChecksumException("No repository was found at \""+url+"\".");
			if (nodeKind==SVNNodeKind.FILE)
				throw new ChecksumException("The URL \""+url+"\" should be a directory.");
		} catch (SVNException exception) {
			throw new ChecksumException("Unable to create repository.", exception);
		}
		// Return created repository
		return repository;
	}

	/**
	 * Constructor.
	 * 
	 * @param serverRoot
	 *            The Subversion server root.
	 * @param path
	 *            The release path to compute checksum (must be a directory).
	 * @param user
	 *            The Subversion user name.
	 * @param passwd
	 *            The Subversion user password.
	 * @throws ChecksumException
	 *             Throws exception if the generator could not be created.
	 */
	public SvnChecksumGenerator(final String serverRoot, String path, final String user, final String passwd) throws ChecksumException {
		// Create repository
		this.repository = SvnChecksumGenerator.createRepository(serverRoot, user, passwd);
		// Create root directory
		this.rootDirectory = new SvnDirectory(path);
		// Create SVN client thread factory
		this.svnClientThreadFactory = new SvnClientThreadFactory(serverRoot, user, passwd);
	}

	/*
	 * Checksum generator.
	 */

	@Override
	public AbstractDirectory compute(ChecksumListener listener) throws ChecksumException {
		// TODO timers
		long time = System.nanoTime();
		/*
		 * List files.
		 */
		// Initialize progress
		this.shouldBreak = false;
		this.fileCounter = 0;
		// Notify worker
		listener.onStart();
		// Create executer service
		ExecutorService executorService = Executors.newFixedThreadPool(SvnChecksumGenerator.NBR_EXECUTORS, this.svnClientThreadFactory);
		// List root directory
		this.prepareListDirectory(executorService, this.rootDirectory, listener);
		try {
			// Wait until no pending directory left
			while (!this.pendingDirectories.isEmpty()&&!this.shouldBreak) {
				Thread.sleep(100);
			}
			// Await terminaison
			executorService.shutdown();
			executorService.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException exception) {
			throw new ChecksumException("Checksum computation did not end in time.", exception);
		}
		System.out.println(this.fileCounter+" files");
		// Check if process has broke
		if (this.shouldBreak)
			throw new ChecksumException("An error occured while listing resources.");
		/*
		 * Compute checksums.
		 */
		// Initialize progress counter
		this.shouldBreak = false;
		this.progressCounter = 0;
		// Notify worker
		listener.onProgress(0);
		// Create executer service
		executorService = Executors.newFixedThreadPool(SvnChecksumGenerator.NBR_EXECUTORS, this.svnClientThreadFactory);
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
		// TODO timers
		time = (System.nanoTime()-time)/1000000000;
		System.out.println("Debug: "+this.fileCounter+" hashs in "+time+"secs ("+this.fileCounter/time+" hashs/secs)");
		// Return the root directory
		return this.rootDirectory;
	}

	/**
	 * Prepare directory listing directory content.
	 * 
	 * @param executorService
	 *            The executor service to get executors.
	 * @param directory
	 *            The directory resource to list.
	 * @param listener
	 *            The listener to notify computation progress.
	 */
	public void prepareListDirectory(final ExecutorService executorService, final SvnDirectory directory, final ChecksumListener listener) {
		// Add directory pending directories
		this.pendingDirectories.add(directory);
		// Submit a task to list directory
		executorService.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				// Check if should break
				if (SvnChecksumGenerator.this.shouldBreak)
					return null;
				try {
					// List directory content
					SvnChecksumGenerator.this.listDirectory(executorService, directory, listener);
				} catch (ChecksumException exception) {
					// Break the process
					SvnChecksumGenerator.this.shouldBreak = true;
					// Notify listener
					listener.onError(exception);
				}
				// Return void
				return null;
			}
		});
	}

	/**
	 * List directory content.
	 * 
	 * @param executorService
	 *            The executor service to get executors.
	 * @param directory
	 *            The directory resource to list.
	 * @param listener
	 *            The listener to notify computation progress.
	 * @throws ChecksumException
	 *             Throw exception if the directory could not be listed.
	 */
	public void listDirectory(ExecutorService executorService, SvnDirectory directory, ChecksumListener listener)
			throws ChecksumException {
		/*
		 * Handle parallel programmation.
		 */
		// Check current thread type
		Thread currentThread = Thread.currentThread();
		if (currentThread instanceof SvnClientThread)
			// Get Subversion repository from Subversion client thread
			repository = ((SvnClientThread) Thread.currentThread()).getRepository();
		// Get directory path
		String path = directory.getPath();
		try {
			// Get properties of directory
			SVNProperties properties = new SVNProperties();
			// Get path entries
			Collection<?> entries = repository.getDir(path, -1, properties, (Collection<?>) null);
			// Process each entry
			for (Object entry : entries) {
				SVNDirEntry dirEntry = (SVNDirEntry) entry;
				// Get entry kind
				SVNNodeKind nodeKind = dirEntry.getKind();
				// Check entry kind
				if (nodeKind==SVNNodeKind.DIR) {
					// Create Subversion directory
					SvnDirectory childDirectory = new SvnDirectory(dirEntry.getName());
					directory.addChild(childDirectory);
					// Recursively process directory
					this.prepareListDirectory(executorService, childDirectory, listener);
					continue;
				}
				// Create Subversion file
				SvnFile file = new SvnFile(dirEntry.getName());
				directory.addChild(file);
				// Update file counter
				this.fileCounter++;
			}
			/*
			 * Process externals.
			 */
			// Get directory externals property
			String externals = properties.getStringValue(SVNProperty.EXTERNALS);
			// Check if externals property is defined
			if (externals==null) {
				this.pendingDirectories.remove(directory);
				return;
			}
			try {
				// Parse external definition
				SVNExternal[] svnExternals = SVNExternal.parseExternals(path, externals);
				// Process each external
				for (SVNExternal svnExternal : svnExternals) {
					// Resolve external URL
					SVNURL rootUrl = repository.getRepositoryRoot(false);
					SVNURL ownerUrl = rootUrl.appendPath(path, false);
					svnExternal.resolveURL(rootUrl, ownerUrl);
					/*
					 * Create external resources up to external location.
					 */
					// Get external path
					String externalPath = svnExternal.getPath();
					// Split external resource path
					String[] externalPathPart = externalPath.split("/");
					// Create each child resource up to external location
					SvnDirectory parent = directory;
					for (int i = 0; i<externalPathPart.length-1; i++) {
						// Create child resource to external location
						SvnDirectory childDirectory = new SvnDirectory(externalPathPart[i]);
						// Add child resource
						parent.addChild(childDirectory);
						// Set child resource as next location
						parent = childDirectory;
					}
					/*
					 * Create external resource.
					 */
					// Get external name
					String externalName = externalPathPart[externalPathPart.length-1];
					// Get external URL path
					String urlPath = svnExternal.getResolvedURL().getPath();
					// Get external revision
					long revision = svnExternal.getRevision().getNumber();
					// Check external type
					SVNNodeKind nodeKind = repository.checkPath(urlPath, revision);
					// Create external resource
					AbstractResource externalResource;
					if (nodeKind==SVNNodeKind.DIR) {
						// Create external directory
						externalResource = new SvnDirectory(externalName);
					} else if (nodeKind==SVNNodeKind.FILE) {
						// Create external file
						externalResource = new SvnFile(externalName);
						// Update file counter
						this.fileCounter++;
					} else {
						// throw new ChecksumException("Unable to get external type for \""+urlPath+"\".");
						// TODO
						continue;
					}
					// Manually set path for external resource
					externalResource.setPath(svnExternal.getResolvedURL().getPath());
					// Manually set revision
					((SvnResource) externalResource).setRevision(revision);
					// Add external resource
					parent.addChild(externalResource);
					/*
					 * Process external.
					 */
					// Process external directory
					if (nodeKind==SVNNodeKind.DIR) {
						// Process external directory
						this.prepareListDirectory(executorService, (SvnDirectory) externalResource, listener);
					}
				}
			} catch (SVNException exception) {
				this.pendingDirectories.remove(directory);
				throw new ChecksumException("Unable to process external for \""+path+"\".", exception);
			}
		} catch (SVNException exception) {
			this.pendingDirectories.remove(directory);
			throw new ChecksumException("Unable to list Subversion directory \""+path+"\".", exception);
		}
		this.pendingDirectories.remove(directory);
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
	public void processDirectory(ExecutorService executorService, SvnDirectory directory, ChecksumListener listener) throws ChecksumException {
		// Process each child resource
		for (AbstractResource resource : directory.getChildren()) {
			// Check if should break
			if (this.shouldBreak)
				break;
			// Check resource type
			if (resource instanceof SvnDirectory)
				// Recursively process directory
				this.processDirectory(executorService, (SvnDirectory) resource, listener);
			else if (resource instanceof SvnFile)
				// Prepare file
				this.prepareFile(executorService, (SvnFile) resource, listener);
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
	public void prepareFile(ExecutorService executorService, final SvnFile file, final ChecksumListener listener) throws ChecksumException {
		// Submit a new task to process file
		executorService.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				// Check if should break
				if (SvnChecksumGenerator.this.shouldBreak)
					return null;
				try {
					// Process file
					SvnChecksumGenerator.this.processFile(repository, file);
					// Update progress counter
					SvnChecksumGenerator.this.progressCounter++;
					// Notify listener
					listener.onProgress(SvnChecksumGenerator.this.progressCounter*100/SvnChecksumGenerator.this.fileCounter);
				} catch (ChecksumException exception) {
					// Break the process
					SvnChecksumGenerator.this.shouldBreak = true;
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
	 * @param repository
	 *            The Subversion repository.
	 * @param file
	 *            The file to proceed.
	 * @throws ChecksumException
	 *             Throws exception if the checksum could not be computed.
	 */
	public void processFile(SVNRepository repository, SvnFile file) throws ChecksumException {
		/*
		 * Handle parallel programmation.
		 */
		// Check current thread type
		Thread currentThread = Thread.currentThread();
		if (currentThread instanceof SvnClientThread)
			// Get Subversion repository from Subversion client thread
			repository = ((SvnClientThread) Thread.currentThread()).getRepository();
		/*
		 * Get file keywords.
		 */
		// Get file path
		String path = file.getPath();
		// Get file properties
		SVNProperties properties = new SVNProperties();
		try {
			repository.getFile(path, -1, properties, null);
		} catch (SVNException exception) {
			throw new ChecksumException("Unable to get file properties for \""+path+"\".", exception);
		}
		// Get file keywords
		String keywords = properties.getStringValue(SVNProperty.KEYWORDS);
		Map<String, byte[]> keywordsMap = null;
		if (keywords!=null) {
			String cmtRev = properties.getStringValue(SVNProperty.COMMITTED_REVISION);
			String cmtDate = properties.getStringValue(SVNProperty.COMMITTED_DATE);
			String author = properties.getStringValue(SVNProperty.LAST_AUTHOR);
			keywordsMap = SVNTranslator.computeKeywords(keywords, repository.getLocation().toString(), author, cmtDate, cmtRev,
					SvnChecksumGenerator.SVN_OPTIONS);
		}
		/*
		 * Create digest output stream.
		 */
		// Create message digest
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance(SvnChecksumGenerator.DIGEST_ALGORITHM);
		} catch (NoSuchAlgorithmException exception) {
			throw new ChecksumException("Unable to compute \""+SvnChecksumGenerator.DIGEST_ALGORITHM+"\" checksum.", exception);
		}
		/*
		 * Get output stream for file content
		 */
		// Create output stream with digest decorator
		try (OutputStream outputStream = new ByteArrayOutputStream(); DigestOutputStream digestOutputStream = new DigestOutputStream(outputStream, digest)) {
			// Declare final output stream
			OutputStream finalOutputStream;
			// Create translator if file has keywords
			if (keywordsMap!=null) {
				// Get related file encoding description
				try {
					String eol = properties.getStringValue(SVNProperty.EOL_STYLE);
					String mimeType = properties.getStringValue(SVNProperty.MIME_TYPE);
					String charset = SVNTranslator.getCharset(properties.getStringValue(SVNProperty.CHARSET), mimeType, path, SvnChecksumGenerator.SVN_OPTIONS);
					// Create translating output stream for keywords
					finalOutputStream = SVNTranslator.getTranslatingOutputStream(digestOutputStream, charset,
							SVNTranslator.getEOL(eol, SvnChecksumGenerator.SVN_OPTIONS), false, keywordsMap, true);
				} catch (SVNException exception) {
					throw new ChecksumException("Unable to compute file encoding for \""+path+"\".", exception);
				}
			} else {
				// Use default digest output stream
				finalOutputStream = digestOutputStream;
			}
			// Get file content
			try {
				repository.getFile(path, -1, null, finalOutputStream);
			} catch (SVNException exception) {
				throw new ChecksumException("Unable to get file content for \""+path+"\".", exception);
			}
			// Compute digest
			byte[] digestBytes = digestOutputStream.getMessageDigest().digest();
			// Store digest to file
			file.setChecksum(digestBytes);
		} catch (IOException exception) {
			throw new ChecksumException("Unable to get file content for \""+path+"\".", exception);
		}
	}
}