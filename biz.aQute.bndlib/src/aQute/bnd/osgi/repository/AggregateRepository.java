package aQute.bnd.osgi.repository;

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

public class AggregateRepository extends BaseRepository {
	private final List<Repository> repositories;

	public AggregateRepository(Collection<? extends Repository> repositories) {
		this.repositories = new ArrayList<>(repositories);
	}

	public AggregateRepository(Repository... repositories) {
		this.repositories = new ArrayList<>(repositories.length);
		Collections.addAll(this.repositories, repositories);
	}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		Map<Requirement, Collection<Capability>> result = ResourceUtils.findProviders(requirements,
			this::findProviders);
		return result;
	}

	public Collection<Capability> findProviders(Requirement requirement) {
		Collection<Requirement> requirements = singleton(requirement);
		Collection<Capability> capabilities = repositories.stream()
			.map(repository -> repository.findProviders(requirements)
				.get(requirement))
			.reduce(new ArrayList<>(), ResourceUtils::capabilitiesCombiner);
		return capabilities;
	}
}
