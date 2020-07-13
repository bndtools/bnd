package bndtools.core.test.utils;

import static bndtools.core.test.utils.TaskUtils.countDownMonitor;
import static bndtools.core.test.utils.TaskUtils.log;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;

import aQute.lib.exceptions.Exceptions;

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

	public static void importProject(Path sourceProject) {
		CountDownLatch importFlag = new CountDownLatch(1);
		importProject(sourceProject, importFlag);
		log("Waiting for import of " + sourceProject);
		try {
			if (!importFlag.await(10000, TimeUnit.MILLISECONDS)) {
				log("WARN: timed out waiting for import to finish " + sourceProject);
			} else {
				log("Finished waiting for import " + sourceProject);
			}
		} catch (InterruptedException e) {
			throw Exceptions.duck(e);
		}
	}

	public void reimportProject(String projectName) {
		try {
			IWorkspaceRoot wsr = ResourcesPlugin.getWorkspace()
				.getRoot();

			IProject project = wsr.getProject(projectName);
			if (project.exists()) {
				project.delete(true, true, null);
			}
			importProject(root.resolve(projectName));
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	public void importProject(String project) {
		importProject(root.resolve(project));
	}

	public void importProject(String project, CountDownLatch importFlag) {
		importProject(root.resolve(project), importFlag);
	}

	public static void importProject(Path sourceProject, CountDownLatch importFlag) {
		try {
			IWorkspaceRoot wsr = ResourcesPlugin.getWorkspace()
				.getRoot();

			String projectName = sourceProject.getFileName()
				.toString();
			IProject project = wsr.getProject(projectName);
			ImportOperation importOperation = new ImportOperation(project.getFullPath(), sourceProject.toFile(),
				FileSystemStructureProvider.INSTANCE, overwriteQuery);
			importOperation.setCreateContainerStructure(false);
			importOperation.run(countDownMonitor(importFlag));
		} catch (InvocationTargetException e) {
			throw Exceptions.duck(e.getTargetException());
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}
}
