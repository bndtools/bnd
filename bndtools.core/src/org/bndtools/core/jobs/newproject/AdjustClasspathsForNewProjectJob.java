package org.bndtools.core.jobs.newproject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bndtools.core.utils.workspace.WorkspaceUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import aQute.bnd.build.Container;
import aQute.bnd.build.Container.TYPE;
import aQute.bnd.build.Project;
import bndtools.Central;
import bndtools.Plugin;
import bndtools.builder.NewBuilder;

public class AdjustClasspathsForNewProjectJob extends WorkspaceJob {

    private final IProject addedProject;

    public AdjustClasspathsForNewProjectJob(IProject addedProject) {
        super("Adjusting classpaths for new project: " + addedProject.getName());
        this.addedProject = addedProject;
    }

    @Override
    public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
            MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, "Errors occurred while adjusting classpaths for new project", null);

            List<Project> projects;
            SubMonitor progress;
            try {
                projects = new ArrayList<Project>(Central.getWorkspace().getAllProjects());
                progress = SubMonitor.convert(monitor, projects.size());
            } catch (Exception e) {
                throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error getting project list", e));
            }

            IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
            while (!projects.isEmpty()) {
                Project project = projects.remove(0);
                try {
                    if (isCandidate(project)) {
                        IProject eclipseProject = WorkspaceUtils.findOpenProject(wsroot, project);
                        if (eclipseProject != null) {
                            project.setChanged();
                            eclipseProject.build(IncrementalProjectBuilder.FULL_BUILD, NewBuilder.BUILDER_ID, null, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
                            progress.setWorkRemaining(projects.size());
                        }
                    }
                } catch (CoreException e) {
                    status.add(e.getStatus());
                } catch (Exception e) {
                    status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error checking project: " + project.getName(), e));
                }
                if (progress.isCanceled())
                    return Status.CANCEL_STATUS;
            }

            if (status.isOK())
                return Status.OK_STATUS;

            return status;
    }

    private boolean isCandidate(Project project) throws Exception {
        if (project.getName().equals(addedProject.getName()))
            return false;

        Collection<Container> buildpath = project.getBuildpath();
        for (Container container : buildpath) {
            if (container.getType() == TYPE.REPO) {
                if ("latest".equals(container.getVersion()) || "snapshot".equals(container.getVersion())) {
                    String bsn = container.getBundleSymbolicName();
                    if (bsn.equals(addedProject.getName()) || bsn.startsWith(addedProject.getName() + ".")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
