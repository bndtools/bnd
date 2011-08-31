package bndtools.wizards.workspace;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import aQute.bnd.build.Project;

public class CnfSetupStartupParticipant implements Runnable {

	public void run() {
		CnfSetupWizard.showIfNeeded(false);

		// Clean the cnf/cache if it exists
        final IWorkspace ws = ResourcesPlugin.getWorkspace();
        new WorkspaceJob("Clear cnf/cache") {
            @Override
            public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
                SubMonitor progress = SubMonitor.convert(monitor, 3);

                IProject cnfProject = ws.getRoot().getProject(Project.BNDCNF);
                cnfProject.refreshLocal(0, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
                if (!cnfProject.exists())
                    return Status.OK_STATUS;
                if (!cnfProject.isOpen())
                    cnfProject.open(progress.newChild(1, SubMonitor.SUPPRESS_NONE));
                else
                    progress.setWorkRemaining(2);

                return Status.OK_STATUS;
            }
        }.schedule();
	}

}
