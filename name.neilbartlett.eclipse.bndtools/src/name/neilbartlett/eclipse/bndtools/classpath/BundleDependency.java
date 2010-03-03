/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package name.neilbartlett.eclipse.bndtools.classpath;

import aQute.libg.version.Version;
import aQute.libg.version.VersionRange;

public class BundleDependency implements Comparable<BundleDependency> {
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

	public int compareTo(BundleDependency other) {
		int diff = this.getSymbolicName().compareTo(other.getSymbolicName());
		if(diff == 0) {
			Version version1 = this.getVersionRange().getLow();
			if(version1 == null) version1 = new Version(0);
			Version version2 = other.getVersionRange().getLow();
			if(version2 == null) version2 = new Version(0);
			
			diff = version1.compareTo(version2);
		}
		return diff;
	}
}
