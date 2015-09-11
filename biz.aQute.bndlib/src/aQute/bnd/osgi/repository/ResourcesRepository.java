package aQute.bnd.osgi.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.lib.collections.MultiMap;

public class ResourcesRepository implements org.osgi.service.repository.Repository {
	final List<Resource> resources = new ArrayList<>();

	public ResourcesRepository(Resource resource) {
		add(resource);
	}

	public ResourcesRepository(Collection< ? extends Resource> resource) {
		addAll(resource);
	}

	public ResourcesRepository() {}

	@SuppressWarnings({
			"unchecked", "rawtypes"
	})
	@Override
	public Map<Requirement,Collection<Capability>> findProviders(Collection< ? extends Requirement> requirements) {
		MultiMap<Requirement,Capability> result = new MultiMap<>();

		for (Requirement requirement : requirements) {
			List<Capability> capabilities = findProvider(requirement);
			result.put(requirement, capabilities);
		}

		return (Map) result;
	}

	public List<Capability> findProvider(Requirement requirement) {
		List<Capability> result = new ArrayList<Capability>();
		String namespace = requirement.getNamespace();
		for (Resource resource : resources) {
			for (Capability capability : resource.getCapabilities(namespace))
				if (ResourceUtils.matches(requirement, capability))
					result.add(capability);
		}
		return result;
	}

	public void add(Resource resource) {
		resources.add(resource);
	}

	public void addAll(Collection< ? extends Resource> resource) {
		resources.addAll(resource);
	}

}
