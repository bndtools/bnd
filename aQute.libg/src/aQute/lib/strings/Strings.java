package aQute.lib.strings;

import static java.util.stream.Collectors.toList;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import aQute.libg.qtokens.QuotedTokenizer;

public class Strings {
	private static final String COMMA = ",";

	public static String join(String middle, Iterable<?> objects) {
		if (objects == null) {
			return "";
		}
		return StreamSupport.stream(objects.spliterator(), false)
			.filter(Objects::nonNull)
			.map(Object::toString)
			.collect(Collectors.joining(middle));
	}

	public static String join(Iterable<?> objects) {
		return join(COMMA, objects);
	}

	public static String join(String middle, Iterable<?> objects, Pattern pattern, String replace) {
		if (pattern == null) {
			return join(middle, objects);
		}
		StringBuilder sb = new StringBuilder();
		join(sb, middle, objects, pattern, replace);
		return sb.toString();
	}

	public static void join(StringBuilder sb, String middle, Iterable<?> objects, Pattern pattern, String replace) {
		if (objects == null) {
			return;
		}

		String del = "";
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
		return join(middle, Arrays.asList(segments));
	}

	public static Collector<CharSequence, ?, String> joining() {
		return Collectors.joining(COMMA);
	}

	public static Collector<CharSequence, ?, String> joining(CharSequence delimiter, CharSequence prefix,
		CharSequence suffix, CharSequence emptyValue) {
		Function<StringJoiner, String> finisher = (emptyValue != null) ? joiner -> joiner.setEmptyValue(emptyValue)
			.toString() : joiner -> {
				String emptyMarker = new String();
				String joined = joiner.setEmptyValue(emptyMarker)
					.toString();
				return (joined != emptyMarker) ? joined : null;
			};
		return Collector.of(() -> new StringJoiner(delimiter, prefix, suffix), StringJoiner::add, StringJoiner::merge,
			finisher);
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
		return join(Arrays.asList(strings));
	}

	public static String join(Object[] strings) {
		return join(Arrays.asList(strings));
	}

