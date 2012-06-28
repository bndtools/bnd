package aQute.libg.glob;

import java.util.*;
import java.util.regex.*;

public class Glob {

	private final String	glob;
	private final Pattern	pattern;

	public Glob(String globString) {
		this.glob = globString;
		this.pattern = Pattern.compile(convertGlobToRegEx(globString));
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
						sb.append('(');
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

	public void select(List<?> objects) {
		for ( Iterator<?> i =objects.iterator(); i.hasNext(); ) {
			String s = i.next().toString();
			if ( !matcher(s).matches())
				i.remove();
		}
	}

	public static Pattern toPattern(String s) {
		try {
			return Pattern.compile( convertGlobToRegEx(s));
		} catch( Exception e) {
			// ignore
		}
		return null;
	}
}
