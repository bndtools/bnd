package aQute.bnd.deployer.repository.aether;

import java.util.regex.*;

import aQute.bnd.version.*;

public class MvnVersion implements Comparable<MvnVersion> {
	
	private static final Pattern QUALIFIER = Pattern.compile("[-.]?([^0-9.].*)$");
	private static final Pattern RELEASE_ONLY_QUALIFIER = Pattern.compile("^[rR](0*[0-9]+)$");
	
	private static final String	QUALIFIER_SNAPSHOT	= "SNAPSHOT";

	private final Version osgiVersion;

	public MvnVersion(Version osgiVersion) {
		this.osgiVersion = osgiVersion;
	}
	
	public static final MvnVersion parseString(String versionStr) {
		MvnVersion result;
		
		try {
			// Special treatment for version strings consisting only
			// of release numbers, e.g. Google Guava r03 release
			// The release number will be parsed into the micro version number
			final Matcher relM = RELEASE_ONLY_QUALIFIER.matcher(versionStr);
			if (relM.find()) {
				final String relNumber = relM.group(1);
				final Version osgiVersion = new Version(0, 0, Integer.parseInt(relNumber));
				result = new MvnVersion(osgiVersion);
			} else {
				Matcher m = QUALIFIER.matcher(versionStr);
				if (!m.find()) {
					result = new MvnVersion(Version.parseVersion(versionStr));
				} else {
					String qualifier = m.group(1);

					Version v = Version.parseVersion(versionStr.substring(0,
							m.start()));
					Version osgiVersion = new Version(v.getMajor(), v.getMinor(),
							v.getMicro(), qualifier);
					result = new MvnVersion(osgiVersion);
				}
			}
		} catch (IllegalArgumentException e) { // bad format
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
