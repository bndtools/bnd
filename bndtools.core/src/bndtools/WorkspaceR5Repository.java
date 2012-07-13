package bndtools;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IProject;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.lib.deployer.repository.CapabilityIndex;

public class WorkspaceR5Repository implements Repository {

    private static final String NAME = "Workspace";

    private final Map<IProject,CapabilityIndex> projectMap = new HashMap<IProject,CapabilityIndex>();

    public void cleanProject(IProject project) {
        synchronized (projectMap) {
            CapabilityIndex index = projectMap.get(project);
            if (index != null)
                index.clear();
        }
    }

    public void addResource(IProject project, Resource resource) {
        synchronized (projectMap) {
            CapabilityIndex index = projectMap.get(project);
            if (index == null) {
                index = new CapabilityIndex();
                projectMap.put(project, index);
            }
            index.addResource(resource);
        }
    }

    public Map<Requirement,Collection<Capability>> findProviders(Collection< ? extends Requirement> requirements) {
        Map<Requirement,Collection<Capability>> result = new HashMap<Requirement,Collection<Capability>>();
        for (Requirement requirement : requirements) {
            List<Capability> matches = new LinkedList<Capability>();
            result.put(requirement, matches);

            for (Entry<IProject,CapabilityIndex> entry : projectMap.entrySet()) {
                CapabilityIndex capabilityIndex = entry.getValue();
                capabilityIndex.appendMatchingCapabilities(requirement, matches);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return NAME;
    }

}
