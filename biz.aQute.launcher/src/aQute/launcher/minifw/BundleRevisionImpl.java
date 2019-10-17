package aQute.launcher.minifw;

import java.util.Collections;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

class BundleRevisionImpl implements BundleRevision {
	private final Bundle bundle;

	BundleRevisionImpl(Bundle bundle) {
		this.bundle = bundle;
	}

	@Override
	public Bundle getBundle() {
		return bundle;
	}

	@Override
	public BundleWiring getWiring() {
		return bundle.adapt(BundleWiring.class);
	}

	@Override
	public Version getVersion() {
		return bundle.getVersion();
	}

	@Override
	public int getTypes() {
		return (bundle.getHeaders()
			.get(Constants.FRAGMENT_HOST) != null) ? BundleRevision.TYPE_FRAGMENT : 0;
	}

	@Override
	public String getSymbolicName() {
		return bundle.getSymbolicName();
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		return Collections.emptyList();
	}

	@Override
	public List<BundleRequirement> getDeclaredRequirements(String namespace) {
		return Collections.emptyList();
	}

	@Override
	public List<BundleCapability> getDeclaredCapabilities(String namespace) {
		return Collections.emptyList();
	}

	@Override
	public List<Capability> getCapabilities(String namespace) {
		return Collections.emptyList();
	}
}
