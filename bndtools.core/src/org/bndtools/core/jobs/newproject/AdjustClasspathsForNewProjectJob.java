package org.bndtools.core.jobs.newproject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bndtools.core.utils.workspace.WorkspaceUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import aQute.bnd.build.Project;
import bndtools.Central;
import bndtools.Plugin;
import bndtools.classpath.BndContainerInitializer;

public class AdjustClasspathsForNewProjectJob extends WorkspaceJob {

    private final IProject addedProject;

    public AdjustClasspathsForNewProjectJob(IProject addedProject) {
        super("Adjusting classpaths for new project: " + addedProject.getName());
        this.addedProject = addedProject;
    }

    @Override
    public IStatus runInWorkspace(IProgressMonitor monitor) {
        List<Project> projects;
        SubMonitor progress;
        try {
            projects = new ArrayList<Project>(Central.getWorkspace().getAllProjects());
            progress = SubMonitor.convert(monitor, projects.size());
        } catch (Exception e) {
            return Status.CANCEL_STATUS;
        }

        IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
        while (!projects.isEmpty()) {
            Project project = projects.remove(0);
            IProject eclipseProject = WorkspaceUtils.findOpenProject(wsroot, project);
            if (eclipseProject != null && !eclipseProject.equals(addedProject)) {
                List<String> errors = new LinkedList<String>();
                if (eclipseProject != null) {
                    try {
                        project.propertiesChanged();
                        BndContainerInitializer.resetClasspaths(project, eclipseProject, errors);
                        BndContainerInitializer.replaceClasspathProblemMarkers(eclipseProject, errors);
                    } catch (CoreException e) {
                        Plugin.log(e.getStatus());
                        return Status.CANCEL_STATUS;
                    }
                }
                progress.worked(1);
            }
            if (progress.isCanceled())
                return Status.CANCEL_STATUS;
        }
        return Status.OK_STATUS;
    }

}
