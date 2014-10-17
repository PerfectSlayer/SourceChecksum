package fr.hardcoding.software.sourcechecksum.resource;

import java.util.regex.Pattern;

/**
 * This class is a path matcher.
 * 
 * @author Bruce BUJON
 *
 */
public class PathMatcher {
	/** The path matcher pattern. */
	private final Pattern pattern;

	/**
	 * Glob factory of pattern matcher.
	 * 
	 * @param pattern
	 *            The glob pattern.
	 * @return The pattern matcher related to the glob.
	 */
	public static PathMatcher fromGlob(String pattern) {
		return new PathMatcher(PathMatcher.convertGlobToRegEx(pattern));
	}

	/**
	 * Convert glob to regexp string.
	 * 
	 * @param glob
	 *            The glob string to convert.
	 * @return The related regexp.
	 */
	private static String convertGlobToRegEx(String glob) {
		// Create new builder
		StringBuilder builder = new StringBuilder();
		// Trim glob
		glob = glob.trim();
		// Remove leading star
		if (glob.startsWith("*"))
			glob = glob.substring(1);
		// Remove ending star
		if (glob.endsWith("*"))
			glob = glob.substring(0, glob.length()-1);
		// Declare escaping mode
		boolean escaping = false;
		// Declare bracing depth
		int bracingDepth = 0;
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
		// Return converted glob
		return builder.toString();
	}

	/**
	 * Constructor.
	 * 
	 * @param regex
	 *            The path match pattern regex.
	 */
	private PathMatcher(String regex) {
		this.pattern = Pattern.compile(regex);
	}

	/**
	 * Check if the path matcher match the path.
	 * 
	 * @param path
	 *            The path to check.
	 * @return <code>true</code> if the path matcher match the path, <code>false</code> otherwise.
	 */
	public boolean matches(String path) {
		return this.pattern.matcher(path).matches();
	}
}