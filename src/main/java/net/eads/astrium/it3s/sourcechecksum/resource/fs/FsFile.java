package net.eads.astrium.it3s.sourcechecksum.resource.fs;

import java.nio.file.Path;

import net.eads.astrium.it3s.sourcechecksum.resource.AbstractFile;

/**
 * This class represents a file system file.
 * 
 * @author Bruce BUJON
 *
 */
public class FsFile extends AbstractFile {
	/** The related file path. */
	private final Path file;

	/**
	 * Constructor.
	 * 
	 * @param file
	 *            The related file path.
	 */
	public FsFile(Path file) {
		super(file.getFileName().toString());
		this.file = file;
	}

	/**
	 * Get the related file path.
	 * 
	 * @return The related file path.
	 */
	public Path getFile() {
		return this.file;
	}
}