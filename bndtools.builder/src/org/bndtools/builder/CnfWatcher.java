package org.bndtools.builder;

import java.util.Collection;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import bndtools.central.Central;

public class CnfWatcher implements IResourceChangeListener {
    private static final ILogger logger = Logger.getLogger(CnfWatcher.class);
    private static final Path CNFPATH = new Path("/cnf");
    volatile int revision = 1000;

    static CnfWatcher install() {
        CnfWatcher cnfw = new CnfWatcher();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(cnfw);
        return cnfw;
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {

        if (event.getType() != IResourceChangeEvent.POST_CHANGE)
            return;

        IResourceDelta delta = event.getDelta();
        if (delta.findMember(CNFPATH) == null)
            return;

        try {
            final Workspace workspace = Central.getWorkspace();
            Collection<Project> allProjects = workspace.getAllProjects();
            if (allProjects.isEmpty())
                return;

            Project p = allProjects.iterator().next();
            DeltaWrapper dw = new DeltaWrapper(p, delta, new BuildLogger(0));
            if (dw.hasCnfChanged()) {

                workspace.clear();
                workspace.forceRefresh();
                workspace.getPlugins();

                BndtoolsBuilder.dirty.addAll(workspace.getAllProjects());

                WorkspaceJob j = new WorkspaceJob("Update errors on workspace") {

                    @Override
                    public IStatus runInWorkspace(IProgressMonitor arg0) throws CoreException {
                        try {
                            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                            IProject project = root.getProject(Workspace.CNFDIR);
                            if (project != null) {
                                MarkerSupport ms = new MarkerSupport(project);
                                ms.setMarkers(workspace, BndtoolsConstants.MARKER_BND_WORKSPACE_PROBLEM);
                            }
                            return Status.OK_STATUS;
                        } catch (Exception e) {
                            return new Status(IStatus.ERROR, BndtoolsConstants.CORE_PLUGIN_ID, "updating errors for workspace", e);
                        }
                    }
                };
                j.schedule();
            }
        } catch (Exception e) {
            logger.logError("Detecting changes in cnf failed, ignoring", e);
        }
    }

    int getRevision() {
        return revision;
    }

}
