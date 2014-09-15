package net.eads.astrium.it3s.sourcechecksum.thread;

import java.util.concurrent.ThreadFactory;

import net.eads.astrium.it3s.sourcechecksum.ChecksumException;
import net.eads.astrium.it3s.sourcechecksum.generator.SvnChecksumGenerator;

import org.tmatesoft.svn.core.io.SVNRepository;

/**
 * This class is a Subversion client thread factory.
 * 
 * @author Bruce BUJON
 *
 */
public class SvnClientThreadFactory implements ThreadFactory {
	/** The Subversion server root. */
	private final String serverRoot;
	/** The Subversion user name. */
	private final String user;
	/** The Subversion user password. */
	private final String passwd;
	/** The created thread counter. */
	private int threadCounter;

	/**
	 * Constructor.
	 * 
	 * @param serverRoot
	 *            The Subversion server root.
	 * @param user
	 *            The Subversion user name.
	 * @param passwd
	 *            The Subversion user password.
	 */
	public SvnClientThreadFactory(String serverRoot, String user, String passwd) {
		// Store Subversion client data
		this.serverRoot = serverRoot;
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
			repository = SvnChecksumGenerator.createRepository(serverRoot, user, passwd);
		} catch (ChecksumException exception) {
			// Failed to create thread
			return null;
		}
		// Return created Subversion client thread
		return new SvnClientThread(runnable, threadName, repository);
	}
}