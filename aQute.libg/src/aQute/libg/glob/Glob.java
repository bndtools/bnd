package aQute.libg.glob;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Glob {

	public static final Glob	ALL	= new Glob("*");
	private final String	glob;
	private final Pattern	pattern;

	public Glob(String globString) {
		this(globString, 0);
	}

	public Glob(String globString, int flags) {
		this(globString, Pattern.compile(convertGlobToRegEx(globString), flags));
	}

	Glob(String globString, Pattern pattern) {
		this.glob = globString;
		this.pattern = pattern;
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
		int inCurlies = 0;
		for (int i = 0; i < strLen; i++) {
			char currentChar = line.charAt(i);
			switch (currentChar) {
				case '*' :
					sb.append(".*");
					break;
				case '?' :
					sb.append('.');
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
					break;
				case '\\' :
					sb.append('\\');
					if (i + 1 < strLen) {
						sb.append(line.charAt(++i));
					}
					break;
				case '{' :
					sb.append("(?:");
					inCurlies++;
					break;
				case '}' :
					if (inCurlies > 0) {
						sb.append(')');
						inCurlies--;
					} else {
						sb.append('}');
					}
					break;
				case ',' :
					if (inCurlies > 0) {
						sb.append('|');
					} else {
						sb.append(',');
					}
					break;
				default :
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
		return toPattern(s, 0);
	}

	public static Pattern toPattern(String s, int flags) {
		try {
			return Pattern.compile(convertGlobToRegEx(s), flags);
		} catch (Exception e) {
			// ignore
		}
		return null;
	}

	/**
	 * Get a list of files that match the glob expression
	 * 
	 * @param root the directory to get the files from
	 * @param recursive to traverse the dirs recursive
	 * @return file list
	 */
	public List<File> getFiles(File root, boolean recursive, boolean usePath) {
		List<File> result = new ArrayList<>();
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
