package bndtools;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import bndtools.central.Central;

public class RefreshReposHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		new WorkspaceJob("Refresing repositories...") {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				if (monitor == null)
					monitor = new NullProgressMonitor();

				try {
					Central.refreshPlugins();
				} catch (Exception e) {
					return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, "Failed to refresh repositories", e);
				}

				return Status.OK_STATUS;
			}
		}.schedule();

		return null;
	}
}
