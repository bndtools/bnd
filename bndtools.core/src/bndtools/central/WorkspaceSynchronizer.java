package bndtools.central;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import aQute.lib.strings.Strings;
import bndtools.Plugin;

/**
 * A utility class to synchronize the bnd & Eclipse workspaces as well as build.
 */
public class WorkspaceSynchronizer {

	private static final ILogger				logger		= Logger.getLogger(WorkspaceSynchronizer.class);
	private static IWorkspace					eclipse		= ResourcesPlugin.getWorkspace();
	private static IWorkspaceRoot				wsroot		= eclipse.getRoot();
	final static ThreadLocal<IProgressMonitor>	monitors	= new ThreadLocal<>();
	IWorkspaceDescription						description	= eclipse.getDescription();

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

			Map<String, IProject> eclipseBndProjects = getAllBndEclipseProjects(ws.getBase());
			IProject cnf = wsroot.getProject(Workspace.CNFDIR);
			if (!cnf.exists()) {
				subMonitor.setTaskName("Creating cnf");
				// must be done inline
				createProject(ws.getFile(Workspace.CNFDIR), null, subMonitor.split(1));
			}
			eclipseBndProjects.remove(Workspace.CNFDIR);

			List<String> toBeCreated = new ArrayList<>();

			Central.bndCall(() -> {
				ws.refreshProjects();
				ws.refresh();
				ws.forceRefresh();
				for (Project model : ws.getAllProjects()) {
					if (model.isCnf()) {
						continue;
					}
					IProject exists = eclipseBndProjects.remove(model.getName());
					if (exists == null) {
						toBeCreated.add(model.getName());
					}
					EclipseUtil.fixDirectories(model);
				}
				return null;
			}, monitor);

			Collection<IProject> toBeDeleted = eclipseBndProjects.values();

			subMonitor.setTaskName("Scheduling creation/deletion of projects");
			subMonitor.setWorkRemaining(80);

			for (IProject project : toBeDeleted) {
				removeProject(project, monitor);
			}

			subMonitor.checkCanceled();

			Central.bndCall(() -> {
				for (String projectName : toBeCreated) {
					Project project = ws.getProject(projectName);
					if (project != null) {
						project.clean();
						createProject(project.getBase(), project, monitor);
					}
					subMonitor.checkCanceled();
				}
				return null;
			});

			if (refresh)
				wsroot.refreshLocal(IResource.DEPTH_INFINITE, monitor);

		} catch (Exception e) {
			Status status = new Status(Status.ERROR, "bndtools.builder", e.getMessage());
			throw new CoreException(status);
		} finally {
			atend.run();
			setAutobuild(previous);
		}
	}

	private boolean hasNature(IProject project) {
		try {
			if (project.isAccessible())
				return project.hasNature(Plugin.BNDTOOLS_NATURE);
		} catch (CoreException e) {
			logger.logError("cannot get nature from " + project, e);
		}
		return false;
	}

	private boolean setAutobuild(boolean on) throws CoreException {
		boolean original = description.isAutoBuilding();
		description.setAutoBuilding(on);
		eclipse.setDescription(description);
		return original;
	}

	private Map<String, IProject> getAllBndEclipseProjects(File base) throws CoreException {
		Map<String, IProject> result = new HashMap<>();
		for (IProject project : ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProjects()) {

			if (project.isHidden()) {
				// System.out.println("Hidden " + project);
				continue;
			}

			if (!inWorkspace(project, base)) {
				continue;
			}

			if (!isClosedAndEmpty(project) && !hasNature(project) && !project.getName()
				.equals(Workspace.CNFDIR))
				continue;

			result.put(project.getName(), project);
		}
		return result;
	}

	private boolean isClosedAndEmpty(IProject project) {
		if (project.isOpen())
			return false;

		File projectDir = project.getLocation()
			.toFile();
		if (projectDir == null)
			return true;

		String[] sub = projectDir.list();

		if (sub == null)
			return true;

		if (sub.length == 0)
			return true;

		if (Strings.in(sub, ".project")) {
			boolean onlyOneFile = sub.length == 1;
			return onlyOneFile;
		}

		return true;
	}

	private static boolean inWorkspace(IProject p, File baseDir) {
		IPath location = p.getLocation();
		if (location == null)
			return false;

		File dir = location.toFile();
		return baseDir.equals(dir.getParentFile());
	}

	private static void removeProject(IProject project, IProgressMonitor monitor) {
		try {
			project.delete(false, true, monitor);
		} catch (CoreException e) {
			logger.logError("Unable to remove project " + project, e);
		}
	}

	private static IProject createProject(File directory, Project model, IProgressMonitor monitor)
		throws CoreException {
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
