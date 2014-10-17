package org.osgi.service.indexer.impl.types;

import org.osgi.framework.Constants;
import org.osgi.service.indexer.Namespaces;

public enum VersionKey {

	PackageVersion(Constants.VERSION_ATTRIBUTE), BundleVersion(Constants.BUNDLE_VERSION_ATTRIBUTE), NativeOsVersion(Namespaces.ATTR_NATIVE_OSVERSION);

	private String key;

	VersionKey(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}
}
