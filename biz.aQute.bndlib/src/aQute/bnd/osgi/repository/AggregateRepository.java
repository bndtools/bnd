package aQute.bnd.osgi.repository;

import static java.util.Collections.singleton;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;

import aQute.lib.collections.MultiMap;

public class AggregateRepository extends BaseRepository {

	private final Repository repositories[];

	public AggregateRepository(Collection<? extends Repository> repositories) {
		this(repositories.toArray(new Repository[0]));
	}

	public AggregateRepository(Repository... repositories) {
		this.repositories = new Repository[repositories.length];
		System.arraycopy(repositories, 0, this.repositories, 0, repositories.length);
	}

	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		MultiMap<Requirement, Capability> result = new MultiMap<>();

		for (Repository repository : repositories) {
			Map<Requirement, Collection<Capability>> capabilities = repository.findProviders(requirements);
			result.addAll(capabilities);
		}

		return (Map) result;
	}

	public Collection<Capability> findProviders(Requirement req) {
		if (req == null)
			return Collections.emptyList();

		Collection<Capability> capabilities = findProviders(singleton(req)).get(req);

		assert capabilities != null : "findProviders must return a map containing the collection";

		return capabilities;
	}

}
