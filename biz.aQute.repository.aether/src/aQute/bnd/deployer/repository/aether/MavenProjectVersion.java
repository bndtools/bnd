package aQute.bnd.deployer.repository.aether;

import aQute.bnd.version.*;

public class MavenProjectVersion implements Comparable<MavenProjectVersion> {
	
	private static final String	QUALIFIER_SNAPSHOT	= "SNAPSHOT";

	private final Version osgiVersion;

	public MavenProjectVersion(Version osgiVersion) {
		this.osgiVersion = osgiVersion;
	}
	
	public static final MavenProjectVersion parseString(String versionStr) {
		MavenProjectVersion result;
		
		int dashIndex = versionStr.indexOf('-');
		if (dashIndex < 0) {
			result = new MavenProjectVersion(Version.parseVersion(versionStr));
		} else {
			String qualifier = versionStr.substring(dashIndex + 1);

			Version v = Version.parseVersion(versionStr.substring(0, dashIndex));
			Version osgiVersion = new Version(v.getMajor(), v.getMinor(), v.getMicro(), qualifier);
			result = new MavenProjectVersion(osgiVersion);
		}
		return result;
	}
	
	public Version getOSGiVersion() {
		return osgiVersion;
	}
	
	public boolean isSnapshot() {
		return QUALIFIER_SNAPSHOT.equals(osgiVersion.getQualifier());
	}
	
	public int compareTo(MavenProjectVersion other) {
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
		MavenProjectVersion other = (MavenProjectVersion) obj;
		if (osgiVersion == null) {
			if (other.osgiVersion != null)
				return false;
		} else if (!osgiVersion.equals(other.osgiVersion))
			return false;
		return true;
	}

}
