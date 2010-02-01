package name.neilbartlett.eclipse.bndtools.classpath;

import aQute.libg.version.VersionRange;

public class BundleDependency {
	private final VersionRange versionRange;
	private final String symbolicName;

	public BundleDependency(String symbolicName, VersionRange versionRange) {
		this.symbolicName = symbolicName;
		this.versionRange = versionRange;
	}

	public VersionRange getVersionRange() {
		return versionRange;
	}

	public String getSymbolicName() {
		return symbolicName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((symbolicName == null) ? 0 : symbolicName.hashCode());
		result = prime * result
				+ ((versionRange == null) ? 0 : versionRange.toString().hashCode());
		return result;
	}
	
	@Override
	public String toString() {
		return "BundleDependency [symbolicName=" + symbolicName
				+ ", versionRange=" + versionRange + "]";
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BundleDependency other = (BundleDependency) obj;
		if (symbolicName == null) {
			if (other.symbolicName != null)
				return false;
		} else if (!symbolicName.equals(other.symbolicName))
			return false;
		if (versionRange == null) {
			if (other.versionRange != null)
				return false;
		} else if (!versionRange.toString().equals(other.versionRange != null ? other.versionRange.toString() : null))
			return false;
		return true;
	}
	
	
	

}
