package aQute.bnd.version;

import java.util.*;
import java.util.regex.*;

public class VersionRange {
	Version			high;
	Version			low;
	char			start	= '[';
	char			end		= ']';

	static Pattern	RANGE	= Pattern.compile("(\\(|\\[)\\s*(" + Version.VERSION_STRING + ")\\s*,\\s*("
									+ Version.VERSION_STRING + ")\\s*(\\)|\\])");

	public VersionRange(String string) {
		string = string.trim();
		Matcher m = RANGE.matcher(string);
		if (m.matches()) {
			start = m.group(1).charAt(0);
			String v1 = m.group(2);
			String v2 = m.group(10);
			low = new Version(v1);
			high = new Version(v2);
			end = m.group(18).charAt(0);
			if (low.compareTo(high) > 0)
				throw new IllegalArgumentException("Low Range is higher than High Range: " + low + "-" + high);

		} else
			high = low = new Version(string);
	}

	public boolean isRange() {
		return high != low;
	}

	public boolean includeLow() {
		return start == '[';
	}

	public boolean includeHigh() {
		return end == ']';
	}

	@Override
	public String toString() {
		if (high == low)
			return high.toString();

		StringBuilder sb = new StringBuilder();
		sb.append(start);
		sb.append(low);
		sb.append(',');
		sb.append(high);
		sb.append(end);
		return sb.toString();
	}

	public Version getLow() {
		return low;
	}

	public Version getHigh() {
		return high;
	}

	public boolean includes(Version v) {
		if (!isRange()) {
			return low.compareTo(v) <= 0;
		}
		if (includeLow()) {
			if (v.compareTo(low) < 0)
				return false;
		} else if (v.compareTo(low) <= 0)
			return false;

		if (includeHigh()) {
			if (v.compareTo(high) > 0)
				return false;
		} else if (v.compareTo(high) >= 0)
			return false;

		return true;
	}

	public Iterable<Version> filter(final Iterable<Version> versions) {
		List<Version> list = new ArrayList<Version>();
		for (Version v : versions) {
			if (includes(v))
				list.add(v);
		}
		return list;
	}

	/**
	 * Convert to an OSGi filter expression
	 * 
	 * @return
	 */
	public String toFilter() {
		Formatter f = new Formatter();
		try {
			if (isRange()) {
				f.format("(&");
				if (includeLow())
					f.format("(version>=%s)", getLow());
				else
					f.format("(!(version<=%s))", getLow());
				if (includeHigh())
					f.format("(version<=%s)", getHigh());
				else
					f.format("(!(version>=%s))", getHigh());
				f.format(")");
			} else {
				f.format("(version>=%s)", getLow());
			}
			return f.toString();
		}
		finally {
			f.close();
		}
	}

	public static boolean isVersionRange(String stringRange) {
		return RANGE.matcher(stringRange).matches();
	}
}