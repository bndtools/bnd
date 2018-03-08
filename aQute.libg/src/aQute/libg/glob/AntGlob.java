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

	private static final String	SLASHY		= "[/\\\\]";
	private static final String	NOT_SLASHY	= "[^/\\\\]";
	private static String convertAntGlobToRegEx(String line) {
		line = line.trim();
		int strLen = line.length();
		StringBuilder sb = new StringBuilder(strLen);
		int inCurlies = 0;
		for (int i = 0; i < strLen; i++) {
			char currentChar = line.charAt(i);
			switch (currentChar) {
				case '*' :
					int j, k;
					if ((i == 0 || line.charAt(i - 1) == '/') && //
						((j = i + 1) < strLen && line.charAt(j) == '*') && //
						((k = j + 1) == strLen || line.charAt(k) == '/')) {
						if (i == 0 && k < strLen) { // line starts with "**/"
							sb.append("(?:.*" + SLASHY + "|)");
							i = k;
						} else if (i > 1) { // after "x/"
							sb.setLength(sb.length() - SLASHY.length());
							sb.append("(?:" + SLASHY + ".*|)");
							i = j;
						} else {
							sb.append(".*");
							i = j;
						}
					} else {
						sb.append(NOT_SLASHY + "*");
					}
					break;
				case '?' :
					sb.append(NOT_SLASHY);
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
					if (i + 1 == strLen) {
						// ending with "/" is shorthand for ending with "/**"
						sb.append("(?:" + SLASHY + ".*|)");
					} else {
						sb.append(SLASHY);
					}
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
