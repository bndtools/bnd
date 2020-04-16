package aQute.bnd.osgi.repository;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.osgi.resource.ResourceUtils;

public class ResourcesRepository extends BaseRepository {
	private final Set<Resource>			resources	= new LinkedHashSet<>();
	private final Map<String, Predicate<Map<String, Object>>>	cache		= new ConcurrentHashMap<>();

	public ResourcesRepository(Resource resource) {
		add(resource);
	}

	public ResourcesRepository(Collection<? extends Resource> resource) {
		addAll(resource);
	}

	public ResourcesRepository() {}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		return requirements.stream()
			.collect(toMap(identity(), this::findProvider, ResourcesRepository::merger));
	}

	public List<Capability> findProvider(Requirement requirement) {

		String namespace = requirement.getNamespace();
		return resources.stream()
			.flatMap(resource -> ResourceUtils.capabilityStream(resource, namespace))
			.filter(ResourceUtils.matcher(requirement, this::filterPredicate))
			.collect(ResourceUtils.toCapabilities());
	}

	private Predicate<Map<String, Object>> filterPredicate(String filterString) {
		if (filterString == null) {
			return ResourceUtils.filterPredicate(null);
		}
		return cache.computeIfAbsent(filterString, ResourceUtils::filterPredicate);
	}

	public Stream<Capability> getCapabilitiesAsStream(String namespace) {
		return resources.stream()
			.flatMap(r -> r.getCapabilities(namespace)
				.stream());
	}

	public void add(Resource resource) {
		if (resource != null) {
			resources.add(resource);
		}
	}

	public void addAll(Collection<? extends Resource> resources) {
		resources.forEach(this::add);
	}

	protected void set(Collection<? extends Resource> resources) {
		this.resources.clear();
		addAll(resources);
	}

	public List<Resource> getResources() {
		return new ArrayList<>(resources);
	}

	public static Collector<Capability, List<Capability>, List<Capability>> toCapabilities() {
		return ResourceUtils.toCapabilities();
	}

	private static <E, C extends Collection<E>> C merger(C t, C u) {
		u.removeAll(t);
		t.addAll(u);
		return t;
	}

	public static Collector<Resource, ResourcesRepository, ResourcesRepository> toResourcesRepository() {
		return Collector.of(ResourcesRepository::new, ResourcesRepository::add, ResourcesRepository::combiner);
	}

	private static ResourcesRepository combiner(ResourcesRepository t, ResourcesRepository u) {
		t.addAll(u.resources);
		return t;
	}
}
