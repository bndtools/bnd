package bndtools.editor.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bndtools.api.BndProjectResource;
import org.bndtools.api.IBndProject;
import org.eclipse.core.resources.IProject;

public class BndProject implements IBndProject {

    private final IProject project;

    private Map<String,BndProjectResource> resources;

    public BndProject(IProject project) {
        this.project = project;
    }

    public String getProjectName() {
        return project.getName();
    }

    public void addResource(String fullPath, BndProjectResource bndProjectResource) {
        if (resources == null) {
            resources = new LinkedHashMap<String,BndProjectResource>();
        }
        resources.put(fullPath, bndProjectResource);
    }

    public Map<String,BndProjectResource> getResources() {
        if (resources == null) {
            return Collections.emptyMap();
        }
        return resources;
    }
}
