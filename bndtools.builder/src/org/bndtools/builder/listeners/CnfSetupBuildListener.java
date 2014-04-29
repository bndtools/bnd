package org.bndtools.builder.listeners;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import org.bndtools.build.api.AbstractBuildListener;
import org.bndtools.builder.NewBuilder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ui.progress.UIJob;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import bndtools.central.Central;
import bndtools.wizards.workspace.CnfSetupWizard;

public class CnfSetupBuildListener extends AbstractBuildListener {

    private static final long DELAY = 1500;
    private static final AtomicReference<CnfSetupJob> jobRef = new AtomicReference<CnfSetupBuildListener.CnfSetupJob>();

    @Override
    public void buildStarting(final IProject project) {
        CnfSetupJob job = new CnfSetupJob();
        if (jobRef.compareAndSet(null, job)) {
            job.schedule(DELAY);
        }
    }

    private static class CnfSetupJob extends UIJob {

        public CnfSetupJob() {
            super("Cnf Initialisation");
        }

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            boolean shown = CnfSetupWizard.showIfNeeded(false);

            if (shown)
                new RebuildProjectsJob().schedule(DELAY);

            return Status.OK_STATUS;
        }

    }

    private static class RebuildProjectsJob extends WorkspaceJob {

        public RebuildProjectsJob() {
            super("Rebuilding bnd projects after creating workspace configuration.");
        }

        @Override
        public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
            Collection<Project> projects = null;
            try {
                Workspace ws = Central.getWorkspace();
                projects = ws.getBuildOrder();
            } catch (Exception e) {
                throw new CoreException(new Status(IStatus.ERROR, NewBuilder.PLUGIN_ID, 0, "Error rebuilding bnd projects after creating workspace configuration.", e));
            }
            if (projects == null || projects.isEmpty())
                return Status.OK_STATUS;

            SubMonitor progress = SubMonitor.convert(monitor, projects.size());
            for (Project project : projects) {
                IJavaProject eclipseProject = Central.getJavaProject(project);
                eclipseProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
            }
            return Status.OK_STATUS;
        }

    }

}
