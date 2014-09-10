package net.eads.astrium.it3s.sourcechecksum.resource.fs;

import java.nio.file.Path;

import net.eads.astrium.it3s.sourcechecksum.resource.AbstractDirectory;

/**
 * This class represents a file system directory.
 * 
 * @author Bruce BUJON
 *
 */
public class FsDirectory extends AbstractDirectory {
	/** The related directory path. */
	private final Path directory;

	/**
	 * Constructor.
	 * 
	 * @param directory
	 *            The related directory path.
	 */
	public FsDirectory(Path directory) {
		super(directory.getFileName().toString());
		this.directory = directory;
	}

	/**
	 * Get the related directory path.
	 * 
	 * @return The related directory path.
	 */
	public Path getFile() {
		return this.directory;
	}
}