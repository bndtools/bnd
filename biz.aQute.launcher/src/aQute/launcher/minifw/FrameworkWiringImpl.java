package aQute.launcher.minifw;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Requirement;

class FrameworkWiringImpl implements FrameworkWiring {
	private final Framework framework;

	FrameworkWiringImpl(Framework framework) {
		this.framework = framework;
	}

	@Override
	public Bundle getBundle() {
		return framework;
	}

	@Override
	public void refreshBundles(Collection<Bundle> bundles, FrameworkListener... listeners) {
		if (listeners != null) {
			FrameworkEvent event = new FrameworkEvent(FrameworkEvent.PACKAGES_REFRESHED, getBundle(), null);
			for (FrameworkListener listener : listeners) {
				listener.frameworkEvent(event);
			}
		}
	}

	@Override
	public boolean resolveBundles(Collection<Bundle> bundles) {
		return true;
	}

	@Override
	public Collection<Bundle> getRemovalPendingBundles() {
		return Collections.emptyList();
	}

	@Override
	public Collection<Bundle> getDependencyClosure(Collection<Bundle> bundles) {
		return new ArrayList<>(bundles);
	}

	@Override
	public Collection<BundleCapability> findProviders(Requirement requirement) {
		return Collections.emptyList();
	}
}
