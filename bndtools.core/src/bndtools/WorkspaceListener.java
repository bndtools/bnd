package bndtools;

import java.io.File;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import aQute.bnd.build.Workspace;
import aQute.bnd.service.BndListener;
import aQute.lib.osgi.Processor;
import aQute.libg.reporter.Reporter;

public final class WorkspaceListener extends BndListener {

    private final Workspace workspace;
    private final Processor errorProcessor = new Processor();

    public WorkspaceListener(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public void changed(final File file) {
        try {
            RefreshFileJob job = new RefreshFileJob(file);
            if (job.isFileInWorkspace()) {
                job.schedule();
            }
        } catch (Exception e) {
            Plugin.logError("Error refreshing workspace", e);
        }
    }

    @Override
    public void signal(Reporter reporter) {
        errorProcessor.clear();
        errorProcessor.getInfo(workspace);

        for (String warning : errorProcessor.getWarnings()) {
            Plugin.log(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0, warning, null));
        }
        for (String error : errorProcessor.getErrors()) {
            Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, error, null));
        }
    }

}