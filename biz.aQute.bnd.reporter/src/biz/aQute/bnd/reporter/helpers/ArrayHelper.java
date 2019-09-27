package biz.aQute.bnd.reporter.helpers;

import java.util.Arrays;

/**
 * Some {@code Array} utility functions.
 */
public class ArrayHelper {

	/**
	 * @param a the array in which we search for {@code x}, may be {@code null}
	 * @param x the value to look for, may be {@code null}
	 * @return true if {@code a} contains {@code x} ignoring case
	 */
	public static boolean containsIgnoreCase(final String[] a, final String x) {
		if (a == null) {
			return false;
		} else {
			return Arrays.stream(a)
				.anyMatch(e -> e.equalsIgnoreCase(x));
		}
	}

	/**
	 * @param a an array in which we search for a value which is also in
	 *            {@code b}, may be {@code null}
	 * @param b another array in which we search for a value which is also in
	 *            {@code a}, may be {@code null}
	 * @return any value (case insensitive) which is contained in both the
	 *         {@code a} and the {@code b} array or null if not found
	 */
	public static String oneInBoth(final String[] a, final String[] b) {
		if (a == null || b == null) {
			return null;
		} else {
			return Arrays.stream(a)
				.filter(x -> Arrays.stream(b)
					.anyMatch(y -> y != null && y.equalsIgnoreCase(x)))
				.findAny()
				.orElse(null);
		}
	}
}
