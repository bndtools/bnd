package bndtools.central;

import java.io.File;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.ResourcesPlugin;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;

import aQute.bnd.build.Workspace;
import aQute.bnd.service.BndListener;

public final class WorkspaceListener extends BndListener {
    private static final ILogger logger = Logger.getLogger(WorkspaceListener.class);

    public WorkspaceListener(@SuppressWarnings("unused") Workspace workspace) {}

    @Override
    public void changed(File file) {
        try {
            if (ResourcesPlugin.getWorkspace().isTreeLocked()) {
                // Sometimes we may be called from a resource delta event handler
                // so we use a job to refresh the file
                final RefreshFileJob job = new RefreshFileJob(file, true);
                if (job.needsToSchedule()) {
                    Central.onWorkspaceInit(new Success<Workspace,Void>() {
                        @Override
                        public Promise<Void> call(Promise<Workspace> resolved) throws Exception {
                            job.schedule();
                            return null;
                        }
                    });
                }
            } else {
                Central.refreshFile(file, null, true);
            }
        } catch (Exception e) {
            logger.logError("Error refreshing file " + file, e);
        }
    }
}
