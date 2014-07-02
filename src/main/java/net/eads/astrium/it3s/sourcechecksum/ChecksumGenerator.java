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
			this.processDirectory(repository, ChecksumGenerator.path);

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
	 * @param path
	 *            The directory path to proceed.
	 * @throws ChecksumException
	 *             Throw exception if the checksum could not be computed.
	 */
	public void processDirectory(SVNRepository repository, String path) throws ChecksumException {
		try {
			// Get path entries
			Collection<?> entries = repository.getDir(path, -1, null, (Collection<?>) null);
			// Process each entry
			for (Object entry : entries) {
				SVNDirEntry dirEntry = (SVNDirEntry) entry;
				// Get entry kind
				SVNNodeKind nodeKind = dirEntry.getKind();
				// Get entry path
				String entryPath = path+"/"+dirEntry.getName();
				// Check entry kind
				if (nodeKind==SVNNodeKind.DIR) {
					// Recursively process directory
					this.processDirectory(repository, entryPath);
					continue;
				}
				// Process file
				this.processFile(repository, entryPath);
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
	 * @param path
	 *            The file path to proceed.
	 * @throws ChecksumException
	 *             Throws exception if the checksum could not be computed.
	 */
	public void processFile(SVNRepository repository, String path) throws ChecksumException {
		/*
		 * Get file keywords.
		 */
		// Get properties of the file
		SVNProperties properties = new SVNProperties();
		try {
			repository.getFile(path, -1, properties, null);
		} catch (SVNException exception) {
			throw new ChecksumException("Unable to get file properties for \""+path+"\".", exception);
		}
		// Get keywords of the file
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
			// Create hash string representation
			StringBuilder stringBuilder = new StringBuilder();
			for (byte b : digestBytes)
				stringBuilder.append(String.format("%02x", b));
			System.out.println(path);
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