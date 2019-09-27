package aQute.libg.glob;

import java.util.regex.Pattern;

public class AntGlob extends Glob {

	public static final AntGlob ALL = new AntGlob("**");

	public AntGlob(String globString) {
		this(globString, 0);
	}

	public AntGlob(String globString, int flags) {
		super(globString, toPattern(globString, flags));
	}

	// match forward slash or back slash (windows)
	private static final String	SLASHY		= "[\\\\/]";
	private static final String	NOT_SLASHY	= "[^\\\\/]";

	public static Pattern toPattern(String line) {
		return toPattern(line, 0);
	}

	public static Pattern toPattern(String line, int flags) {
		line = line.trim();
		int strLen = line.length();
		StringBuilder sb = new StringBuilder(strLen << 2);
		char previousChar = 0;
		for (int i = 0; i < strLen; i++) {
			char currentChar = line.charAt(i);
			switch (currentChar) {
				case '*' :
					int j, k;
					if ((i == 0 || isSlashy(previousChar)) && //
						((j = i + 1) < strLen && line.charAt(j) == '*') && //
						((k = j + 1) == strLen || isSlashy(line.charAt(k)))) {
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
				case '/' :
				case '\\' :
					if (i + 1 == strLen) {
						// ending with "/" is shorthand for ending with "/**"
						sb.append("(?:" + SLASHY + ".*|)");
					} else {
						sb.append(SLASHY);
					}
					break;
				case '.' :
				case '(' :
				case ')' :
				case '[' :
				case ']' :
				case '{' :
				case '}' :
				case '+' :
				case '|' :
				case '^' :
				case '$' :
					sb.append('\\');
					// FALL THROUGH
				default :
					sb.append(currentChar);
					break;
			}
			previousChar = currentChar;
		}
		return Pattern.compile(sb.toString(), flags);
	}

	private static boolean isSlashy(char c) {
		return c == '/' || c == '\\';
	}
}
