/*******************************************************************************
 * Copyright (c) 2010 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package bndtools.diff;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class PackageInfo implements Comparable<PackageInfo> {

	public static final int CHANGE_CODE_NONE = 0;
	public static final int CHANGE_CODE_NEW = 1;
	public static final int CHANGE_CODE_MODIFIED = 2;
	public static final int CHANGE_CODE_REMOVED = 3;
	public static final int CHANGE_CODE_VERSION_MISSING = 4;
	
	
	private String versionRange;
	private String suggestedVersionRange;
	private final String packageName;
	private String currentVersion;
	private int severity;
//	private String suggestedVersion;
	private int changeCode = CHANGE_CODE_NONE;
	private Set<ClassInfo> classes = new TreeSet<ClassInfo>();
	private JarDiff jarDiff;
	private boolean imported;
	private boolean exported;
	private String selectedVersion;
	private TreeSet<String> suggestedVersions;
	
	public PackageInfo(JarDiff jarDiff, String packageName) {
		this.jarDiff = jarDiff;
		this.packageName = packageName;
		suggestedVersions = new TreeSet<String>();
	}

	public String getPackageName() {
		return packageName;
	}
	
	public Set<ClassInfo> getClasses() {
		if (classes == null) {
			return Collections.emptySet();
		}
		return classes;
	}
	
	public Set<ClassInfo> getChangedClasses() {
		if (classes == null) {
			return Collections.emptySet();
		}
		Set<ClassInfo> ret = new TreeSet<ClassInfo>();
		for (ClassInfo ci : classes) {
			if (ci.getChangeCode() == ClassInfo.CHANGE_CODE_NONE) {
				continue;
			}
			ret.add(ci);
		}
		return ret;
	}

	public void setClasses(Set<ClassInfo> classes) {
		this.classes = classes;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((packageName == null) ? 0 : packageName.hashCode());
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
		PackageInfo other = (PackageInfo) obj;
		if (packageName == null) {
			if (other.packageName != null)
				return false;
		} else if (!packageName.equals(other.packageName)) {
			return false;
		}
		
		return true;
	}

	public String getCurrentVersion() {
		return currentVersion;
	}

	public void setCurrentVersion(String version) {
		this.currentVersion = version;
	}

	public int compareTo(PackageInfo o) {
		return packageName.compareTo(o.packageName);
	}

	public int getSeverity() {
		return severity;
	}

	public void setSeverity(int severity) {
		this.severity = severity;
	}

	public String getSelectedVersion() {
		if (selectedVersion != null) {
			return selectedVersion;
		}
		return getSuggestedVersion();
	}

	public void setSelectedVersion(String selectedVersion) {
		this.selectedVersion = selectedVersion;
	}

	public int getChangeCode() {
		return changeCode;
	}

	public void setChangeCode(int changeCode) {
		this.changeCode = changeCode;
	}

	public JarDiff getJarDiff() {
		return jarDiff;
	}

	public boolean isImported() {
		return imported;
	}

	public boolean isExported() {
		return exported;
	}

	public void setExported(boolean exported) {
		this.exported = exported;
	}

	public void setImported(boolean imported) {
		this.imported = imported;
	}

	public String getVersionRange() {
		return versionRange;
	}

	public void setVersionRange(String versionRange) {
		this.versionRange = versionRange;
	}

	public String getSuggestedVersionRange() {
		return suggestedVersionRange;
	}

	public void setSuggestedVersionRange(String suggestedVersionRange) {
		this.suggestedVersionRange = suggestedVersionRange;
	}
	
	public void addSuggestedVersion(String version) {
		suggestedVersions.add(version);
	}
	
	public TreeSet<String> getSuggestedVersions() {
		return suggestedVersions;
	}
	
	public String getSuggestedVersion() {
		if (suggestedVersions.size() > 0) {
			return suggestedVersions.last();
		}
		return null;
	}
	
	public String toString() {
		return packageName + "[" + currentVersion + "]";
	}
}
