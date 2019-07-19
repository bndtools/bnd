package aQute.bnd.version;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.bnd.version.maven.ComparableVersion;
import aQute.lib.date.Dates;

public class MavenVersion implements Comparable<MavenVersion> {

	private static final Pattern			fuzzyVersion		= Pattern
		.compile("(\\d+)(\\.(\\d+)(\\.(\\d+))?)?([-.]?(.*))?");
	private static final Pattern			fuzzyVersionRange	= Pattern
		.compile("(\\(|\\[)\\s*([-.\\w]+)\\s*,\\s*([-.\\w]+)\\s*(\\]|\\))");
	private static final String				VERSION_STRING		= "(\\d{1,10})(\\.(\\d{1,10})(\\.(\\d{1,10})(\\.([-\\w]+))?)?)?";
	private static final Pattern			VERSION				= Pattern.compile(VERSION_STRING);
	private static final Pattern			VERSIONRANGE		= Pattern
		.compile("((\\(|\\[)" + VERSION_STRING + "," + VERSION_STRING + "(\\]|\\)))|" + VERSION_STRING);

	private static final DateTimeFormatter	MAVEN_SNAPSHOT_DATE_TIME	= DateTimeFormatter
		.ofPattern("yyyyMMdd.HHmmss", Locale.ROOT)
		.withZone(Dates.UTC_ZONE_ID);

	public static final MavenVersion	UNRESOLVED	= new MavenVersion("0-UNRESOLVED");

	private static final String			SNAPSHOT	= "SNAPSHOT";
	public static final MavenVersion	HIGHEST		= new MavenVersion("2147483647.2147483647.2147483647.2147483647");
	public static final MavenVersion	LOWEST		= new MavenVersion("alpha");

	private final Version				version;
	private final ComparableVersion		comparable;

	public MavenVersion(Version osgiVersion) {
		this.version = osgiVersion;
		this.comparable = new ComparableVersion(osgiVersion.toMavenString());
	}

	public MavenVersion(String maven) {
		this.version = new Version(cleanupVersion(maven));
		this.comparable = new ComparableVersion((maven != null) ? maven : "");
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
		return version.isSnapshot();
	}

	@Override
	public int compareTo(MavenVersion other) {
		if (other == this)
			return 0;

		return comparable.compareTo(other.comparable);
	}

	@Override
	public String toString() {
		return comparable.toString();
	}

	@Override
	public int hashCode() {
		return comparable.hashCode();
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
		return comparable.equals(other.comparable);
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
		return Dates.formatMillis(MAVEN_SNAPSHOT_DATE_TIME, epoch);
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
		String literal = comparable.toString();
		String clean = literal.substring(0, literal.length() - 9);
		String result = clean + "-" + dateStamp;

		return new MavenVersion(result);
	}

	static public String cleanupVersion(String version) {
		if ((version == null) || (version = version.trim()).isEmpty()) {
			return "0";
		}

		Matcher m = VERSIONRANGE.matcher(version);
		if (m.matches()) {
			try {
				VersionRange vr = new VersionRange(version);
				return version;
			} catch (Exception e) {
				// ignore
			}
		}

		StringBuilder result = new StringBuilder();

		m = fuzzyVersionRange.matcher(version);
		if (m.matches()) {
			String prefix = m.group(1);
			String first = m.group(2);
			String last = m.group(3);
			String suffix = m.group(4);
			result.append(prefix)
				.append(cleanupVersion(first))
				.append(',')
				.append(cleanupVersion(last))
				.append(suffix);
			return result.toString();
		}

		m = fuzzyVersion.matcher(version);
		if (m.matches()) {
			String major = removeLeadingZeroes(m.group(1));
			if (isInteger(major)) {
				result.append(major);
				String minor = removeLeadingZeroes(m.group(3));
				if (!isInteger(minor)) {
					result.append(".0.0.")
						.append(minor); // minor is all digits
					String micro = m.group(4);
					if (micro != null) {
						cleanupModifier(result, micro);
					}
					String qualifier = m.group(6);
					if (!qualifier.isEmpty()) {
						cleanupModifier(result, qualifier);
					}
					return result.toString();
				}
				result.append('.')
					.append(minor);
				String micro = removeLeadingZeroes(m.group(5));
				if (!isInteger(micro)) {
					result.append(".0.")
						.append(micro); // micro is all digits
					String qualifier = m.group(6);
					if (!qualifier.isEmpty()) {
						cleanupModifier(result, qualifier);
					}
					return result.toString();
				}
				result.append('.')
					.append(micro);
				String qualifier = m.group(7);
				if (!qualifier.isEmpty()) {
					result.append('.');
					cleanupModifier(result, qualifier);
				}
				return result.toString();
			}
		}

		result.append("0.0.0.");
		cleanupModifier(result, version);
		return result.toString();
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
