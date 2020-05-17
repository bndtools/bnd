package bndtools.command;


import org.bndtools.api.RunMode;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import biz.aQute.resolve.Bndrun;
import bndtools.Plugin;
import bndtools.central.Central;
import bndtools.launch.util.LaunchUtils;

public class BndResolveHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String bndrunFilePath = event.getParameter(BndrunFilesParameterValues.BNDRUN_FILE);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace()
			.getRoot();
		IFile bndrunFile = root.getFile(new Path(bndrunFilePath));
		IProject project = bndrunFile.getProject();

		WorkspaceJob job = new WorkspaceJob("bnd resolve: " + bndrunFilePath) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				IStatus status = Status.OK_STATUS;
				try {
					Bndrun bndrun = (Bndrun) LaunchUtils.createRun(bndrunFile, RunMode.LAUNCH);

					bndrun.resolve(false, true);

					if (Central.isBndProject(project)) {
						bndrunFile.refreshLocal(IResource.DEPTH_ONE, monitor);
					}
				} catch (Exception e) {
					status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, "Unable to resolve jar", e);
				}

				return status;
			}
		};
		job.setRule(project);
		job.schedule();

		return null;
	}

}
