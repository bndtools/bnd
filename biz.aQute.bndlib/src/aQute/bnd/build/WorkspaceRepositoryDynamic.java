package aQute.bnd.build;

import java.util.Collection;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.lib.collections.MultiMap;

public class WorkspaceRepositoryDynamic extends BaseRepository implements Repository {

	final Workspace workspace;

	public WorkspaceRepositoryDynamic(Workspace workspace) {
		this.workspace = workspace;
	}

	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		MultiMap<Requirement, Capability> map = new MultiMap<>();

		for (Project project : workspace.getAllProjects()) {
			for (Resource resource : project.getResources()) {
				for (Requirement requirement : requirements) {
					for (Capability capability : resource.getCapabilities(requirement.getNamespace())) {
						if (ResourceUtils.matches(requirement, capability)) {
							map.add(requirement, capability);
						}
					}
				}
			}
		}
		return (Map) map;
	}

}
