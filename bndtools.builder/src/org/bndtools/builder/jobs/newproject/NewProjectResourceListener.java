package org.bndtools.builder.jobs.newproject;

import java.util.LinkedList;
import java.util.List;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;

public class NewProjectResourceListener implements IResourceChangeListener {
	private static final ILogger logger = Logger.getLogger(NewProjectResourceListener.class);

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		if (delta == null)
			return;

		final List<IProject> newProjects = new LinkedList<>();
		try {
			delta.accept(d -> {
				if (d.getFlags() == IResourceDelta.MARKERS)
					return false; // ignore marker-only deltas.

				IResource resource = d.getResource();
				if (resource.getType() == IResource.ROOT)
					return true;

				if (resource.getType() == IResource.PROJECT) {
					IProject project = (IProject) resource;
					if (project.isOpen() && project.hasNature(BndtoolsConstants.NATURE_ID)
						&& ((d.getKind() & IResourceDelta.ADDED) != 0)) {
						newProjects.add(project);
					}
				}

				return false;
			});

			if (!newProjects.isEmpty()) {
				AdjustClasspathsForNewProjectJob adjustClasspathsJob = new AdjustClasspathsForNewProjectJob(
					newProjects);
				adjustClasspathsJob.setSystem(true);
				adjustClasspathsJob.schedule();
			}
		} catch (CoreException e) {
			logger.logError("An error occurred while analysing the resource change", e);
		}
	}

}
