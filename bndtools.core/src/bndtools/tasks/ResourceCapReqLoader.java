package bndtools.tasks;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bndtools.utils.resources.ResourceUtils;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import bndtools.model.resolution.RequirementWrapper;

public class ResourceCapReqLoader implements CapReqLoader {

	private final Resource	resource;
	private final String	name;
	private final URI		uri;

	public ResourceCapReqLoader(Resource resource) {
		this.resource = resource;
		this.name = ResourceUtils.getIdentity(resource);
		this.uri = ResourceUtils.getURI(ResourceUtils.getContentCapability(resource));
	}

	@Override
	public String getShortLabel() {
		return name;
	}

	@Override
	public String getLongLabel() {
		return name + "[" + uri + "]";
	}

	@Override
	public Map<String, List<Capability>> loadCapabilities() throws Exception {
		Map<String, List<Capability>> result = new HashMap<>();

		List<Capability> caps = resource.getCapabilities(null);
		for (Capability cap : caps) {
			String ns = cap.getNamespace();
			List<Capability> listForNamespace = result.get(ns);
			if (listForNamespace == null) {
				listForNamespace = new LinkedList<>();
				result.put(ns, listForNamespace);
			}
			listForNamespace.add(cap);
		}

		return result;
	}

	@Override
	public Map<String, List<RequirementWrapper>> loadRequirements() throws Exception {
		Map<String, List<RequirementWrapper>> result = new HashMap<>();

		List<Requirement> reqs = resource.getRequirements(null);
		for (Requirement req : reqs) {
			String ns = req.getNamespace();
			List<RequirementWrapper> listForNamespace = result.get(ns);
			if (listForNamespace == null) {
				listForNamespace = new LinkedList<>();
				result.put(ns, listForNamespace);
			}
			RequirementWrapper wrapper = new RequirementWrapper();
			wrapper.requirement = req;
			listForNamespace.add(wrapper);
		}

		return result;
	}

	@Override
	public void close() throws IOException {
		// no-op
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((resource == null) ? 0 : resource.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResourceCapReqLoader other = (ResourceCapReqLoader) obj;
		if (resource == null) {
			if (other.resource != null)
				return false;
		} else if (!resource.equals(other.resource))
			return false;
		return true;
	}

}
