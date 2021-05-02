package bndtools.core.test.utils;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;

import aQute.bnd.exceptions.Exceptions;
import bndtools.central.Central;

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
			IWorkspaceRoot wsr = ResourcesPlugin.getWorkspace()
				.getRoot();

			IProject project = wsr.getProject(projectName);
			if (project.exists()) {
				project.delete(true, true, null);
			}
			importProject(root.resolve(projectName), null);
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
		Job job = new WorkspaceJob("Clean and import all") {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) {
				try {
					// Clean the workspace
					IProject[] existingProjects = wsr.getProjects();
					for (IProject project : existingProjects) {
						project.delete(true, true, monitor);
					}

					projects.forEach(path -> importProject(path, monitor));
					return Status.OK_STATUS;
				} catch (Exception e) {
					return new Status(IStatus.ERROR, WorkspaceImporter.class, 0,
						"Error during import: " + e.getMessage(), e);
				}
			}
		};
		// Lock the entire workspace so that we don't run simultaneously with
		// other jobs; see eg #4573.
		job.setRule(wsr);
		job.schedule();
		try {
			if (!job.join(10000, null)) {
				TaskUtils.dumpWorkspace();
				throw new IllegalStateException("Timed out waiting for workspace import to complete");
			}

			// Wait for Workspace object to be complete.
			final CountDownLatch flag = new CountDownLatch(1);
			Central.onCnfWorkspace(bndWS -> {
				flag.countDown();
			});
			flag.await(10000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
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
