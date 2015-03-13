package aQute.bnd.deployer.repository.aether;

import static java.lang.Integer.*;

import java.util.regex.*;

import aQute.bnd.version.*;

public class MvnVersion implements Comparable<MvnVersion> {

	public final static String		VERSION_STRING		= "(\\d{1,9})(\\.(\\d{1,9}|[-_\\da-zA-Z]+)(\\.(\\d{1,9}|[-_\\da-zA-Z]+)((\\.|-)?([-_\\da-zA-Z]+))?)?)?";

	private static final Pattern	VERSION				= Pattern.compile(VERSION_STRING);

	private static final Pattern	NUM					= Pattern.compile("\\d{1,9}");

	private static final String		QUALIFIER_SNAPSHOT	= "SNAPSHOT";

	private final Version			osgiVersion;

	public MvnVersion(Version osgiVersion) {
		this.osgiVersion = osgiVersion;
	}

	public static final MvnVersion parseString(String versionStr) {
		MvnVersion result = null;

		try {
			Matcher m = VERSION.matcher(versionStr);
			if (m.find()) {
				String major = m.group(1);
				String minor = m.group(3);
				String micro = m.group(5);
				String qualifier = m.group(8);
				if (m.end() < versionStr.length()) {
					qualifier = qualifier + versionStr.substring(m.end(8));
				}
				Version version;
				if (minor != null && NUM.matcher(minor).matches()) {
					if (micro != null && NUM.matcher(micro).matches()) {
						if (qualifier != null && qualifier.length() > 0) {
							version = new Version(parseInt(major), parseInt(minor), parseInt(micro), qualifier);
						} else {
							version = new Version(parseInt(major), parseInt(minor), parseInt(micro));
						}
					} else if (micro != null) {
						version = new Version(parseInt(major), parseInt(minor), 0, micro);
					} else {
						version = new Version(parseInt(major), parseInt(minor));
					}
				} else if (minor != null) {
					version = new Version(parseInt(major), 0, 0, minor);
				} else {
					version = new Version(parseInt(major));
				}
				return new MvnVersion(version);
			}
		}
		catch (IllegalArgumentException e) { // bad format
			result = null;
		}
		return result;
	}

	public Version getOSGiVersion() {
		return osgiVersion;
	}

	public boolean isSnapshot() {
		return QUALIFIER_SNAPSHOT.equals(osgiVersion.getQualifier());
	}

	public int compareTo(MvnVersion other) {
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
		MvnVersion other = (MvnVersion) obj;
		if (osgiVersion == null) {
			if (other.osgiVersion != null)
				return false;
		} else if (!osgiVersion.equals(other.osgiVersion))
			return false;
		return true;
	}

}
