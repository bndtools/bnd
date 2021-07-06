package bndtools.core.test.utils;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;

import aQute.bnd.exceptions.Exceptions;

public class WorkspaceImporter {
	private static final IOverwriteQuery	overwriteQuery	= file -> IOverwriteQuery.ALL;

	private final Path						root;

	/**
	 * @param root the root path to the resources directory where the template
	 *            projects are stored.
	 */
	public WorkspaceImporter(Path root) {
		this.root = root;
	}

	public void reimportProject(String projectName) {
		try {
			IWorkspace ws = ResourcesPlugin.getWorkspace();
			IWorkspaceRoot wsr = ResourcesPlugin.getWorkspace()
				.getRoot();

			ws.run(monitor -> {
				IProject project = wsr.getProject(projectName);
				if (project.exists()) {
					project.delete(true, true, null);
				}
				importProject(root.resolve(projectName), null);
			}, null);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	public void importProject(String project) {
		importProject(root.resolve(project), null);
	}

	// First cleans the workspace of all existing projects and then imports the
	// specified projects.
	// Wraps all of the operations into a single WorkspaceJob to avoid multiple
	// resource change events.
	public static void importAllProjects(Stream<Path> projects) {
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot wsr = ResourcesPlugin.getWorkspace()
			.getRoot();
		try {
			ws.run(monitor -> {
				// Clean the workspace
				IProject[] existingProjects = wsr.getProjects();
				SubMonitor loopMonitor = SubMonitor.convert(monitor, existingProjects.length * 2);
				for (IProject project : existingProjects) {
					project.delete(true, true, loopMonitor.split(1));
				}

				projects.forEach(path -> importProject(path, loopMonitor.split(1)));
			}, new LoggingProgressMonitor("importAllProjects()"));

			TaskUtils.updateWorkspace("importAllProjects()");
			TaskUtils.requestClasspathUpdate("importAllProjects()");
			TaskUtils.waitForBuild("importAllProjects()");
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	public static void importProject(Path sourceProject, IProgressMonitor monitor) {
		IWorkspaceRoot wsr = ResourcesPlugin.getWorkspace()
			.getRoot();

		TaskUtils.log("importing sourceProject: " + sourceProject);
		String projectName = sourceProject.getFileName()
			.toString();
		IProject project = wsr.getProject(projectName);
		ImportOperation importOperation = new ImportOperation(project.getFullPath(), sourceProject.toFile(),
			FileSystemStructureProvider.INSTANCE, overwriteQuery);
		importOperation.setCreateContainerStructure(false);
		try {
			importOperation.run(monitor);
		} catch (InterruptedException e) {
			throw Exceptions.duck(e);
		} catch (InvocationTargetException e) {
			throw Exceptions.duck(e.getTargetException());
		}
	}
}
