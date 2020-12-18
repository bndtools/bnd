package aQute.bnd.build;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
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
		List<Capability> l = new ArrayList<>();
		String namespace = requirement.getNamespace();
		for (Resource r : resources) {
			for (Capability c : r.getCapabilities(namespace)) {
				if (ResourceUtils.matches(requirement, c)) {
					l.add(c);
				}
			}
		}
		return l;
	}

	@Override
	public String toString() {
		return NAME;
	}
}
