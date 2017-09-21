package org.bndtools.builder;

import java.util.Collection;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.utils.workspace.WorkspaceUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;
import org.osgi.util.promise.Success;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import bndtools.central.Central;

public class CnfWatcher implements IResourceChangeListener {
    private static final ILogger logger = Logger.getLogger(CnfWatcher.class);
    private static final CnfWatcher INSTANCE = new CnfWatcher();

    static CnfWatcher install() {
        ResourcesPlugin.getWorkspace().addResourceChangeListener(INSTANCE, IResourceChangeEvent.POST_CHANGE);
        return INSTANCE;
    }

    private CnfWatcher() {}

    @Override
    public void resourceChanged(final IResourceChangeEvent event) {
        if (Central.isWorkspaceInited()) {
            processEvent(event);
        } else {
            Central.onWorkspaceInit(new Success<Workspace,Void>() {
                @Override
                public Promise<Void> call(Promise<Workspace> resolved) throws Exception {
                    try {
                        processEvent(event);
                        return Promises.resolved(null);
                    } catch (Exception e) {
                        return Promises.failed(e);
                    }
                }
            });
        }
    }

    private void processEvent(IResourceChangeEvent event) {
        try {
            final Workspace workspace = Central.getWorkspaceIfPresent();

            if (workspace == null) {
                // this can happen during first project creation in an empty workspace
                logger.logInfo("Unable to get workspace", null);
                return;
            }

            final IProject cnfProject = WorkspaceUtils.findCnfProject(ResourcesPlugin.getWorkspace().getRoot(), workspace);
            if (cnfProject == null)
                return;

            IResourceDelta delta = event.getDelta();
            if (delta.findMember(cnfProject.getFullPath()) == null)
                return;

            Collection<Project> allProjects = workspace.getAllProjects();
            if (allProjects.isEmpty())
                return;

            Project p = allProjects.iterator().next();
            DeltaWrapper dw = new DeltaWrapper(p, delta, new BuildLogger(BuildLogger.LOG_NONE, "", 0));
            if (dw.hasCnfChanged()) {
                workspace.clear();
                workspace.forceRefresh();
                workspace.getPlugins();

                BndtoolsBuilder.dirty.addAll(allProjects);

                WorkspaceJob j = new WorkspaceJob("Update errors on workspace") {
                    @Override
                    public IStatus runInWorkspace(IProgressMonitor arg0) throws CoreException {
                        try {
                            MarkerSupport ms = new MarkerSupport(cnfProject);
                            ms.deleteMarkers("*");
                            ms.setMarkers(workspace, BndtoolsConstants.MARKER_BND_WORKSPACE_PROBLEM);
                            return Status.OK_STATUS;
                        } catch (Exception e) {
                            return new Status(IStatus.ERROR, BndtoolsBuilder.PLUGIN_ID, "updating errors for workspace", e);
                        }
                    }
                };
                j.schedule();
            }
        } catch (Exception e) {
            logger.logError("Detecting changes in cnf failed, ignoring", e);
        }
    }
}
