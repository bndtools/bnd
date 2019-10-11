package aQute.tester.bundle.engine.discovery;

import org.junit.platform.engine.DiscoverySelector;
import org.osgi.framework.Bundle;
import org.osgi.framework.VersionRange;

public class BundleSelector implements DiscoverySelector {

	private final String		symbolicName;
	private final VersionRange	version;

	private BundleSelector(String symbolicName, VersionRange version) {
		this.symbolicName = symbolicName;
		this.version = version;
	}

	public String getSymbolicName() {
		return symbolicName;
	}

	public VersionRange getVersion() {
		return version;
	}

	public static BundleSelector selectBundle(String bsn) {
		return selectBundle(bsn, "0");
	}

	public static BundleSelector selectBundle(String bsn, String versionRange) {
		return new BundleSelector(bsn, VersionRange.valueOf(versionRange));
	}

	public static BundleSelector selectBundle(Bundle bundle) {
		return new BundleSelector(bundle.getSymbolicName(),
			new VersionRange('[', bundle.getVersion(), bundle.getVersion(), ']'));
	}
}
