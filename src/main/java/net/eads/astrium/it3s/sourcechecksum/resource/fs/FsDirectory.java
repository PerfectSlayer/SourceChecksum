package net.eads.astrium.it3s.sourcechecksum.resource.fs;

import java.io.File;

import net.eads.astrium.it3s.sourcechecksum.resource.AbstractDirectory;

/**
 * This class represents a file system directory.
 * 
 * @author Bruce BUJON
 *
 */
public class FsDirectory extends AbstractDirectory {
	/** The related directory path. */
	private final File directory;

	/**
	 * Constructor.
	 * 
	 * @param directory
	 *            The related directory path.
	 */
	public FsDirectory(File directory) {
		super(directory.getName());
		this.directory = directory;
	}

	/**
	 * Get the related directory path.
	 * 
	 * @return The related directory path.
	 */
	public File getFile() {
		return this.directory;
	}
}