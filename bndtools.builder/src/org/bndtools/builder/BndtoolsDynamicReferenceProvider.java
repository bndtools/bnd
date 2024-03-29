package org.bndtools.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IDynamicReferenceProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import bndtools.central.Central;

public class BndtoolsDynamicReferenceProvider implements IDynamicReferenceProvider {
	private static IWorkspaceRoot eclipse = ResourcesPlugin.getWorkspace()
		.getRoot();

	@Override
	public List<IProject> getDependentProjects(IBuildConfiguration buildConfiguration) throws CoreException {
		try {
			IProject project = buildConfiguration.getProject();
			Workspace ws = Central.getWorkspace();
			if (!ws.isDefaultWorkspace()) {
				return ws.readLocked(() -> getDependencies(project, ws));
			}
			return Collections.emptyList();
		} catch (Exception e) {
			Status status = new Status(Status.ERROR, "bndtools.builder", "Failed dependencies " + e.getMessage());
			throw new CoreException(status);
		}
	}

	private List<IProject> getDependencies(IProject project, Workspace ws) throws Exception {
		Project model = ws.getProject(project.getName());
		if (model != null) {
			List<IProject> result = new ArrayList<>();
			IProject cnf = eclipse.getProject(Workspace.CNFDIR);
			if (cnf != null)
				result.add(cnf);
			for (Project dep : model.getBuildDependencies()) {
				IProject idep = eclipse.getProject(dep.getName());
				if (idep != null && !dep.isCnf())
					result.add(idep);
			}
			return result;
		} else
			return Collections.emptyList();
	}
}
