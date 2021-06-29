package aQute.bnd.version;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionRange {
	final Version					high;
	final Version					low;
	final boolean					includeLow;
	final boolean					includeHigh;

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
			includeLow = m.group(1)
				.charAt(0) == '[';
			String v1 = m.group(2);
			String v2 = m.group(10);
			low = new Version(v1);
			high = new Version(v2);
			includeHigh = m.group(18)
				.charAt(0) == ']';
			if (low.compareTo(high) > 0)
				throw new IllegalArgumentException("Low Range is higher than High Range: " + low + "-" + high);

		} else {
			Version v = new Version(string);
			if (auto == 3) {
				includeLow = true;
				low = v;
				high = v;
				includeHigh = true;
			} else if (auto != 0) {
				includeLow = true;
				low = v;
				high = auto == 1 ? v.bumpMajor() : v.bumpMinor();
				includeHigh = false;
			} else { // single version
				includeLow = true;
				low = v;
				high = Version.HIGHEST;
				includeHigh = true;
			}
		}
	}

	public VersionRange(boolean includeLow, Version low, Version high, boolean includeHigh) {
		this.includeLow = includeLow;
		this.low = low;
		this.high = unique(high);
		this.includeHigh = includeHigh;
	}

	public VersionRange(String low, String high) {
		this(new Version(low), new Version(high));
	}

	public VersionRange(Version low, Version high) {
		this.includeLow = true;
		this.low = low;
		this.high = unique(high);
		this.includeHigh = this.low.equals(this.high);
	}

	static Version unique(Version v) {
		if (Version.HIGHEST.equals(v))
			return Version.HIGHEST;

		if (Version.LOWEST.equals(v))
			return Version.LOWEST;

		return v;
	}

	public boolean isRange() {
		return getHigh() != getLow();
	}

	public boolean includeLow() {
		return includeLow;
	}

	public boolean includeHigh() {
		return includeHigh;
	}

	@Override
	public String toString() {
		if (isSingleVersion())
			return getLow().toString();

		StringBuilder sb = new StringBuilder();
		sb.append(includeLow() ? '[' : '(')
			.append(getLow())
			.append(',')
			.append(getHigh())
			.append(includeHigh() ? ']' : ')');
		return sb.toString();
	}

	public Version getLow() {
		return low;
	}

	public Version getHigh() {
		return high;
	}

	public boolean includes(Version v) {
		if (includeLow()) {
			if (v.compareTo(getLow()) < 0)
				return false;
		} else if (v.compareTo(getLow()) <= 0)
			return false;

		if (!isSingleVersion()) {
			if (includeHigh()) {
				if (v.compareTo(getHigh()) > 0)
					return false;
			} else if (v.compareTo(getHigh()) >= 0)
				return false;
		}

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
		StringBuilder result = new StringBuilder(128);
		final boolean needPresence = !includeLow() && (isSingleVersion() || !includeHigh());
		final boolean multipleTerms = needPresence || !isSingleVersion();

		if (multipleTerms) {
			result.append('(')
				.append('&');
		}
		if (needPresence) {
			result.append('(')
				.append(versionAttribute)
				.append('=')
				.append('*')
				.append(')');
		}
		if (includeLow()) {
			result.append('(')
				.append(versionAttribute)
				.append('>')
				.append('=')
				.append(getLow())
				.append(')');
		} else {
			result.append('(')
				.append('!')
				.append('(')
				.append(versionAttribute)
				.append('<')
				.append('=')
				.append(getLow())
				.append(')')
				.append(')');
		}
		if (!isSingleVersion()) {
			if (includeHigh()) {
				result.append('(')
					.append(versionAttribute)
					.append('<')
					.append('=')
					.append(getHigh())
					.append(')');
			} else {
				result.append('(')
					.append('!')
					.append('(')
					.append(versionAttribute)
					.append('>')
					.append('=')
					.append(getHigh())
					.append(')')
					.append(')');
			}
		}
		if (multipleTerms) {
			result.append(')');
		}
		return result.toString();
	}

	public static boolean isVersionRange(String stringRange) {
		return (stringRange != null) && RANGE.matcher(stringRange)
			.matches();
	}

	/**
	 * Intersect two version ranges
	 */

	public VersionRange intersect(VersionRange other) {
		Version low = getLow();
		boolean includeLow = includeLow();
		int lowc = low.compareTo(other.getLow());
		if (lowc <= 0) {
			low = other.getLow();
			if (lowc != 0 || includeLow) {
				includeLow = other.includeLow();
			}
		}

		Version high = getHigh();
		boolean includeHigh = includeHigh();
		int highc = high.compareTo(other.getHigh());
		if (highc >= 0) {
			high = other.getHigh();
			if (highc != 0 || includeHigh) {
				includeHigh = other.includeHigh();
			}
		}

		return new VersionRange(includeLow, low, high, includeHigh);
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
		return getHigh() == Version.HIGHEST;
	}

	/**
	 * Returns whether this version range is empty. A version range is empty if
	 * the set of versions defined by the interval is empty.
	 *
	 * @return {@code true} if this version range is empty; {@code false}
	 *         otherwise.
	 */
	public boolean isEmpty() {
		if (isSingleVersion()) { // infinity
			return false;
		}
		int comparison = getLow().compareTo(getHigh());
		if (comparison == 0) { // endpoints equal
			return !includeLow() || !includeHigh();
		}
		return comparison > 0; // true if low > high
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
