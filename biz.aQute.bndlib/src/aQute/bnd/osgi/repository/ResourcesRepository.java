package aQute.bnd.osgi.repository;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.osgi.resource.ResourceUtils;

public class ResourcesRepository extends BaseRepository {
	final Set<Resource> resources = new LinkedHashSet<>();

	public ResourcesRepository(Resource resource) {
		add(resource);
	}

	public ResourcesRepository(Collection< ? extends Resource> resource) {
		addAll(resource);
	}

	public ResourcesRepository() {}

	@Override
	public Map<Requirement,Collection<Capability>> findProviders(Collection< ? extends Requirement> requirements) {
		return requirements.stream().collect(toMap(identity(), this::findProvider, (u, v) -> v));
	}

	public List<Capability> findProvider(Requirement requirement) {
		String namespace = requirement.getNamespace();
		return resources.stream()
				.flatMap(resource -> resource.getCapabilities(namespace).stream())
				.filter(capability -> ResourceUtils.matches(requirement, capability))
				.collect(toList());
	}

	public void add(Resource resource) {
		if (resource != null) {
			resources.add(resource);
		}
	}

	public void addAll(Collection< ? extends Resource> resources) {
		resources.forEach(this::add);
	}

	protected void set(Collection< ? extends Resource> resources) {
		this.resources.clear();
		addAll(resources);
	}

	public List<Resource> getResources() {
		return new ArrayList<>(resources);
	}

	public static Collector<Resource,ResourcesRepository,ResourcesRepository> toResourcesRepository() {
		return Collector.of(ResourcesRepository::new, ResourcesRepository::add, ResourcesRepository::combiner);
	}

	private static ResourcesRepository combiner(ResourcesRepository t, ResourcesRepository u) {
		t.addAll(u.resources);
		return t;
	}
}
