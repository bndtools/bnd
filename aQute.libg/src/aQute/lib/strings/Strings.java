package aQute.lib.strings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.lib.collections.ExtList;

public class Strings {

	public static String join(String middle, Iterable< ? > objects) {
		return join(middle, objects, null, null);
	}

	public static String join(Iterable< ? > objects) {
		return join(",", objects, null, null);
	}

	public static String join(String middle, Iterable< ? > objects, Pattern pattern, String replace) {
		StringBuilder sb = new StringBuilder();
		join(sb, middle, objects, pattern, replace);
		return sb.toString();
	}

	public static void join(StringBuilder sb, String middle, Iterable< ? > objects, Pattern pattern, String replace) {
		String del = "";
		if (objects == null)
			return;

		for (Object o : objects) {
			if (o != null) {
				sb.append(del);
				String s = o.toString();
				if (pattern != null) {
					Matcher matcher = pattern.matcher(s);
					if (!matcher.matches())
						continue;

					s = matcher.replaceAll(replace);
				}
				sb.append(s);
				del = middle;
			}
		}
	}

	public static String join(String middle, Object[] segments) {
		return join(middle, new ExtList<Object>(segments));
	}

	public static String display(Object o, Object... ifNull) {
		if (o != null)
			return o.toString();

		for (int i = 0; i < ifNull.length; i++) {
			if (ifNull[i] != null)
				return ifNull[i].toString();
		}
		return "";
	}

	public static String join(String[] strings) {
		return join(",", strings);
	}

	public static String join(Object[] strings) {
		return join(",", strings);
	}

	public static String getLastSegment(String name, char c) {
		return name.substring(name.indexOf(c) + 1);
	}

	public static String getLastSegment(String name) {
		return getLastSegment(name, '.');
	}

	public static String trim(String s) {
		if (s == null)
			return null;

		if (s.isEmpty())
			return s;

		int start = 0;
		while (start < s.length() && Character.isWhitespace(s.charAt(start)))
			start++;

		int end = s.length();
		while (end > start && Character.isWhitespace(s.charAt(end - 1)))
			end--;

		if (start == 0 && end == s.length())
			return s;

		return s.substring(start, end);
	}
}
