package org.bndtools.builder;

import java.util.Collection;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

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
            Workspace workspace = Central.getWorkspace();
            Collection<Project> allProjects = workspace.getAllProjects();
            if (allProjects.isEmpty())
                return;

            Project p = allProjects.iterator().next();
            DeltaWrapper dw = new DeltaWrapper(p, delta, new BuildLogger(0));
            if (dw.hasCnfChanged()) {
                workspace.refresh();
                BndtoolsBuilder.dirty.addAll(workspace.getAllProjects());
            }
        } catch (Exception e) {
            logger.logError("Detecting changes in cnf failed, ignoring", e);
        }
    }

    int getRevision() {
        return revision;
    }

}
