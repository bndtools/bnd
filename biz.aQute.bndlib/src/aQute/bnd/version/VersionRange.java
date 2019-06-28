package aQute.bnd.version;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionRange {
	final Version					high;
	final Version					low;
	char							start	= '[';
	char							end		= ']';

	private final static Pattern	RANGE	= Pattern
		.compile("(\\(|\\[)\\s*(" + Version.VERSION_STRING + ")\\s*,\\s*(" + Version.VERSION_STRING + ")\\s*(\\)|\\])");

	public VersionRange(String string) {
		string = string.trim();

		// If a range starts with @ then we make it a
		// a semantic import range

		int auto = 0;
		if (string.startsWith("@")) {
			string = string.substring(1);
			auto = 1; // for consumers
		} else if (string.endsWith("@")) {
			string = string.substring(0, string.length() - 1);
			auto = 2; // for providers
		} else if (string.startsWith("=")) {
			string = string.substring(1);
			auto = 3;
		}

		Matcher m = RANGE.matcher(string);
		if (m.matches()) {
			start = m.group(1)
				.charAt(0);
			String v1 = m.group(2);
			String v2 = m.group(10);
			low = new Version(v1);
			high = new Version(v2);
			end = m.group(18)
				.charAt(0);
			if (low.compareTo(high) > 0)
				throw new IllegalArgumentException("Low Range is higher than High Range: " + low + "-" + high);

		} else {
			Version v = new Version(string);
			if (auto == 3) {
				start = '[';
				end = ']';
				low = v;
				high = v;
			} else if (auto != 0) {
				low = v;
				high = auto == 1 ? v.bumpMajor() : v.bumpMinor();
				start = '[';
				end = ')';
			} else {
				low = high = v;
			}
		}
	}

	public VersionRange(boolean b, Version lower, Version upper, boolean c) {
		start = b ? '[' : '(';
		end = c ? ']' : ')';
		low = lower;
		high = unique(upper);
	}

	public VersionRange(String low, String higher) {
		this(new Version(low), new Version(higher));
	}

	public VersionRange(Version low, Version higher) {
		this.low = low;
		this.high = unique(higher);
		start = '[';
		end = this.low.equals(this.high) ? ']' : ')';
	}

	static Version unique(Version v) {
		if (Version.HIGHEST.equals(v))
			return Version.HIGHEST;

		if (Version.LOWEST.equals(v))
			return Version.LOWEST;

		return v;
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
		if (high == Version.HIGHEST)
			return low.toString();

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
		List<Version> list = new ArrayList<>();
		for (Version v : versions) {
			if (includes(v))
				list.add(v);
		}
		return list;
	}

	/**
	 * Convert to an OSGi filter expression
	 */
	public String toFilter() {
		return toFilter("version");
	}

	/**
	 * Convert to an OSGi filter expression
	 */
	public String toFilter(String versionAttribute) {
		try (Formatter f = new Formatter()) {
			if (high == Version.HIGHEST)
				return "(" + versionAttribute + ">=" + low + ")";
			if (isRange()) {
				f.format("(&");
				if (includeLow())
					f.format("(%s>=%s)", versionAttribute, getLow());
				else
					f.format("(!(%s<=%s))", versionAttribute, getLow());
				if (includeHigh())
					f.format("(%s<=%s)", versionAttribute, getHigh());
				else
					f.format("(!(%s>=%s))", versionAttribute, getHigh());
				f.format(")");
			} else {
				f.format("(%s>=%s)", versionAttribute, getLow());
			}
			return f.toString();
		}
	}

	public static boolean isVersionRange(String stringRange) {
		return RANGE.matcher(stringRange)
			.matches();
	}

	/**
	 * Intersect two version ranges
	 */

	public VersionRange intersect(VersionRange other) {
		Version lower;
		char start = this.start;

		int lowc = this.low.compareTo(other.low);
		if (lowc <= 0) {
			lower = other.low;
			if (lowc != 0 || start == '[') {
				start = other.start;
			}
		} else {
			lower = this.low;
		}
		Version upper;
		char end = this.end;

		int highc = this.high.compareTo(other.high);
		if (highc >= 0) {
			upper = other.high;
			if (highc != 0 || end == ']') {
				end = other.end;
			}
		} else {
			upper = this.high;
		}
		return new VersionRange(start == '[', lower, upper, end == ']');
	}

	public static VersionRange parseVersionRange(String version) {
		if (!isVersionRange(version))
			return null;

		return new VersionRange(version);
	}

	public static VersionRange parseOSGiVersionRange(String version) {
		if (Version.isVersion(version))
			return new VersionRange(new Version(version), Version.HIGHEST);

		if (isVersionRange(version))
			return new VersionRange(version);
		return null;
	}

	public static boolean isOSGiVersionRange(String range) {
		return Version.isVersion(range) || isVersionRange(range);
	}

	public boolean isSingleVersion() {
		return high == Version.HIGHEST;
	}

	public static VersionRange likeOSGi(String version) {
		if (version == null) {
			return new VersionRange(Version.LOWEST, Version.HIGHEST);
		}

		if (Version.isVersion(version)) {
			return new VersionRange(new Version(version), Version.HIGHEST);
		}
		if (isVersionRange(version)) {
			return new VersionRange(version);
		}
		return null;
	}
}
