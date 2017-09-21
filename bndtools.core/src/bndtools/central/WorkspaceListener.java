package bndtools.central;

import java.io.File;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import aQute.bnd.build.Workspace;
import aQute.bnd.service.BndListener;

public final class WorkspaceListener extends BndListener {
    private static final ILogger logger = Logger.getLogger(WorkspaceListener.class);

    public WorkspaceListener(@SuppressWarnings("unused") Workspace workspace) {}

    @Override
    public void changed(final File file) {
        try {
            Central.refreshFile(file, null, true);
        } catch (Exception e) {
            logger.logError("Error refreshing file " + file, e);
        }
    }
}
