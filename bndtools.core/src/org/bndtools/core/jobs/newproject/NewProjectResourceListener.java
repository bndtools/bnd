package org.bndtools.core.jobs.newproject;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

import bndtools.Plugin;
import bndtools.builder.BndProjectNature;

public class NewProjectResourceListener implements IResourceChangeListener {

    public void resourceChanged(IResourceChangeEvent event) {
        IResourceDelta delta = event.getDelta();
        if (delta == null)
            return;

        final List<IProject> newProjects = new LinkedList<IProject>();
        try {
            delta.accept(new IResourceDeltaVisitor() {
                public boolean visit(IResourceDelta delta) throws CoreException {
                    if (delta.getFlags() == IResourceDelta.MARKERS)
                        return false; // ignore marker-only deltas.

                    IResource resource = delta.getResource();
                    if (resource.getType() == IResource.ROOT)
                        return true;

                    if (resource.getType() == IResource.PROJECT) {
                        IProject project = (IProject) resource;
                        if (project.isOpen() && project.hasNature(BndProjectNature.NATURE_ID) && ((delta.getKind() & IResourceDelta.ADDED) != 0)) {
                            newProjects.add(project);
                        }
                    }

                    return false;
                }
            });

            for (IProject project : newProjects) {
                RequiredObrCheckingJob requiredObrJob = new RequiredObrCheckingJob(project);
                requiredObrJob.schedule();

                AdjustClasspathsForNewProjectJob adjustClasspathsJob = new AdjustClasspathsForNewProjectJob(project);
                adjustClasspathsJob.setSystem(true);
                adjustClasspathsJob.schedule();
            }

        } catch (CoreException e) {
            Plugin.logError("An error occurred while analysing the resource change", e);
        }
    }

}
