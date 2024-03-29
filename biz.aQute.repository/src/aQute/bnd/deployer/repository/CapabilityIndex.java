package aQute.bnd.deployer.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.osgi.resource.ResourceUtils;

/**
 * @ThreadSafe
 */
public class CapabilityIndex {

	private final Map<String, List<Capability>> capabilityMap = new HashMap<>();

	public synchronized void clear() {
		capabilityMap.clear();
	}

	public void addResource(Resource resource) {
		addCapabilities(resource.getCapabilities(null));
	}

	public void addCapability(Capability cap) {
		addCapabilities(Collections.singletonList(cap));
	}

	private synchronized void addCapabilities(Iterable<Capability> capabilities) {
		for (Capability cap : capabilities) {
			List<Capability> list = capabilityMap.get(cap.getNamespace());
			if (list == null) {
				list = new LinkedList<>();
				capabilityMap.put(cap.getNamespace(), list);
			}
			list.add(cap);
		}
	}

	public void appendMatchingCapabilities(Requirement requirement, Collection<Capability> capabilities) {
		List<Capability> caps;

		synchronized (this) {
			if (!capabilityMap.containsKey(requirement.getNamespace()))
				return;
			caps = new ArrayList<>(capabilityMap.get(requirement.getNamespace()));
		}

		caps.stream()
			.filter(ResourceUtils.filterMatcher(requirement))
			.forEachOrdered(capabilities::add);
	}

}
