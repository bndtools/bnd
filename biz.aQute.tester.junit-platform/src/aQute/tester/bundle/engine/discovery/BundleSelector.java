package aQute.tester.bundle.engine.discovery;

import java.util.Objects;

import org.junit.platform.engine.DiscoverySelector;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

public class BundleSelector implements DiscoverySelector {
	private static final VersionRange	allRange	= new VersionRange(VersionRange.LEFT_CLOSED, Version.emptyVersion,
		null, VersionRange.RIGHT_OPEN);

	private final String				symbolicName;
	private final VersionRange			versionRange;

	private BundleSelector(String symbolicName, VersionRange versionRange) {
		this.symbolicName = symbolicName;
		this.versionRange = versionRange;
	}

	public String getSymbolicName() {
		return symbolicName;
	}

	public VersionRange getVersionRange() {
		return versionRange;
	}

	public static BundleSelector selectBundle(String symbolicName) {
		return new BundleSelector(symbolicName, allRange);
	}

	public static BundleSelector selectBundle(String symbolicName, String versionRange) {
		return new BundleSelector(symbolicName, VersionRange.valueOf(versionRange));
	}

	public static BundleSelector selectBundle(Bundle bundle) {
		return new BundleSelector(bundle.getSymbolicName(),
			new VersionRange(VersionRange.LEFT_CLOSED, bundle.getVersion(), bundle.getVersion(),
				VersionRange.RIGHT_CLOSED));
	}

	public boolean selects(Bundle bundle) {
		return Objects.equals(getSymbolicName(), bundle.getSymbolicName())
			&& getVersionRange().includes(bundle.getVersion());
	}

	@Override
	public String toString() {
		return "BundleSelector [symbolicName = '" + getSymbolicName() + "', versionRange = " + getVersionRange() + "]";
	}
}
