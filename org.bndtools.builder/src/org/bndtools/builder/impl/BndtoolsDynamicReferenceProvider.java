package org.bndtools.builder.impl;

import static org.bndtools.builder.impl.BuilderConstants.PLUGIN_ID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IDynamicReferenceProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;

@Component
public class BndtoolsDynamicReferenceProvider implements IDynamicReferenceProvider {

	final Workspace			bndWS;
	final IWorkspaceRoot	wsRoot;

	@Activate
	public BndtoolsDynamicReferenceProvider(@Reference
	Workspace bndWS, @Reference
	IWorkspace eclipseWS) {
		this.bndWS = bndWS;
		this.wsRoot = eclipseWS.getRoot();
	}

	@Override
	public List<IProject> getDependentProjects(IBuildConfiguration buildConfiguration) throws CoreException {
		try {
			IProject project = buildConfiguration.getProject();
			return bndWS.readLocked(() -> getDependencies(project));
		} catch (Exception e) {
			Status status = new Status(Status.ERROR, PLUGIN_ID, "Failed dependencies " + e.getMessage(), e);
			throw new CoreException(status);
		}
	}

	private List<IProject> getDependencies(IProject project) throws Exception {
		Project model = bndWS.getProject(project.getName());
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
			}
		}
		return Collections.emptyList();
	}
}
