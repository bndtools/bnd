package bndtools.command;

import java.io.File;
import java.util.Collections;
import java.util.Map.Entry;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Resource;
import biz.aQute.resolve.Bndrun;
import bndtools.Plugin;
import bndtools.central.Central;

public class BndExportRunbundlesHandler extends AbstractHandler {
	private static final String RUNBUNDLES = "bnd.runbundles";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String bndrunFilePath = event.getParameter(BndrunFilesParameterValues.BNDRUN_FILE);
		IFile bndrunFile = ResourcesPlugin.getWorkspace()
			.getRoot()
			.getFile(new Path(bndrunFilePath));
		IProject project = bndrunFile.getProject();

		WorkspaceJob job = new WorkspaceJob("bnd export runbundles: " + bndrunFilePath) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				IStatus status = Status.OK_STATUS;
				try {
					Bndrun bndrun = Bndrun.createBndrun(Central.getWorkspace(), new File(bndrunFile.getLocationURI()));
					Project bndProject = Central.getProject(project);
					bndrun.setBase(bndProject.getBase());

					Entry<String, Resource> export = bndrun.export(RUNBUNDLES,
						Collections.emptyMap());

					if (export != null) {
						try (JarResource jarResource = (JarResource) export.getValue()) {
							File runbundlesDir = bndProject
								.getTargetDir()
								.toPath()
								.resolve("export")
								.resolve(bndrunFile.getName())
								.toFile();
							jarResource.getJar()
								.writeFolder(runbundlesDir);
							project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
						}
					}
				} catch (Exception e) {
					status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, "Unable to export runbundles", e);
				}
				return status;
			}
		};
		job.setRule(project);
		job.schedule();

		return null;
	}

}
