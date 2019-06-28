package org.bndtools.utils.workspace;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;

public class WorkspaceUtils {

	public static IProject findOpenProject(IWorkspaceRoot wsroot, Project model) {
		return findOpenProject(wsroot, model.getName());
	}

	public static IProject findOpenProject(IWorkspaceRoot wsroot, String name) {
		IProject project = wsroot.getProject(name);
		if (checkProject(project, name)) {
			return project;
		}
		for (IProject p : wsroot.getProjects()) {
			if (checkProject(p, name)) {
				return p;
			}
		}
		return null;
	}

	public static IProject findCnfProject(IWorkspaceRoot wsroot, Workspace ws) throws Exception {
		return findOpenProject(wsroot, ws.getBuildDir()
			.getName());
	}

	private static boolean checkProject(IProject project, String name) {
		if ((project != null) && project.isOpen()) {
			IPath path = project.getLocation();
			if ((path != null) && name.equals(path.lastSegment())) {
				return true;
			}
		}
		return false;
	}
}
