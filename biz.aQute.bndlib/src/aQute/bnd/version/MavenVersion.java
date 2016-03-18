package aQute.bnd.version;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.bnd.osgi.Verifier;

public class MavenVersion implements Comparable<MavenVersion> {
	static Pattern					fuzzyVersion		= Pattern
			.compile("(\\d+)(\\.(\\d+)(\\.(\\d+))?)?([^a-zA-Z0-9](.*))?", Pattern.DOTALL);
	static Pattern					fuzzyVersionRange	= Pattern
			.compile("(\\(|\\[)\\s*([-\\da-zA-Z.]+)\\s*,\\s*([-\\da-zA-Z.]+)\\s*(\\]|\\))", Pattern.DOTALL);
	static Pattern					fuzzyModifier		= Pattern.compile("(\\d+[.-])*(.*)", Pattern.DOTALL);
	public static final String		VERSION_STRING		= "(\\d{1,9})(\\.(\\d{1,9})(\\.(\\d{1,9}))?)?([-\\.]?([-_\\.\\da-zA-Z]+))?";
	final static SimpleDateFormat	snapshotTimestamp	= new SimpleDateFormat("yyyyMMdd.HHmmss", Locale.US);

	private static final Pattern VERSION = Pattern.compile(VERSION_STRING);

	static final String SNAPSHOT = "SNAPSHOT";

	private final Version	version;
	private final String	literal;

	private final boolean	snapshot;

	public MavenVersion(Version osgiVersion) {
		this.version = osgiVersion;
		String qual = "";
		if (this.version.qualifier != null)
			qual += "-" + this.version.qualifier;

		this.literal = osgiVersion.getWithoutQualifier().toString() + qual;
		this.snapshot = osgiVersion.isSnapshot();
	}

	public MavenVersion(String maven) {
		this.version = new Version(cleanupVersion(maven));
		this.literal = maven;
		this.snapshot = maven.endsWith("-" + SNAPSHOT);
	}

	public static final MavenVersion parseString(String versionStr) {
		versionStr = versionStr.trim();
		Matcher m = VERSION.matcher(versionStr);
		if (!m.matches())
			throw new IllegalArgumentException("Invalid syntax for version: " + versionStr);

		int major = Integer.parseInt(m.group(1));
		int minor = (m.group(3) != null) ? Integer.parseInt(m.group(3)) : 0;
		int micro = (m.group(5) != null) ? Integer.parseInt(m.group(5)) : 0;
		String qualifier = m.group(7);
		Version version = new Version(major, minor, micro, qualifier);
		return new MavenVersion(version);
	}

	public static final MavenVersion parseMavenString(String versionStr) {
		return new MavenVersion(versionStr);
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

	public int compareTo(MavenVersion other) {
		return this.version.compareTo(other.version);
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

		if (!VERSION.matcher(v).matches())
			return "Not a version";

		return null;
	}

	public MavenVersion toSnapshot(long epoch, String build) {
		String datestamp;
		synchronized (snapshotTimestamp) {
			datestamp = snapshotTimestamp.format(new Date(epoch));
		}

		return toSnapshot(datestamp, build);
	}

	public MavenVersion toSnapshot(String tstamp, String build) {
		// -SNAPSHOT == 9 characters
		String clean = literal.substring(0, literal.length() - 9);
		String result = clean + "-" + tstamp;
		if (build != null)
			result += "-" + build;
		
		return new MavenVersion(result);
	}
	static public String cleanupVersion(String version) {

		if (version == null)
			return "0";

		Matcher m = Verifier.VERSIONRANGE.matcher(version);

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
		return version;
	}

	/**
	 * TRhe cleanup version got confused when people used numeric dates like
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
		return minor.length() < 10 || (minor.length() == 10 && minor.compareTo("2147483647") < 0);
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
		Matcher m = fuzzyModifier.matcher(modifier);
		if (m.matches())
			modifier = m.group(2);

		for (int i = 0; i < modifier.length(); i++) {
			char c = modifier.charAt(i);
			if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '-')
				result.append(c);
		}
	}

}
