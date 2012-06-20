package aQute.lib.deployer.repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

class CapabilityIndex {

	private final Map<String,List<Capability>>	capabilityMap	= new HashMap<String,List<Capability>>();

	public void clear() {
		capabilityMap.clear();
	}

	public void addResource(Resource resource) {
		List<Capability> capabilities = resource.getCapabilities(null);
		if (capabilities == null)
			return;

		for (Capability cap : capabilities) {
			List<Capability> list = capabilityMap.get(cap.getNamespace());
			if (list == null) {
				list = new LinkedList<Capability>();
				capabilityMap.put(cap.getNamespace(), list);
			}
			list.add(cap);
		}
	}

	public void appendMatchingCapabilities(Requirement requirement, Collection< ? super Capability> capabilities) {
		List<Capability> caps = capabilityMap.get(requirement.getNamespace());
		if (caps == null || caps.isEmpty())
			return;

		try {
			String filterStr = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
			Filter filter = filterStr != null ? FrameworkUtil.createFilter(filterStr) : null;

			for (Capability cap : caps) {
				boolean match;
				if (filter == null)
					match = true;
				else
					match = filter.match(new MapToDictionaryAdapter(cap.getAttributes()));

				if (match)
					capabilities.add(cap);
			}
		}
		catch (InvalidSyntaxException e) {
			// Assume no matches
		}
	}

}
