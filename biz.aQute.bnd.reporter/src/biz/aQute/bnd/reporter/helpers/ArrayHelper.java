package biz.aQute.bnd.reporter.helpers;

import java.util.Arrays;

final public class ArrayHelper {
	
	private ArrayHelper() {
	}
	
	public static boolean containsIgnoreCase(final String[] a, final String x) {
		if (a == null) {
			return false;
		} else {
			return Arrays.stream(a).anyMatch(e -> e.equalsIgnoreCase(x));
		}
	}
	
	public static String oneInBoth(final String[] a, final String[] b) {
		if (a == null || b == null) {
			return null;
		} else {
			return Arrays.stream(a).filter(x -> Arrays.stream(b).anyMatch(y -> y != null && y.equalsIgnoreCase(x))).findAny()
				.orElse(null);
		}
	}
}
