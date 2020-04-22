package aQute.bnd.osgi.repository;

import static aQute.bnd.stream.MapStream.toMap;
import static java.util.Collections.singleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;

import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.stream.MapStream;

public class AggregateRepository extends BaseRepository {
	private final List<Repository> repositories;

	public AggregateRepository(Collection<? extends Repository> repositories) {
		this.repositories = new ArrayList<>(repositories);
	}

	public AggregateRepository(Repository... repositories) {
		this.repositories = new ArrayList<>();
		Collections.addAll(this.repositories, repositories);
	}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		Map<Requirement, Collection<Capability>> result = MapStream.of(repositories.stream()
			.flatMap(repository -> repository.findProviders(requirements)
				.entrySet()
				.stream()))
			.collect(toMap(ResourceUtils::capabilitiesCombiner));
		return result;
	}

	public Collection<Capability> findProviders(Requirement req) {
		if (req == null) {
			return new ArrayList<>();
		}

		Collection<Capability> capabilities = findProviders(singleton(req)).get(req);

		assert capabilities != null : "findProviders must return a map containing the collection";

		return capabilities;
	}
}
