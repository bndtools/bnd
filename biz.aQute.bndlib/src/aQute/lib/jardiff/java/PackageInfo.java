/*******************************************************************************
 * Copyright (c) 2011 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package aQute.lib.jardiff.java;

import java.util.*;

import aQute.lib.jardiff.*;
import aQute.libg.version.*;

public class PackageInfo implements PackageDiff, Comparable<PackageInfo> {

	private VersionRange oldVersionRange;
	private VersionRange suggestedVersionRange;
	private VersionRange newVersionRange;
	private final String packageName;
	private Version oldVersion;
	private PackageSeverity severity = PackageSeverity.NONE;
	private Delta delta = Delta.UNCHANGED;
	private Set<ClassInfo> classes = new TreeSet<ClassInfo>();
	private JavaDiff javaDiff;
	private boolean imported;
	private boolean exported;
	private Version newVersion;
	private TreeSet<Version> suggestedVersions;
	
	public PackageInfo(JavaDiff javaDiff, String packageName) {
		this.javaDiff = javaDiff;
		this.packageName = packageName;
		suggestedVersions = new TreeSet<Version>();
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
			if (ci.getDelta() == Delta.UNCHANGED) {
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

	public int compareTo(PackageInfo o) {
		return packageName.compareTo(o.packageName);
	}

	public PackageSeverity getSeverity() {
		return severity;
	}

	public void setSeverity(PackageSeverity severity) {
		this.severity = severity;
	}

	public Version getNewVersion() {
		if (newVersion != null) {
			return newVersion;
		}
		return getSuggestedVersion();
	}

	public void setNewVersion(Version version) {
		this.newVersion = version;
	}

	public Delta getDelta() {
		return delta;
	}

	public void setDelta(Delta delta) {
		this.delta = delta;
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

	public void setOldVersionRange(VersionRange versionRange) {
		this.oldVersionRange = versionRange;
	}

	public VersionRange getSuggestedVersionRange() {
		return suggestedVersionRange;
	}

	public void setSuggestedVersionRange(VersionRange suggestedVersionRange) {
		this.suggestedVersionRange = suggestedVersionRange;
	}
	
	public void addSuggestedVersion(Version version) {
		suggestedVersions.add(version);
	}
	
	public TreeSet<Version> getSuggestedVersions() {
		return suggestedVersions;
	}
	
	public Version getSuggestedVersion() {
		if (suggestedVersions.size() > 0) {
			return suggestedVersions.last();
		}
		return null;
	}
	
	public String toString() {
		return packageName + "[" + oldVersion + "]";
	}

	public String getName() {
		return packageName;
	}

	public Diff getContainer() {
		return javaDiff;
	}

	public Collection<? extends Diff> getContained() {
		return classes;
	}

	public String explain() {
		StringBuilder sb = new StringBuilder();
		sb.append(getDelta());
		sb.append(' ');
		
		sb.append(getPackageName());
		sb.append(' ');
		if (getDelta() != Delta.ADDED) {
			sb.append(getOldVersion());
		}
		sb.append("   : ");
		sb.append(JavaDiff.getSeverityText(getSeverity()));
		if (getDelta() == Delta.ADDED || getDelta() == Delta.MODIFIED) {
			sb.append((getSuggestedVersion() != null ? " -> Suggested version: " + getSuggestedVersion() : ""));
		}
		return sb.toString();
	}

	public void setOldVersion(Version version) {
		this.oldVersion = version;
	}
	
	public Version getOldVersion() {
		return oldVersion;
	}

	public VersionRange getOldVersionRange() {
		return oldVersionRange;
	}

	public void setNewVersionRange(VersionRange version) {
		this.newVersionRange = version;
	}

	public VersionRange getNewVersionRange() {
		return newVersionRange;
	}

	public void calculateVersions() {
		PackageSeverity highestSeverity = PackageSeverity.NONE;
		
		if (isExported()) {
			if (getDelta() == Delta.MODIFIED) { 
				String ver = JavaDiff.getVersionString(javaDiff.jarDiff.getNewJar(), getPackageName());
				Version version = null;
				if (ver != null) {
					version = Version.parseVersion(JavaDiff.getVersionString(javaDiff.jarDiff.getNewJar(), getPackageName()));
				} else {
					version = getOldVersion();
				}
				String mask;
				if (getSeverity().value() > highestSeverity.value()) {
					highestSeverity = getSeverity();
				}
				switch(getSeverity()) {
				case MINOR :
					mask = "=+0";
					break;
				case MAJOR :
					mask = "+00";
					break;
				default:
					mask = null;
				}
				if (mask != null) {
					String suggestedVersion = JavaDiff._version(new String[] { "", mask, version.toString()});
					addSuggestedVersion(Version.parseVersion(suggestedVersion));
				} else {
					addSuggestedVersion(version);
				}
			} else if (getDelta() == Delta.ADDED) {
				
				Version highestVersion = javaDiff. new HighestVersion();
				
				// Obey packageinfo if it exist
				String ver = JavaDiff.getVersionString(javaDiff.jarDiff.getNewJar(), getPackageName());
				if (ver != null) {
					Version version = Version.parseVersion(JavaDiff.getVersionString(javaDiff.jarDiff.getNewJar(), getPackageName()));
					addSuggestedVersion(version);
					if (JarDiff.suggestVersionOne(version)) {
						addSuggestedVersion(JarDiff.VERSION_ONE);
					}
				} else {
					if (getSuggestedVersion() == null || Version.LOWEST.equals(getSuggestedVersion())) {
						addSuggestedVersion(highestVersion);
					}
					addSuggestedVersion(JarDiff.VERSION_ONE);
				}
			}
		}
		
		if (isImported() && getDelta() == Delta.MODIFIED) {
			String mask;
			if (getSeverity().value() > highestSeverity.value()) {
				highestSeverity = getSeverity();
			}
			switch(getSeverity()) {
			case MINOR :
				mask = "=+0";
				break;
			case MAJOR :
				mask = "+00";
				break;
			default:
				mask = null;
			}
			if (mask != null) {
				String suggestedVersion = "[" + JavaDiff._version(new String[] { "", mask, oldVersion.toString()}) + "]";
				addSuggestedVersion(Version.parseVersion(suggestedVersion));
			} else {
				addSuggestedVersion(oldVersion);
			}

		}
	}
}
