package aQute.bnd.version;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/**
 * Provides a model of an artifact version which can be used as a maven version.
 * <P>
 * The maven <a href="https://maven.apache.org/pom.html">POM reference</a> does
 * not define a format for versions. This is presumably intentional as it allows
 * artifacts with arbitrary versioning schemes to be referenced in a POM.
 * <P>
 * Maven tooling, on the other hand side, defines a rather <a href=
 * "http://books.sonatype.com/mvnref-book/reference/pom-relationships-sect-pom-syntax.html#pom-reationships-sect-versions">restrictive
 * version number pattern</a> for maven projects. Non-compliant version numbers
 * are parsed as qualifier-only versions.
 * <P>
 * The parsing methods of this class make an attempt to interpret a version
 * number as a
 * &lt;major&gt;/&lt;minor&gt;/&lt;micro/incremental&gt;/&lt;qualifier&gt;
 * pattern, even if it does not match the restrictive maven project version
 * number pattern. The string representation of an instance of this class is
 * always the original, unparsed (or "literal") representation because due to
 * the permissive parsing algorithm used, the original representation cannot
 * faithfully be reconstructed from the parsed components.
 */
public class MavenVersion implements ArtifactVersion {

	static Pattern					fuzzyVersion		= Pattern
			.compile("(\\d+)(\\.(\\d+)(\\.(\\d+))?)?([^a-zA-Z0-9](.*))?", Pattern.DOTALL);
	static Pattern					fuzzyVersionRange	= Pattern
			.compile("(\\(|\\[)\\s*([-\\da-zA-Z.]+)\\s*,\\s*([-\\da-zA-Z.]+)\\s*(\\]|\\))", Pattern.DOTALL);
	static Pattern					fuzzyModifier		= Pattern.compile("(\\d+[.-])*(.*)", Pattern.DOTALL);
	public static final String		VERSION_STRING		= "(\\d{1,15})(\\.(\\d{1,9})(\\.(\\d{1,9}))?)?([-\\.]?([-_\\.\\da-zA-Z]+))?";
	final static SimpleDateFormat	snapshotTimestamp	= new SimpleDateFormat("yyyyMMdd.HHmmss", Locale.ROOT);
	public final static Pattern		VERSIONRANGE		= Pattern.compile("((\\(|\\[)"

			+ VERSION_STRING + "," + VERSION_STRING + "(\\]|\\)))|" + VERSION_STRING);

	static {
		snapshotTimestamp.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private static final Pattern		VERSION		= Pattern.compile(VERSION_STRING);
	public static MavenVersion			UNRESOLVED	= new MavenVersion("0-UNRESOLVED");

	static final String					SNAPSHOT	= "SNAPSHOT";
	public static final MavenVersion	HIGHEST		= new MavenVersion(Version.HIGHEST);
	public static final MavenVersion	LOWEST		= new MavenVersion("0");

	// Used as "container" for the components of the maven version number
	private final Version				version;
	// Some maven versions are too odd to be restored after parsing, keep
	// original.
	private final String				literal;
	// Used for comparison, cached for efficiency.
	private final ComparableVersion		comparable;

	private final boolean				snapshot;

	/**
	 * Creates a new maven version from an osgi version. The version components
	 * are copied, the string representation is built from the components as
	 * "&lt;major&gt;.&lt;minor&gt;.&lt;micro&gt;.&lt;qualifier&gt;"
	 *
	 * @param osgiVersion the osgi version
	 */
	public MavenVersion(Version osgiVersion) {
		this.version = osgiVersion;
		String qual = "";
		if (this.version.qualifier != null)
			qual += "-" + this.version.qualifier;

		this.literal = osgiVersion.getWithoutQualifier().toString() + qual;
		this.comparable = new ComparableVersion(literal);
		this.snapshot = osgiVersion.isSnapshot();
	}

	/**
	 * Instantiates a new maven version representing the information from the
	 * argument. In addition to allowing the formats supported by
	 * {@link #parseString(String)}, this constructor supports formats such as
	 * "1.2rc1", i.e. without a separator before the qualifier.
	 *
	 * @param maven the version
	 */
	public MavenVersion(String maven) {
		this.version = new Version(cleanupVersion(maven));
		this.literal = maven;
		this.comparable = new ComparableVersion(literal);
		this.snapshot = maven.endsWith("-" + SNAPSHOT);
	}

	/**
	 * Parses the string as a maven version, but allows a dot as separator
	 * before the qualifier.
	 * <P>
	 * Leading sequences of digits followed by a dot or dash are converted to
	 * the major, minor and incremental version components. A dash or a dot that
	 * is not followed by a digit or the third dot is interpreted as the start
	 * of the qualifier.
	 * <P>
	 * In particular, version numbers such as "1.2.3.4.5" are parsed as major=1,
	 * minor=2, incremental=3 and qualifier="4.5". This is closer to the
	 * (assumed) semantics of such a version number than the parsing implemented
	 * in maven tooling, which interprets the complete version as a qualifier in
	 * such cases.
	 *
	 * @param versionStr the version string
	 * @return the maven version
	 * @throws IllegalArgumentException if the version cannot be parsed
	 */
	public static final MavenVersion parseString(String versionStr) {
		if (versionStr == null) {
			versionStr = "0";
		} else {
			versionStr = versionStr.trim();
			if (versionStr.isEmpty()) {
				versionStr = "0";
			}
		}
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

	/**
	 * Similar to {@link #parseString(String)}, but returns {@code null} if the
	 * version cannot be parsed.
	 * 
	 * @param versionStr the version string
	 * @return the maven version
	 */
	public static final MavenVersion parseMavenString(String versionStr) {
		try {
			return new MavenVersion(versionStr);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * This method is required by the {@link ArtifactVersion} interface.
	 * However, because instances of this class are intended to be immutable, it
	 * is not implemented. Use one of the other {@code parse...} methods
	 * instead.
	 *
	 * @param version the version to parse
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void parseVersion(String version) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public int getMajorVersion() {
		return version.major;
	}

	@Override
	public int getMinorVersion() {
		return version.minor;
	}

	@Override
	public int getIncrementalVersion() {
		return version.micro;
	}

	@Override
	public int getBuildNumber() {
		return new DefaultArtifactVersion(literal).getBuildNumber();
	}

	@Override
	public String getQualifier() {
		return version.qualifier;
	}

	public ComparableVersion getComparable() {
		return comparable;
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

	/**
	 * Compares maven version numbers according to the rules defined in the
	 * <a href=
	 * "https://maven.apache.org/pom.html#Version_Order_Specification">POM
	 * reference</a>.
	 */
	@Override
	public int compareTo(ArtifactVersion other) {
		if (other instanceof MavenVersion) {
			return comparable.compareTo(((MavenVersion) other).comparable);
		}
		return comparable.compareTo(new ComparableVersion(other.toString()));
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

	public static String toDateStamp(long epoch) {
		String datestamp;
		synchronized (snapshotTimestamp) {
			datestamp = snapshotTimestamp.format(new Date(epoch));
		}
		return datestamp;

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

		if (version == null || version.trim().isEmpty())
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
		Matcher m = fuzzyModifier.matcher(modifier);
		if (m.matches())
			modifier = m.group(2);

		for (int i = 0; i < modifier.length(); i++) {
			char c = modifier.charAt(i);
			if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '-')
				result.append(c);
		}
		if (l == result.length())
			result.append("_");
	}

}
