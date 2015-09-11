package aQute.bnd.version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MavenVersion implements Comparable<MavenVersion> {

	public static final String VERSION_STRING = "(\\d{1,9})(\\.(\\d{1,9})(\\.(\\d{1,9}))?)?([-\\.]?([-_\\.\\da-zA-Z]+))?";

	private static final Pattern VERSION = Pattern.compile(VERSION_STRING);

	private static final String QUALIFIER_SNAPSHOT = "SNAPSHOT";

	private final Version osgiVersion;

	public MavenVersion(Version osgiVersion) {
		this.osgiVersion = osgiVersion;
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

	public Version getOSGiVersion() {
		return osgiVersion;
	}

	public boolean isSnapshot() {
		return QUALIFIER_SNAPSHOT.equals(osgiVersion.getQualifier());
	}

	public int compareTo(MavenVersion other) {
		return this.osgiVersion.compareTo(other.osgiVersion);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(osgiVersion.getWithoutQualifier());

		String qualifier = osgiVersion.getQualifier();
		if (qualifier != null && qualifier.length() > 0)
			builder.append('-').append(qualifier);
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((osgiVersion == null) ? 0 : osgiVersion.hashCode());
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
		if (osgiVersion == null) {
			if (other.osgiVersion != null)
				return false;
		} else if (!osgiVersion.equals(other.osgiVersion))
			return false;
		return true;
	}

}
