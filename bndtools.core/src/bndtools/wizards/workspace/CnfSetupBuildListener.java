package bndtools.wizards.workspace;

import java.util.concurrent.atomic.AtomicReference;

import org.bndtools.build.api.AbstractBuildListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.progress.UIJob;

public class CnfSetupBuildListener extends AbstractBuildListener {

    private static final long DELAY = 3000;
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
            CnfSetupWizard.showIfNeeded(false);
            return Status.OK_STATUS;
        }

    }

}
