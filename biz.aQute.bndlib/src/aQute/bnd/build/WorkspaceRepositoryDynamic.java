package aQute.bnd.build;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.osgi.repository.WorkspaceRepositoryMarker;
import aQute.bnd.osgi.resource.ResourceUtils;

class WorkspaceRepositoryDynamic extends BaseRepository implements Repository, WorkspaceRepositoryMarker {
	private final Workspace workspace;

	WorkspaceRepositoryDynamic(Workspace workspace) {
		this.workspace = workspace;
	}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		List<Resource> resources = workspace.getAllProjects()
			.stream()
			.map(Project::getResources)
			.flatMap(Collection::stream)
			.collect(toList());

		Map<Requirement, Collection<Capability>> result = requirements.stream()
			.collect(toMap(identity(), requirement -> findProvider(resources, requirement),
				ResourceUtils::capabilitiesCombiner));
		return result;
	}

	private List<Capability> findProvider(Collection<? extends Resource> resources, Requirement requirement) {
		String namespace = requirement.getNamespace();
		return resources.stream()
			.flatMap(resource -> ResourceUtils.capabilityStream(resource, namespace))
			.filter(ResourceUtils.matcher(requirement))
			.collect(ResourceUtils.toCapabilities());
	}

	@Override
	public String toString() {
		return NAME;
	}
}
