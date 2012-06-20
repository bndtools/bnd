package aQute.lib.deployer.repository;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

class CapabilityIndex {
	
	private final Map<String,Map<Capability,URI>> map = new HashMap<String,Map<Capability,URI>>();

	public Map<Capability,URI> getCapabilities(String namespace) {
		return map.get(namespace);
	}
	
	public void addResource(Resource resource, URI baseUri) {
		List<Capability> capabilities = resource.getCapabilities(null);
		if (capabilities == null)
			return;
		
		for (Capability cap : capabilities) {
			Map<Capability,URI> uriMap = map.get(cap.getNamespace());
			if (uriMap == null) {
				uriMap = new LinkedHashMap<Capability,URI>();
				map.put(cap.getNamespace(), uriMap);
			}
			uriMap.put(cap, baseUri);
		}
	}
}
