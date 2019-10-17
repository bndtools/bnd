package aQute.launcher.minifw;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Wire;

class BundleWiringImpl implements BundleWiring {
	private final Bundle		bundle;
	private final ClassLoader		classLoader;

	BundleWiringImpl(Bundle bundle, ClassLoader classLoader) {
		this.bundle = bundle;
		this.classLoader = classLoader;
	}

	@Override
	public Bundle getBundle() {
		return bundle;
	}

	@Override
	public boolean isCurrent() {
		return true;
	}

	@Override
	public boolean isInUse() {
		return true;
	}

	@Override
	public List<BundleCapability> getCapabilities(String namespace) {
		return Collections.emptyList();
	}

	@Override
	public List<BundleRequirement> getRequirements(String namespace) {
		return Collections.emptyList();
	}

	@Override
	public List<BundleWire> getProvidedWires(String namespace) {
		return Collections.emptyList();
	}

	@Override
	public List<BundleWire> getRequiredWires(String namespace) {
		return Collections.emptyList();
	}

	@Override
	public BundleRevision getRevision() {
		return bundle.adapt(BundleRevision.class);
	}

	@Override
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	@Override
	public List<URL> findEntries(String path, String filePattern, int options) {
		return Collections
			.list(bundle.findEntries(path, filePattern, (options & BundleWiring.FINDENTRIES_RECURSE) != 0));
	}

	@Override
	public Collection<String> listResources(String path, String filePattern, int options) {
		return Collections.emptyList();
	}

	@Override
	public List<Capability> getResourceCapabilities(String namespace) {
		return Collections.emptyList();
	}

	@Override
	public List<Requirement> getResourceRequirements(String namespace) {
		return Collections.emptyList();
	}

	@Override
	public List<Wire> getProvidedResourceWires(String namespace) {
		return Collections.emptyList();
	}

	@Override
	public List<Wire> getRequiredResourceWires(String namespace) {
		return Collections.emptyList();
	}

	@Override
	public BundleRevision getResource() {
		return getRevision();
	}
}
