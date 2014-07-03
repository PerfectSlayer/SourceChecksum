package net.eads.astrium.it3s.sourcechecksum;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Map;

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
public class ChecksumGenerator {
	/** The digest algorithm name. */
	private static final String DIGEST_ALGORITHM = "MD5"; // SHA-256
	/** The default Subversion options. */
	private static final ISVNOptions SVN_OPTIONS = SVNWCUtil.createDefaultOptions(true);

	private static String serverRoot;
	private static String path;
	private static String user;
	private static String passwd;

	/**
	 * Constructor.
	 */
	public ChecksumGenerator() {
		try {
			SVNRepository repository = this.createRepository(ChecksumGenerator.serverRoot, ChecksumGenerator.user, ChecksumGenerator.passwd);
			SvnDirectory rootResource = new SvnDirectory(path);
			this.processDirectory(repository, rootResource);

		} catch (ChecksumException exception) {
			// TODO Auto-generated catch block
			exception.printStackTrace();
		}
	}

	/**
	 * Process directory to compute content checksum.
	 * 
	 * @param repository
	 *            The Subversion repository.
	 * @param directory
	 *            The directory path to proceed.
	 * @throws ChecksumException
	 *             Throw exception if the checksum could not be computed.
	 */
	public void processDirectory(SVNRepository repository, SvnDirectory directory) throws ChecksumException {
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
					this.processDirectory(repository, childDirectory);
					continue;
				}
				// Create Subversion file
				SvnFile file = new SvnFile(dirEntry.getName());
				directory.addChild(file);
				// Process file
				this.processFile(repository, file);
			}
			/*
			 * Process externals.
			 */
			// Get directory externals property
			String externals = properties.getStringValue(SVNProperty.EXTERNALS);
			// Check if externals property is defined
			if (externals==null)
				return;
			try {
				// Parse external definition
				SVNExternal[] svnExternals = SVNExternal.parseExternals(path, externals);
				// Process each external
				for (SVNExternal svnExternal : svnExternals) {
					// Resolve external URL
					SVNURL rootUrl = repository.getRepositoryRoot(false);
					SVNURL ownerUrl = rootUrl.appendPath(path, false);
					svnExternal.resolveURL(rootUrl, ownerUrl);

					// System.err.println("Unresolved: "+svnExternal.getUnresolvedUrl());
					// System.err.println("Resolved: "+svnExternal.getResolvedURL());
					// System.err.println("Resolved path: "+svnExternal.getResolvedURL().getPath());
					// System.err.println("Path: "+svnExternal.getPath());

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
					SvnResource externalResource;
					if (nodeKind==SVNNodeKind.DIR) {
						// Create external directory
						externalResource = new SvnDirectory(externalName);
					} else if (nodeKind==SVNNodeKind.FILE) {
						// Create external file
						externalResource = new SvnFile(externalName);
					} else {
						throw new ChecksumException("Unable to get external type for \""+urlPath+"\".");
					}
					// Manually set path for external resource
					externalResource.setPath(svnExternal.getResolvedURL().getPath());
					// Manually set revision
					externalResource.setRevision(revision);
					// Add external resource
					parent.addChild(externalResource);
					/*
					 * Process external.
					 */
					// Process external resource
					if (nodeKind==SVNNodeKind.DIR) {
						// Process external directory
						this.processDirectory(repository, (SvnDirectory) externalResource);
					} else {
						// Process external file
						this.processFile(repository, (SvnFile) externalResource);
					}
				}
			} catch (SVNException exception) {
				throw new ChecksumException("Unable to process external for \""+path+"\".", exception);
			}
		} catch (SVNException exception) {
			throw new ChecksumException("Unable to list Subversion directory \""+path+"\".", exception);
		}
	}

	/**
	 * Process file.
	 * 
	 * @param repository
	 *            The Subversion repository.
	 * @param file
	 *            The file path to proceed.
	 * @throws ChecksumException
	 *             Throws exception if the checksum could not be computed.
	 */
	public void processFile(SVNRepository repository, SvnFile file) throws ChecksumException {
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
			keywordsMap = SVNTranslator.computeKeywords(keywords, repository.getLocation().toString(), author, cmtDate, cmtRev, ChecksumGenerator.SVN_OPTIONS);
		}
		/*
		 * Create digest output stream.
		 */
		// Create message digest
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance(ChecksumGenerator.DIGEST_ALGORITHM);
		} catch (NoSuchAlgorithmException exception) {
			throw new ChecksumException("Unable to compute \""+ChecksumGenerator.DIGEST_ALGORITHM+"\" checksum.", exception);
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
					String charset = SVNTranslator.getCharset(properties.getStringValue(SVNProperty.CHARSET), mimeType, path, ChecksumGenerator.SVN_OPTIONS);
					// Create translating output stream for keywords
					finalOutputStream = SVNTranslator.getTranslatingOutputStream(digestOutputStream, charset,
							SVNTranslator.getEOL(eol, ChecksumGenerator.SVN_OPTIONS), false, keywordsMap, true);
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

			// Create hash string representation
			StringBuilder stringBuilder = new StringBuilder();
			for (byte b : digestBytes)
				stringBuilder.append(String.format("%02x", b));
			System.out.println(file.getWorkingCopyPath());
			System.out.println(stringBuilder.toString());
		} catch (IOException exception) {
			throw new ChecksumException("Unable to get file content for \""+path+"\".", exception);
		}
	}

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
	public SVNRepository createRepository(String url, String name, String passwd) throws ChecksumException {
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
	 * The main procedure.
	 * 
	 * @param args
	 *            The CLI parameters.
	 */
	public static void main(String[] args) {
		if (args.length<3)
			System.exit(-1);
		ChecksumGenerator.serverRoot = args[0];
		ChecksumGenerator.path = args[1];
		ChecksumGenerator.user = args[2];
		if (args.length<4) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
				System.out.print("Type user password: ");
				ChecksumGenerator.passwd = reader.readLine();
			} catch (IOException exception) {
				System.exit(-1);
			}
		} else {
			ChecksumGenerator.passwd = args[3];
		}
		new ChecksumGenerator();
	}
}