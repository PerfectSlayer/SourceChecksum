package fr.hardcoding.software.sourcechecksum.thread;

import org.tmatesoft.svn.core.io.SVNRepository;

/**
 * This class is a specific thread to perform multi-threading Subversion operations.
 * 
 * @author Bruce BUJON
 *
 */
public class SvnClientThread extends Thread {
	/** The Subversion repository of the thread. */
	final SVNRepository repository;

	/**
	 * Constructor.
	 * 
	 * @param target
	 *            The target to execute.
	 * @param name
	 *            The name of the thread.
	 * @param repository
	 *            The Subversion client of the thread.
	 */
	public SvnClientThread(Runnable target, String name, SVNRepository repository) {
		super(target, name);
		this.repository = repository;
	}

	/**
	 * Get the Subversion repository of the thread.
	 * 
	 * @return The Subversion repository of the thread.
	 */
	public SVNRepository getRepository() {
		return this.repository;
	}
}