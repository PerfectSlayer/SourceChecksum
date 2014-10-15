package fr.hardcoding.software.sourcechecksum.thread;

import java.util.concurrent.ThreadFactory;

import org.tmatesoft.svn.core.io.SVNRepository;

import fr.hardcoding.software.sourcechecksum.ChecksumException;
import fr.hardcoding.software.sourcechecksum.generator.SvnChecksumGenerator;

/**
 * This class is a Subversion client thread factory.
 * 
 * @author Bruce BUJON
 *
 */
public class SvnClientThreadFactory implements ThreadFactory {
	/** The Subversion url base. */
	private final String url;
	/** The Subversion user name. */
	private final String user;
	/** The Subversion user password. */
	private final String passwd;
	/** The created thread counter. */
	private int threadCounter;

	/**
	 * Constructor.
	 * 
	 * @param url
	 *            The Subversion URL base.
	 * @param user
	 *            The Subversion user name.
	 * @param passwd
	 *            The Subversion user password.
	 */
	public SvnClientThreadFactory(String url, String user, String passwd) {
		// Store Subversion client data
		this.url = url;
		this.user = user;
		this.passwd = passwd;
		// Initialize thread counter
		this.threadCounter = 0;
	}

	/*
	 * Thread Factory.
	 */

	@Override
	public Thread newThread(Runnable runnable) {
		// Compute thread name
		String threadName = "SvnClientThread-"+this.threadCounter++;
		// Create Subversion repository
		SVNRepository repository;
		try {
			repository = SvnChecksumGenerator.createRepository(this.url, this.user, this.passwd);
		} catch (ChecksumException exception) {
			// Failed to create thread
			return null;
		}
		// Return created Subversion client thread
		return new SvnClientThread(runnable, threadName, repository);
	}
}