	public static String getLastSegment(String name, char c) {
		return name.substring(name.lastIndexOf(c) + 1);
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

	public static boolean notEmpty(String s) {
		return !s.isEmpty();
	}

	public static boolean nonNullOrEmpty(String s) {
		return (s != null) && !s.isEmpty();
	}

	public static boolean nonNullOrTrimmedEmpty(String s) {
		return (s != null) && !s.trim()
			.isEmpty();
	}

	private final static Pattern SIMPLE_LIST_SPLITTER = Pattern.compile("\\s*,\\s*");

	public static Stream<String> splitAsStream(String s) {
		if ((s == null) || (s = s.trim()).isEmpty()) {
			return Stream.empty();
		}
		return SIMPLE_LIST_SPLITTER.splitAsStream(s)
			.filter(Strings::notEmpty);
	}

	public static List<String> split(String s) {
		return splitAsStream(s).collect(toList());
	}

	public static Stream<String> splitQuotedAsStream(String s) {
		return splitQuotedAsStream(s, true);
	}

	public static Stream<String> splitQuotedAsStream(String s, boolean retainQuotes) {
		if ((s == null) || (s = s.trim()).isEmpty()) {
			return Stream.empty();
		}
		return new QuotedTokenizer(s, COMMA, false, retainQuotes).stream()
			.filter(Strings::notEmpty);
	}

	private final static Pattern SIMPLE_LINE_SPLITTER = Pattern.compile("\r?\n");

	public static Stream<String> splitLinesAsStream(String s) {
		if (s == null) {
			return Stream.empty();
		}
		return SIMPLE_LINE_SPLITTER.splitAsStream(s);
	}

	public static List<String> splitLines(String s) {
		return splitLinesAsStream(s).collect(toList());
	}

	public static List<String> splitQuoted(String s) {
		return splitQuotedAsStream(s).collect(toList());
	}

	public static List<String> split(String regex, String s) {
		if ((s == null) || (s = s.trim()).isEmpty())
			return new ArrayList<>();
		return Pattern.compile(regex)
			.splitAsStream(s)
			.filter(Strings::notEmpty)
			.collect(toList());
	}

	public static boolean in(String[] skip, String key) {
		for (String s : skip) {
			if (key.equals(s))
				return true;
		}
		return false;
	}

	public static char charAt(String s, int n) {
		return s.charAt(adjustBegin(s, n));
	}

	public static String from(String s, int n) {
		return s.substring(adjustBegin(s, n));
	}

	public static String substring(String s, int begin, int end) {
		return s.substring(adjustBegin(s, begin), adjustEnd(s, end));
	}

	public static String substring(String s, int begin, int end, int stride) {
		StringBuilder sb = new StringBuilder();
		begin = adjustBegin(s, begin);
		end = adjustEnd(s, end);
		if (stride == 0)
			stride = 1;

		if (stride < 0)
			for (int i = end - 1; i >= begin; i += stride) {
				sb.append(s.charAt(i));
			}
		else
			for (int i = begin; i < end; i += stride) {
				sb.append(s.charAt(i));
			}

		return sb.toString();
	}

	public static String delete(String s, int begin, int end) {
		return s.substring(0, adjustBegin(s, begin)) + s.substring(adjustEnd(s, end));
	}

	public static String to(String s, int end) {
		return s.substring(0, adjustEnd(s, end));
	}

	public static int adjustBegin(String s, int n) {
		if (n < 0)
			n = s.length() + n;

		return n;
	}

	public static int adjustEnd(String s, int n) {
		if (n <= 0)
			n = s.length() + n;

		return n;
	}

	/**
	 * Split a string into a base and an extension.
	 *
	 * @param s the string that contains an extension
	 * @return null if no extension or an array of 2 elements, first is the
	 *         prefix and second is the extension without a '.'
	 */
	public static String[] extension(String s) {
		return last(s, '.');
	}

	/**
	 * Split a path (/ based) into a prefix and a last segment
	 *
	 * @param s the string that contains a path
	 * @return null if no extension or an array of 2 elements, first is the
	 *         prefix and second is the last segment without a '/' at the start
	 */
	public static String[] lastPathSegment(String s) {
		return last(s, '/');
	}

	/**
	 * Split a string into a prefix and a suffix based on the last time the
	 * separator appears
	 *
	 * @param s the string that contains a path
	 * @return null if no suffix or an array of 2 elements, first is the prefix
	 *         and second is the suffix without the separator at the start
	 */
	public static String[] last(String s, char separator) {
		int n = s.lastIndexOf(separator);
		if (n >= 0) {
			String[] answer = new String[2];
			answer[0] = s.substring(0, n);
			answer[1] = s.substring(n + 1);
			return answer;
		}
		return null;
	}

	public static String[] first(String s, char separator) {
		int n = s.indexOf(separator);
		if (n >= 0) {
			String[] answer = new String[2];
			answer[0] = s.substring(0, n);
			answer[1] = s.substring(n + 1);
			return answer;
		}
		return null;
	}

	public static String stripPrefix(String s, String prefix) {
		Pattern p = Pattern.compile(prefix);
		return stripPrefix(s, p);
	}

	public static String stripPrefix(String s, Pattern p) {
		Matcher matcher = p.matcher(s);
		if (matcher.lookingAt()) {
			return s.substring(matcher.end());
		}
		return null;
	}

	public static String stripSuffix(String s, String prefix) {
		Pattern p = Pattern.compile(prefix);
		return stripSuffix(s, p);
	}

	public static String stripSuffix(String s, Pattern p) {
		Matcher matcher = p.matcher(s);
		while (matcher.find()) {
			if (matcher.end() == s.length())
				return s.substring(0, matcher.start());
		}
		return null;
	}

	public static String ensureSuffix(String s, String suffix) {
		if (s.endsWith(suffix))
			return s;

		return s + suffix;
	}

	public static String ensurePrefix(String s, String prefix) {
		if (s.startsWith(prefix))
			return s;

		return prefix + s;
	}

	public static String times(String s, int times) {
		if (times <= 1)
			return s;

		StringBuilder sb = new StringBuilder(times * s.length());
		for (int i = 0; i < times; i++) {
			sb.append(s);
		}
		return sb.toString();
	}

	/**
	 * This method is the same as String.format but it makes sure that any
	 * arrays are transformed to strings.
	 *
	 * @param string
	 * @param parms
	 */
	public static String format(String string, Object... parms) {
		if (parms == null) {
			parms = new Object[0];
		}
		return String.format(string, makePrintableArray(parms));
	}

	private static Object[] makePrintableArray(Object array) {
		final int length = Array.getLength(array);
		Object[] output = new Object[length];
		for (int i = 0; i < length; i++) {
			output[i] = makePrintable(Array.get(array, i));
		}
		return output;
	}

	private static Object makePrintable(Object object) {
		if (object == null) {
			return null;
		}
		if (object.getClass()
			.isArray()) {
			return Arrays.toString(makePrintableArray(object));
		}
		return object;
	}

	/**
	 * Compare two strings except for where the first group in pattern. The
	 * patterns is matched in the strings using find(). Only group 1 is ignored.
	 * Use ignored groups {@code(?:...)} to ignore irrelevant groups.
	 *
	 * <pre>
	 * 		a = "abcdefxxxxghixxxxx678"
	 * 		b = "abcdefxxghix678"
	 * 		Pattern "(x+)"
	 * </pre>
	 *
	 * First developed to compare two XML files that only differed in their
	 * increment number, which was a time long.
	 *
	 * @param a the first string to compare
	 * @param b the second string to compare
	 * @param pattern where first group should be ignored in the comparison
	 * @return true if the strings are equal ignoring the first group's pattern
	 *         matches
	 */
	public static boolean compareExcept(String a, String b, Pattern pattern) {
		Matcher ma = pattern.matcher(a);
		Matcher mb = pattern.matcher(b);
		int ra = 0, rb = 0;

		while (ma.find()) {
			if (!mb.find()) {
				// pattern in first but not in second
				return false;
			}

			int sa = ma.start(1);
			int sb = mb.start(1);

			if (sa - ra != sb - rb) {
				// there must be differences before the pattern match
				// since the length to the start of the match differs
				// for both strings
				return false;
			}

			for (int i = 0; i < sa - ra; i++) {
				if (a.charAt(ra + i) != b.charAt(rb + i)) {
					// strings do not match
					return false;
				}
			}

			ra = ma.end() + 1;
			rb = mb.end() + 1;
		}
		if (a.length() - ra != b.length() - rb) {
			// there must be differences before the pattern match
			// since the length to the match differs
			return false;
		}

		for (int i = 0; i < a.length() - ra; i++) {
			if (a.charAt(ra + i) != b.charAt(rb + i)) {
				// strings do not match
				return false;
			}
		}

		return true;
	}

}
