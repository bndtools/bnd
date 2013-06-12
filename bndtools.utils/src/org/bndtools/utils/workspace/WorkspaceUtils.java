package org.bndtools.utils.workspace;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import aQute.bnd.build.Project;

public class WorkspaceUtils {

    public static IProject findOpenProject(IWorkspaceRoot wsroot, Project model) {
        return findOpenProject(wsroot, model.getName());
    }

    public static IProject findOpenProject(IWorkspaceRoot wsroot, String name) {
        IProject project = wsroot.getProject(name);
        if (project == null || !project.exists() || !project.isOpen())
            return null;
        return project;
    }

    public static IProject findCnfProject() throws Exception {
        return findOpenProject(ResourcesPlugin.getWorkspace().getRoot(), "cnf");
    }

}
