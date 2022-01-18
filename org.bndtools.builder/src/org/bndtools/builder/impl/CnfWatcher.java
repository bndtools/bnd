package org.bndtools.builder.impl;

import static org.bndtools.builder.impl.BuilderConstants.PLUGIN_ID;

import java.util.Collection;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.builder.BuildLoggerConstants;
import org.bndtools.utils.workspace.WorkspaceUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import bndtools.central.Central;

@Component(property = {
	"org.eclipse.core.resources.eventmask:Integer=" + IResourceChangeEvent.POST_CHANGE
})
public class CnfWatcher implements IResourceChangeListener {
	private static final ILogger	logger		= Logger.getLogger(CnfWatcher.class);
	static final org.slf4j.Logger	consoleLog	= org.slf4j.LoggerFactory.getLogger(CnfWatcher.class);

	@Reference
	BndtoolsBuilder					builder;

	@Reference
	IWorkspace						eclipseWorkspace;

	@Reference
	Workspace						bndWorkspace;

	@Override
	public void resourceChanged(final IResourceChangeEvent event) {
		if (Central.hasCnfWorkspace()) {
			processEvent(event);
		} else {
			Central.onCnfWorkspace(workspace -> processEvent(event));
		}
	}

	private void processEvent(IResourceChangeEvent event) {
		try {
			final IProject cnfProject = WorkspaceUtils.findCnfProject(eclipseWorkspace.getRoot(), bndWorkspace);
			if (cnfProject == null)
				return;

			IResourceDelta delta = event.getDelta();
			if (delta.findMember(cnfProject.getFullPath()) == null)
				return;

			Collection<Project> allProjects = bndWorkspace.getAllProjects();
			if (allProjects.isEmpty())
				return;

			Project p = allProjects.iterator()
				.next();
			DeltaWrapper dw = new DeltaWrapper(p, delta, new BuildLogger(BuildLoggerConstants.LOG_NONE, "", 0));
			if (dw.hasCnfChanged()) {
				WorkspaceJob j = new WorkspaceJob("Refreshing workspace for cnf change") {
					@Override
					public IStatus runInWorkspace(IProgressMonitor arg0) throws CoreException {
						try {
							bndWorkspace.clear();
							bndWorkspace.refresh();
							bndWorkspace.getPlugins();

							BndtoolsBuilder.dirty.addAll(allProjects);
							MarkerSupport ms = new MarkerSupport(cnfProject);
							ms.deleteMarkers("*");
							ms.setMarkers(bndWorkspace, BndtoolsConstants.MARKER_BND_WORKSPACE_PROBLEM);
						} catch (Exception e) {
							return new Status(IStatus.ERROR, PLUGIN_ID,
								"error during workspace refresh",
								e);
						}
						return Status.OK_STATUS;
					}
				};
				j.schedule();
			}
		} catch (Exception e) {
			logger.logError("Detecting changes in cnf failed, ignoring", e);
		}
	}
}
