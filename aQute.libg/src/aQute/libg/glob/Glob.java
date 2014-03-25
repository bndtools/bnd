package aQute.libg.glob;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class Glob {

	private final String	glob;
	private final Pattern	pattern;

	public Glob(String globString) {
		this(globString,0);
	}

	public Glob(String globString, int flags) {
		this.glob = globString;
		this.pattern = Pattern.compile(convertGlobToRegEx(globString), flags);
	}
	
	public Matcher matcher(CharSequence input) {
		return pattern.matcher(input);
	}

	@Override
	public String toString() {
		return glob;
	}

	private static String convertGlobToRegEx(String line) {
		line = line.trim();
		int strLen = line.length();
		StringBuilder sb = new StringBuilder(strLen);
		boolean escaping = false;
		int inCurlies = 0;
		for (char currentChar : line.toCharArray()) {
			switch (currentChar) {
				case '*' :
					if (escaping)
						sb.append("\\*");
					else
						sb.append(".*");
					escaping = false;
					break;
				case '?' :
					if (escaping)
						sb.append("\\?");
					else
						sb.append('.');
					escaping = false;
					break;
				case '.' :
				case '(' :
				case ')' :
				case '+' :
				case '|' :
				case '^' :
				case '$' :
				case '@' :
				case '%' :
					sb.append('\\');
					sb.append(currentChar);
					escaping = false;
					break;
				case '\\' :
					if (escaping) {
						sb.append("\\\\");
						escaping = false;
					} else
						escaping = true;
					break;
				case '{' :
					if (escaping) {
						sb.append("\\{");
					} else {
						sb.append("(?:");
						inCurlies++;
					}
					escaping = false;
					break;
				case '}' :
					if (inCurlies > 0 && !escaping) {
						sb.append(')');
						inCurlies--;
					} else if (escaping)
						sb.append("\\}");
					else
						sb.append("}");
					escaping = false;
					break;
				case ',' :
					if (inCurlies > 0 && !escaping) {
						sb.append('|');
					} else if (escaping)
						sb.append("\\,");
					else
						sb.append(",");
					break;
				default :
					escaping = false;
					sb.append(currentChar);
			}
		}
		return sb.toString();
	}

	public void select(List< ? > objects) {
		for (Iterator< ? > i = objects.iterator(); i.hasNext();) {
			String s = i.next().toString();
			if (!matcher(s).matches())
				i.remove();
		}
	}

	public static Pattern toPattern(String s) {
		return toPattern(s,0);
	}
	
	public static Pattern toPattern(String s, int flags) {
		try {
			return Pattern.compile(convertGlobToRegEx(s), flags);
		}
		catch (Exception e) {
			// ignore
		}
		return null;
	}

	/**
	 * Get a list of files that match the glob expression
	 * 
	 * @param root
	 *            the directory to get the files from
	 * @param recursive
	 *            to traverse the dirs recursive
	 * @return
	 */
	public List<File> getFiles(File root, boolean recursive, boolean usePath) {
		List<File> result = new ArrayList<File>();
		getFiles(root, result, recursive, usePath);
		return result;
	}

	public void getFiles(File root, List<File> result, boolean recursive, boolean usePath) {
		if (root == null || !root.isDirectory())
			return;

		for (File sub : root.listFiles()) {
			if (sub.isFile()) {
				String s = usePath ? sub.getAbsolutePath() : sub.getName();
				if (matcher(s).matches())
					result.add(sub);
			} else if (recursive && sub.isDirectory())
				getFiles(sub, result, recursive, usePath);
		}
	}
}
