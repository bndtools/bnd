package aQute.bnd.version;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MavenVersion implements Comparable<MavenVersion> {

	private static final Pattern			fuzzyVersion		= Pattern
		.compile("(\\d+)(\\.(\\d+)(\\.(\\d+))?)?([^a-zA-Z0-9]?(.*))?", Pattern.DOTALL);
	private static final Pattern			fuzzyVersionRange	= Pattern
		.compile("(\\(|\\[)\\s*([-\\da-zA-Z.]+)\\s*,\\s*([-\\da-zA-Z.]+)\\s*(\\]|\\))", Pattern.DOTALL);
	private static final String				VERSION_STRING		= "(\\d{1,10})(\\.(\\d{1,10})(\\.(\\d{1,10}))?)?([-\\.]?([-_\\.\\da-zA-Z]+))?";
	private static final SimpleDateFormat	snapshotTimestamp	= new SimpleDateFormat("yyyyMMdd.HHmmss", Locale.ROOT);
	private static final Pattern			VERSIONRANGE		= Pattern
		.compile("((\\(|\\[)" + VERSION_STRING + "," + VERSION_STRING + "(\\]|\\)))|" + VERSION_STRING);

	static {
		snapshotTimestamp.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private static final Pattern		VERSION			= Pattern.compile(VERSION_STRING);
	public static final MavenVersion	UNRESOLVED		= new MavenVersion("0-UNRESOLVED");

	private static final String			SNAPSHOT		= "SNAPSHOT";
	public static final MavenVersion	HIGHEST			= new MavenVersion(Version.HIGHEST);
	public static final MavenVersion	LOWEST			= new MavenVersion("0-SNAPSHOT", Version.LOWEST, true);
	static final MavenVersion			RANGE_HIGHEST	= new MavenVersion("", Version.HIGHEST, false);
	static final MavenVersion			RANGE_LOWEST	= new MavenVersion("", Version.LOWEST, true);

	private final Version				version;
	private final String				literal;
	private final boolean				snapshot;
	private final ComparableVersion		qualifier;

	public MavenVersion(Version osgiVersion) {
		this.version = osgiVersion;
		String q = osgiVersion.qualifier;
		this.qualifier = ComparableVersion.parseVersion(q);

		String l = osgiVersion.toStringWithoutQualifier();
		if (q != null) {
			l += "-" + q;
		}
		this.literal = l;
		this.snapshot = osgiVersion.isSnapshot();
	}

	public MavenVersion(String maven) {
		Version osgiVersion = new Version(cleanupVersion(maven));
		this.version = osgiVersion;
		this.qualifier = ComparableVersion.parseVersion(osgiVersion.qualifier);
		this.literal = maven;
		this.snapshot = osgiVersion.isSnapshot();
	}

	private MavenVersion(String literal, Version osgiVersion, boolean snapshot) {
		this.version = osgiVersion;
		this.qualifier = ComparableVersion.parseVersion(osgiVersion.qualifier);
		this.literal = literal;
		this.snapshot = snapshot;
	}

	/**
	 * This parses an OSGi Version string into a MavenVersion which is not very
	 * interesting. You probably want {@link #parseMavenString(String)}.
	 */
	public static final MavenVersion parseString(String osgiVersionStr) {
		if (osgiVersionStr == null) {
			osgiVersionStr = "0";
		} else {
			osgiVersionStr = osgiVersionStr.trim();
			if (osgiVersionStr.isEmpty()) {
				osgiVersionStr = "0";
			}
		}
		Matcher m = VERSION.matcher(osgiVersionStr);
		if (!m.matches())
			throw new IllegalArgumentException("Invalid syntax for version: " + osgiVersionStr);

		int major = Integer.parseInt(m.group(1));
		int minor = (m.group(3) != null) ? Integer.parseInt(m.group(3)) : 0;
		int micro = (m.group(5) != null) ? Integer.parseInt(m.group(5)) : 0;
		String qualifier = m.group(7);
		Version version = new Version(major, minor, micro, qualifier);
		return new MavenVersion(version);
	}

	public static final MavenVersion parseMavenString(String versionStr) {
		try {
			return new MavenVersion(versionStr);
		} catch (Exception e) {
			return null;
		}
	}

	public Version getOSGiVersion() {
		return version;
	}

	/**
	 * If the qualifier ends with -SNAPSHOT or for an OSGI version with a
	 * qualifier that is SNAPSHOT
	 */

	public boolean isSnapshot() {
		return snapshot;
	}

	@Override
	public int compareTo(MavenVersion other) {
		if (other == this)
			return 0;

		Version o = other.version;
		int cmp = version.major - o.major;
		if (cmp != 0)
			return cmp;

		cmp = version.minor - o.minor;
		if (cmp != 0)
			return cmp;

		cmp = version.micro - o.micro;
		if (cmp != 0)
			return cmp;

		return qualifier.compareTo(other.qualifier);
	}

	@Override
	public String toString() {
		return literal;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((literal == null) ? 0 : literal.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MavenVersion other = (MavenVersion) obj;
		return literal.equals(other.literal);
	}

	public MavenVersion toSnapshot() {
		Version newv = new Version(version.getMajor(), version.getMinor(), version.getMicro(), SNAPSHOT);
		return new MavenVersion(newv);
	}

	public static String validate(String v) {
		if (v == null)
			return "Version is null";

		if (!VERSION.matcher(v)
			.matches())
			return "Not a version";

		return null;
	}

	public static String toDateStamp(long epoch) {
		synchronized (snapshotTimestamp) {
			return snapshotTimestamp.format(new Date(epoch));
		}
	}

	public static String toDateStamp(long epoch, String build) {
		String s = toDateStamp(epoch);
		if (build != null)
			s += "-" + build;

		return s;
	}

	public MavenVersion toSnapshot(long epoch, String build) {
		return toSnapshot(toDateStamp(epoch, build));
	}

	public MavenVersion toSnapshot(String timestamp, String build) {
		if (build != null)
			timestamp += "-" + build;
		return toSnapshot(timestamp);
	}

	public MavenVersion toSnapshot(String dateStamp) {
		// -SNAPSHOT == 9 characters
		String clean = literal.substring(0, literal.length() - 9);
		String result = clean + "-" + dateStamp;

		return new MavenVersion(result);
	}

	static public String cleanupVersion(String version) {

		if (version == null || version.trim()
			.isEmpty())
			return "0";

		Matcher m = VERSIONRANGE.matcher(version);

		if (m.matches()) {
			try {
				VersionRange vr = new VersionRange(version);
				return version;
			} catch (Exception e) {
				// ignore
			}
		}

		m = fuzzyVersionRange.matcher(version);
		if (m.matches()) {
			String prefix = m.group(1);
			String first = m.group(2);
			String last = m.group(3);
			String suffix = m.group(4);
			return prefix + cleanupVersion(first) + "," + cleanupVersion(last) + suffix;
		}

		m = fuzzyVersion.matcher(version);
		if (m.matches()) {
			StringBuilder result = new StringBuilder();
			String major = removeLeadingZeroes(m.group(1));
			String minor = removeLeadingZeroes(m.group(3));
			String micro = removeLeadingZeroes(m.group(5));
			String qualifier = m.group(7);

			if (qualifier == null) {
				if (!isInteger(minor)) {
					qualifier = minor;
					minor = "0";
				} else if (!isInteger(micro)) {
					qualifier = micro;
					micro = "0";
				}
			}
			if (major != null) {
				result.append(major);
				if (minor != null) {
					result.append(".");
					result.append(minor);
					if (micro != null) {
						result.append(".");
						result.append(micro);
						if (qualifier != null) {
							result.append(".");
							cleanupModifier(result, qualifier);
						}
					} else if (qualifier != null) {
						result.append(".0.");
						cleanupModifier(result, qualifier);
					}
				} else if (qualifier != null) {
					result.append(".0.0.");
					cleanupModifier(result, qualifier);
				}
				return result.toString();
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append("0.0.0.");
		cleanupModifier(sb, version);

		return sb.toString();
	}

	/**
	 * The cleanup version got confused when people used numeric dates like
	 * 201209091230120 as qualifiers. These are too large for Integers. This
	 * method checks if the all digit string fits in an integer.
	 *
	 * <pre>
	 *  maxint =
	 * 2,147,483,647 = 10 digits
	 * </pre>
	 *
	 * @param integer
	 * @return if this fits in an integer
	 */
	private static boolean isInteger(String minor) {
		return minor.length() < 10 || (minor.length() == 10 && minor.compareTo("2147483647") <= 0);
	}

	private static String removeLeadingZeroes(String group) {
		if (group == null)
			return "0";

		int n = 0;
		while (n < group.length() - 1 && group.charAt(n) == '0')
			n++;
		if (n == 0)
			return group;

		return group.substring(n);
	}

	static void cleanupModifier(StringBuilder result, String modifier) {
		int l = result.length();
		for (int i = 0; i < modifier.length(); i++) {
			char c = modifier.charAt(i);
			if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '-')
				result.append(c);
		}
		if (l == result.length())
			result.append("_");
	}

}
