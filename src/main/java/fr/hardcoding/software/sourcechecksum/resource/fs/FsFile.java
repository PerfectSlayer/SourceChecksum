package fr.hardcoding.software.sourcechecksum.resource.fs;

import java.io.File;

import fr.hardcoding.software.sourcechecksum.resource.AbstractFile;

/**
 * This class represents a file system file.
 * 
 * @author Bruce BUJON
 *
 */
public class FsFile extends AbstractFile {
	/** The related file path. */
	private final File file;

	/**
	 * Constructor.
	 * 
	 * @param file
	 *            The related file path.
	 */
	public FsFile(File file) {
		super(file.getName());
		this.file = file;
	}

	/**
	 * Get the related file path.
	 * 
	 * @return The related file path.
	 */
	public File getFile() {
		return this.file;
	}
}