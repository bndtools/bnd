package aQute.bnd.build;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

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

		Map<Requirement, Collection<Capability>> result = ResourceUtils.findProviders(requirements,
			requirement -> findProvider(resources, requirement));
		return result;
	}

	private List<Capability> findProvider(Collection<? extends Resource> resources, Requirement requirement) {
		List<Capability> capabilities = new ArrayList<>();
		String namespace = requirement.getNamespace();
		Predicate<Capability> matcher = ResourceUtils.matcher(requirement);
		for (Resource resource : resources) {
			for (Capability capability : resource.getCapabilities(namespace)) {
				if (matcher.test(capability)) {
					capabilities.add(capability);
				}
			}
		}
		return capabilities;
	}

	@Override
	public String toString() {
		return NAME;
	}
}
