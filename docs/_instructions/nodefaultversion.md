---
layout: default
class: Builder
title: -nodefaultversion  BOOLEAN
summary:  Do not add a default version to exported packages when no version is present. 
---

	public void cleanupVersion(Packages packages, String defaultVersion) {
		if (defaultVersion != null) {
			Matcher m = Verifier.VERSION.matcher(defaultVersion);
			if (m.matches()) {
				// Strip qualifier from default package version
				defaultVersion = Version.parseVersion(defaultVersion).getWithoutQualifier().toString();
			}
		}
		for (Map.Entry<PackageRef,Attrs> entry : packages.entrySet()) {
			Attrs attributes = entry.getValue();
			String v = attributes.get(Constants.VERSION_ATTRIBUTE);
			if (v == null && defaultVersion != null) {
				if (!isTrue(getProperty(Constants.NODEFAULTVERSION))) {
					v = defaultVersion;
					if (isPedantic())
						warning("Used bundle version %s for exported package %s", v, entry.getKey());
				} else {
					if (isPedantic())
						warning("No export version for exported package %s", entry.getKey());
				}
			}
			if (v != null)
				attributes.put(Constants.VERSION_ATTRIBUTE, cleanupVersion(v));
		}
	}
