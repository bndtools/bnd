package bndtools.command;

import java.io.File;
import java.io.OutputStream;
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
import aQute.bnd.osgi.Resource;
import aQute.lib.io.IO;
import biz.aQute.resolve.Bndrun;
import bndtools.Plugin;
import bndtools.central.Central;

public class BndExportJarHandler extends AbstractHandler {
	private static final String EXECUTABLE_JAR = "bnd.executablejar";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String bndrunFilePath = event.getParameter(BndrunFilesParameterValues.BNDRUN_FILE);
		IFile bndrunFile = ResourcesPlugin.getWorkspace()
			.getRoot()
			.getFile(new Path(bndrunFilePath));
		IProject project = bndrunFile.getProject();

		WorkspaceJob job = new WorkspaceJob("bnd export jar: " + bndrunFilePath) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				IStatus status = Status.OK_STATUS;
				try {
					Bndrun bndrun = Bndrun.createBndrun(Central.getWorkspace(), new File(bndrunFile.getLocationURI()));
					Project bndProject = Central.getProject(project);
					bndrun.setBase(bndProject.getBase());

					Entry<String, Resource> export = bndrun.export(EXECUTABLE_JAR,
						Collections.emptyMap());

					if (export != null) {
						try (Resource resource = export.getValue()) {
							File exportDir = IO.getBasedFile(bndProject.getTargetDir(), "export");
							exportDir.mkdirs();
							File exported = IO.getBasedFile(exportDir, export.getKey());
							try (OutputStream out = IO.outputStream(exported)) {
								resource.write(out);
							}
							exported.setLastModified(resource.lastModified());
							project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
						}
					}
				} catch (Exception e) {
					status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, "Unable to export jar", e);
				}
				return status;
			}
		};
		job.setRule(project);
		job.schedule();

		return null;
	}

}
