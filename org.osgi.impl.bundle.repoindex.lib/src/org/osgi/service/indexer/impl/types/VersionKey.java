package org.osgi.service.indexer.impl.types;

import org.osgi.framework.Constants;

public enum VersionKey {

	PackageVersion(Constants.VERSION_ATTRIBUTE), BundleVersion(Constants.BUNDLE_VERSION_ATTRIBUTE);

	private String key;

	VersionKey(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}
}
