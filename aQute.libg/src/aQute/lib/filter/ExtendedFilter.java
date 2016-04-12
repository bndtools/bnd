/*
 * This used to have a license header that it was licensed by Gatespace in the year 2000. However, this was licensed
 * to the OSGi Alliance. A member donated this as ASL 2.0 licensed matching this project's default license.
 */
package aQute.lib.filter;

public class ExtendedFilter extends Filter {

	public ExtendedFilter(String filter) throws IllegalArgumentException {
		super(cleanup(filter), true);
	}

	public static String cleanup(String s) {
		if (s == null)
			return null;

		s = s.trim();
		if (s.startsWith("(") && s.endsWith(")"))
			return s;
		return "(" + s + ")";
	}
}
