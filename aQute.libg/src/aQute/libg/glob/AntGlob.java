package aQute.libg.glob;

import java.util.regex.Pattern;

public class AntGlob extends Glob {

	public static final AntGlob ALL = new AntGlob("**");

	public AntGlob(String globString) {
		this(globString, 0);
	}

	public AntGlob(String globString, int flags) {
		super(globString, Pattern.compile(convertAntGlobToRegEx(globString), flags));
	}

	private static String convertAntGlobToRegEx(String line) {
		line = line.trim();
		int strLen = line.length();
		boolean ending;
		if (line.endsWith("/**")) {
			ending = true;
			strLen -= 3;
		} else if (line.endsWith("/")) {
			// ending with "/" is shorthand for ending with "/**"
			ending = true;
			strLen--;
		} else {
			ending = false;
		}
		StringBuilder sb = new StringBuilder(strLen);
		int inCurlies = 0;
		for (int i = 0; i < strLen; i++) {
			char currentChar = line.charAt(i);
			switch (currentChar) {
				case '*' :
					if (i + 1 < strLen && line.charAt(i + 1) == '*') {
						i++;
						if (i + 1 < strLen && line.charAt(i + 1) == '/') {
							i++;
							sb.append("(?:.*[/\\\\]|)");
						} else {
							sb.append(".*");
						}
					} else {
						sb.append("[^/\\\\]*");
					}
					break;
				case '?' :
					sb.append("[^/\\\\]");
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
				case '/' :
					sb.append("[/\\\\]");
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
		if (ending) {
			sb.append("(?:[/\\\\].*|)");
		}
		return sb.toString();
	}

	public static Pattern toPattern(String s) {
		return toPattern(s, 0);
	}

	public static Pattern toPattern(String s, int flags) {
		try {
			return Pattern.compile(convertAntGlobToRegEx(s), flags);
		} catch (Exception e) {
			// ignore
		}
		return null;
	}
}
