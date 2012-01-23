package org.osgi.service.bindex.impl;

import org.osgi.framework.Constants;

enum VersionKey {

	PackageVersion(Constants.VERSION_ATTRIBUTE), BundleVersion(Constants.BUNDLE_VERSION_ATTRIBUTE);

	private String key;

	VersionKey(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}
}
