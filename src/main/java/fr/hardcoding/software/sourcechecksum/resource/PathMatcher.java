package fr.hardcoding.software.sourcechecksum.resource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This class is a path matcher.
 * 
 * @author Bruce BUJON
 *
 */
public class PathMatcher {
	/** The glob path separator. */
	private static final String GLOB_PATH_SEPARATOR = "/";
	/** The any depth pattern syntax. */
	private static final String ANY_DEPTH_SYNTAX = "**";
	/** The path matcher patterns (in depth order). */
	private final Pattern[] patterns;

	/**
	 * Glob factory of pattern matcher.
	 * 
	 * @param pattern
	 *            The glob pattern.
	 * @return The pattern matcher related to the glob.
	 */
	public static PathMatcher fromGlob(String pattern) {
		// Split pattern according file separator
		String[] globParts = pattern.split(PathMatcher.GLOB_PATH_SEPARATOR);
		// Declare regex parts
		String[] regexParts = new String[globParts.length];
		// Convert each glob part to regex part
		for (int partIndex = 0; partIndex<globParts.length; partIndex++) {
			// Convert glob part to regex part
			regexParts[partIndex] = PathMatcher.convertGlobToRegEx(globParts[partIndex]);
		}
		// Return new path matcher
		return new PathMatcher(regexParts);
	}

	/**
	 * Convert glob to regexp string.
	 * 
	 * @param glob
	 *            The glob string to convert.
	 * @return The related regexp, <code>null</code> for any depth regexp.
	 */
	private static String convertGlobToRegEx(String glob) {
		// Trim glob
		glob = glob.trim();
		// Check any depth syntax
		if (glob.equals(PathMatcher.ANY_DEPTH_SYNTAX))
			return null;
		// Declare escaping mode
		boolean escaping = false;
		// Declare bracing depth
		int bracingDepth = 0;
		// Create new builder
		StringBuilder builder = new StringBuilder("^");
		// Process each char
		for (char currentChar : glob.toCharArray()) {
			// Check char
			switch (currentChar) {
			case '*':
				if (escaping)
					builder.append("\\*");
				else
					builder.append(".*");
				escaping = false;
				break;
			case '?':
				if (escaping)
					builder.append("\\?");
				else
					builder.append('.');
				escaping = false;
				break;
			case '.':
			case '(':
			case ')':
			case '+':
			case '|':
			case '^':
			case '$':
			case '@':
			case '%':
				builder.append('\\');
				builder.append(currentChar);
				escaping = false;
				break;
			case '\\':
				if (escaping) {
					builder.append("\\\\");
					escaping = false;
				} else
					escaping = true;
				break;
			case '{':
				if (escaping) {
					builder.append("\\{");
				} else {
					builder.append('(');
					bracingDepth++;
				}
				escaping = false;
				break;
			case '}':
				if (bracingDepth>0&&!escaping) {
					builder.append(')');
					bracingDepth--;
				} else if (escaping)
					builder.append("\\}");
				else
					builder.append("}");
				escaping = false;
				break;
			case ',':
				if (bracingDepth>0&&!escaping) {
					builder.append('|');
				} else if (escaping)
					builder.append("\\,");
				else
					builder.append(",");
				break;
			default:
				escaping = false;
				builder.append(currentChar);
			}
		}
		// End builder
		builder.append("$");
		// Return converted glob
		return builder.toString();
	}

	/**
	 * Constructor.
	 * 
	 * @param pathRegex
	 *            The path match pattern regex (in depth order).
	 */
	private PathMatcher(String... pathRegex) {
		// Create pattern array
		this.patterns = new Pattern[pathRegex.length];
		// Compile each pattern
		for (int index = 0; index<pathRegex.length; index++) {
			if (pathRegex[index]==null) {
				this.patterns[index] = null;

			} else {
				// Compile pattern
				this.patterns[index] = Pattern.compile(pathRegex[index]);
			}
		}
	}

	/**
	 * Check if the path matcher match the path.
	 * 
	 * @param path
	 *            The path to check.
	 * @return <code>true</code> if the path matcher match the path, <code>false</code> otherwise.
	 */
	public boolean matches(String path) {
		/*
		 * Parse file path as file list.
		 */
		// Get related path file
		File file = new File(path);
		// Declare file name list in depth order
		List<String> fileNames = new ArrayList<String>();
		// Fill the file stack until no parent found
		while (file!=null) {
			// Append file to the list
			fileNames.add(0, file.getName());
			// Get file parent
			file = file.getParentFile();
		}
		/*
		 * Check the path matcher.
		 */
		// Declare tested pattern
		Pattern pattern = null;
		// Declare pattern collection index
		int patternIndex = 0;
		// Declare tested file name
		String fileName = null;
		// Declare file collection index
		int fileIndex = 0;
		// Declare any depth check status
		boolean anyDepth = false;
		// Check each pattern match in depth order
		do {
			// Check if new pattern must be taken
			if (pattern==null) {
				// Check if remains pattern
				if (patternIndex>=this.patterns.length)
					// return as not matching
					return false;
				// Get next pattern
				pattern = this.patterns[patternIndex++];
			}
			// Check file name
			if (fileName==null) {
				// Check if remains file name
				if (fileIndex>=fileNames.size())
					// Return as not matching
					return false;
				// Get next file name
				fileName = fileNames.get(fileIndex++);
			}
			// Check any depth match
			if (pattern==null) {
				// Mark any depth check as enabled
				anyDepth = true;
				// Go to next check
				continue;
			}
			// Check if pattern match file name
			if (pattern.matcher(fileName).matches()) {
				// Clear pattern to get next
				pattern = null;
				// Clear matched file name
				fileName = null;
				// Mark any depth check as disabled
				anyDepth = false;
				// Go to next check
				continue;
			}
			// Check if any depth check is enabled
			if (anyDepth) {
				// Clear not matched file name
				fileName = null;
				// Go to next check
				continue;
			}
			// Return as not matching
			return false;
		} while (pattern!=null||patternIndex<this.patterns.length);
		// Check if all patterns match all files
		return pattern==null&&patternIndex==this.patterns.length&&(anyDepth==true||fileName==null)&&fileIndex==fileNames.size();
	}
}