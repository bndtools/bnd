package aQute.bnd.osgi.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collector;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.service.resource.SupportingResource;

/**
 * A repository that contains a set of resources.
 */

public class ResourcesRepository extends BaseRepository {
	private final Set<Resource>									resources;
	private final Map<String, Predicate<Map<String, Object>>>	cache;

	/**
	 * Creates a new resources repository with an empty set of resources.
	 */
	public ResourcesRepository() {
		resources = new LinkedHashSet<>();
		cache = new ConcurrentHashMap<>();
	}

	/**
	 * Creates a new resources repository with a single resource.
	 *
	 * @param resource The resource to add to the repository.
	 */
	public ResourcesRepository(Resource resource) {
		this();
		add(resource);
	}

	/**
	 * Creates a new resources repository with a collection of resources.
	 *
	 * @param resources The resources to add to the repository.
	 */
	public ResourcesRepository(Collection<? extends Resource> resources) {
		this();
		addAll(resources);
	}

	/**
	 * Finds all the providers of the specified requirements in this repository.
	 *
	 * @param requirements The requirements to find providers for.
	 * @return A map of requirements to the providers that satisfy them.
	 */
	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		return ResourceUtils.findProviders(requirements, this::findProvider);
	}

	/**
	 * Finds the providers of the specified requirement in this repository.
	 *
	 * @param requirement The requirement to find providers for.
	 * @return A list of capabilities that satisfy the requirement.
	 */
	public List<Capability> findProvider(Requirement requirement) {
		String namespace = requirement.getNamespace();
		return resources.stream()
			.flatMap(resource -> ResourceUtils.capabilityStream(resource, namespace))
			.filter(ResourceUtils.matcher(requirement, this::filterPredicate))
			.collect(ResourceUtils.toCapabilities());
	}

	/**
	 * Gets the predicate that filters capabilities based on the specified
	 * filter string.
	 *
	 * @param filterString The filter string.
	 * @return A predicate that filters capabilities based on the specified
	 *         filter string.
	 */
	private Predicate<Map<String, Object>> filterPredicate(String filterString) {
		if (filterString == null) {
			return ResourceUtils.filterPredicate(null);
		}
		return cache.computeIfAbsent(filterString, ResourceUtils::filterPredicate);
	}

	/**
	 * Adds a resource to this repository.
	 *
	 * @param resource The resource to add.
	 */
	public void add(Resource resource) {
		if (resource != null) {
			resources.add(resource);
			if (resource instanceof SupportingResource cr)
				resources.addAll(cr.getSupportingResources());
		}
	}

	/**
	 * Adds a collection of resources to this repository.
	 *
	 * @param resources The resources to add.
	 */
	public void addAll(Collection<? extends Resource> resources) {
		resources.forEach(this::add);
	}

	/**
	 * Sets the resources in this repository to the specified collection of
	 * resources.
	 *
	 * @param resources The resources to set.
	 */
	protected void set(Collection<? extends Resource> resources) {
		this.resources.clear();
		addAll(resources);
	}

	/**
	 * Gets a list of all the resources in this repository.
	 *
	 * @return A list of all the resources in this repository.
	 */
	public List<Resource> getResources() {
		return new ArrayList<>(resources);
	}

	/**
	 * Returns a collector that accumulates capabilities into a list.
	 *
	 * @return A collector that accumulates capabilities into a list.
	 */
	public static Collector<Capability, List<Capability>, List<Capability>> toCapabilities() {
		return ResourceUtils.toCapabilities();
	}

	/**
	 * Returns a collector that accumulates resources into a resources
	 * repository.
	 *
	 * @return A collector that accumulates resources into a resources
	 *         repository.
	 */
	public static Collector<Resource, ResourcesRepository, ResourcesRepository> toResourcesRepository() {
		return Collector.of(ResourcesRepository::new, ResourcesRepository::add, ResourcesRepository::combiner);
	}

	/**
	 * Combines two resources repositories into one.
	 *
	 * @param t The first resources repository.
	 * @param u The second resources repository.
	 * @return A new resources repository containing all the resources from both
	 *         input repositories.
	 */
	private static ResourcesRepository combiner(ResourcesRepository t, ResourcesRepository u) {
		t.addAll(u.resources);
		return t;
	}

	@Override
	public String toString() {
		return resources.toString();
	}
}
