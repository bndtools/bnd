package bndtools.central.sync;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.eclipse.EclipseUtil;
import bndtools.central.Central;

/**
 * A utility class to synchronize the bnd & Eclipse workspaces as well as build.
 */
public class WorkspaceSynchronizer {

	private static final ILogger	logger	= Logger.getLogger(WorkspaceSynchronizer.class);
	private static IWorkspace		eclipse	= ResourcesPlugin.getWorkspace();
	private static IWorkspaceRoot	wsroot	= eclipse.getRoot();

	/**
	 * Build the whole workspace.
	 *
	 * @param workspace the workspace
	 * @param monitor a monitor
	 */
	boolean build(Workspace workspace, IProgressMonitor monitor) throws CoreException {

		try {
			eclipse.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
		} catch (OperationCanceledException e) {
			return true;
		} catch (Exception e) {
			logger.logError("build failed", e);
		}
		return monitor.isCanceled();
	}

	/**
	 * Check if all the projects are imported. Make it as fast as possible if
	 * they are.
	 *
	 * @param refresh true if a refresh must be done
	 * @throws CoreException
	 */
	public void synchronize(boolean refresh, IProgressMonitor monitor, Runnable atend) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

		boolean previous = setAutobuild(false);
		try {
			Workspace ws = Central.getWorkspace();
			if (ws == null || ws.isDefaultWorkspace())
				return;
			System.out.println("Syncing");

			Map<String, IProject> projects = new HashMap<>();

			for (IProject project : wsroot.getProjects()) {
				if (!project.exists())
					continue;

				if (!project.isAccessible())
					continue;

				IPath location = project.getLocation();
				if (location == null)
					continue;

				File projectDir = location.toFile();
				if (projectDir.isDirectory())
					continue;

				if (!projectDir.getParentFile()
					.equals(ws.getBase()))
					continue;

				projects.put(projectDir.getName(), project);
			}

			IProject cnf = wsroot.getProject(Workspace.CNFDIR);
			if (!cnf.exists()) {
				subMonitor.setTaskName("Creating cnf");
				// must be done inline
				createProject(ws.getFile(Workspace.CNFDIR), null, subMonitor.split(1));
			}

			List<String> models = Central.bndCall(after -> {
				ws.refreshProjects();
				ws.forceRefresh();
				return ws.getBuildOrder()
					.stream()
					.map(Project::getName)
					.collect(Collectors.toList());

			}, monitor);
			projects.remove(Workspace.CNFDIR);
			models.remove(Workspace.CNFDIR);

			for (String mm : models) {
				IProject project = projects.remove(mm);
				if (project != null)
					continue;

				System.out.println("creating " + mm);

				File dir = ws.getFile(mm);
				project = createProject(dir, null, subMonitor);
			}

			for (String toBeDeleted : projects.keySet()) {
				IProject project = wsroot.getProject(toBeDeleted);

				if (project != null && isEmpty(project)) {
					System.out.println("deleting " + toBeDeleted);
					project.delete(false, subMonitor);
				}
			}

			if (refresh && monitor != null) {
				monitor.subTask("Refresh workspace ");
				wsroot.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			}

		} catch (

		Exception e) {
			e.printStackTrace();
			Status status = new Status(Status.ERROR, "bndtools.builder", e.getMessage());
			throw new CoreException(status);
		} finally {
			atend.run();
			setAutobuild(previous);
		}
	}

	private boolean isEmpty(IProject project) {

		IPath location = project.getLocation();
		if (location == null)
			return false;

		File folder = location.toFile();
		if (!folder.isDirectory())
			return true;

		File[] listFiles = folder.listFiles();
		if (listFiles.length == 0)
			return true;

		return false;
	}

	private boolean setAutobuild(boolean on) throws CoreException {
		IWorkspaceDescription description = eclipse.getDescription();
		boolean original = description.isAutoBuilding();
		description.setAutoBuilding(on);
		eclipse.setDescription(description);
		return original;
	}

	public static void removeProject(IProject project, IProgressMonitor monitor) {
		try {
			project.delete(false, true, monitor);
		} catch (CoreException e) {
			logger.logError("Unable to remove project " + project, e);
		}
	}

	public static IProject createProject(File directory, Project model, IProgressMonitor monitor) throws CoreException {
		IPath location = new Path(directory.getAbsolutePath());

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot()
			.getProject(directory.getName());

		if (!project.exists()) {
			try {
				if (model != null) {
					if (!model.getFile(".project")
						.isFile()) {
						EclipseUtil.createProject(model);
					}
					if (!model.getFile(".classpath")
						.isFile()) {
						EclipseUtil.createClasspath(model);
					}
				}
				IProjectDescription description = workspace.newProjectDescription(directory.getName());
				description.setLocation(location);
				project.create(description, monitor);
				project.open(monitor);
			} catch (Exception e) {
				logger.logError("Failed to create project " + project, e);
			}
		}

		return project;
	}
}
