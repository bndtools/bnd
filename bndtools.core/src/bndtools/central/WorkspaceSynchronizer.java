package bndtools.central;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
import org.eclipse.core.runtime.jobs.Job;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.eclipse.EclipseUtil;
import bndtools.Plugin;

/**
 * A utility class to synchronize the bnd & Eclipse workspaces as well as build.
 */
public class WorkspaceSynchronizer {

	private static final ILogger				logger		= Logger.getLogger(WorkspaceSynchronizer.class);
	private static IWorkspace					eclipse		= ResourcesPlugin.getWorkspace();
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
			eclipse.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
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

		try {
			Workspace ws = Central.getWorkspace();
			if (ws == null || ws.isDefaultWorkspace())
				return;

			IWorkspaceRoot wsroot = eclipse.getRoot();

			if (refresh)
				refresh(subMonitor.split(20));

			subMonitor.setWorkRemaining(80);
			boolean previous = setAutobuild(false);

			Map<String, IProject> existing = getAllBndEclipseProjects(ws.getBase());
			IProject cnf = wsroot.getProject(Workspace.CNFDIR);
			if (!cnf.exists()) {
				subMonitor.setTaskName("Creating cnf");
				// must be done inline
				createProject(ws.getFile(Workspace.CNFDIR), null, subMonitor.split(1));
			}
			existing.remove(Workspace.CNFDIR);

			List<Project> missing = new ArrayList<>();

			for (Project model : Central.bndCall(() -> ws.getAllProjects())) {
				if (model.isCnf()) {
					continue;
				}
				IProject exists = existing.remove(model.getName());
				if (exists == null) {
					missing.add(model);
				}
				EclipseUtil.fixDirectories(model);
			}

			existing.values()
				.removeIf(project -> !project.isAccessible() || !hasNature(project));

			if (existing.isEmpty() && missing.isEmpty()) {
				if (refresh)
					build(ws, subMonitor);
				setAutobuild(previous);
				atend.run();
				return;
			}
			subMonitor.setTaskName("Scheduling creation/deletion of projects");
			subMonitor.setWorkRemaining(80);

			CountDownLatch missingCounter = new CountDownLatch(missing.size());
			CountDownLatch existingCounter = new CountDownLatch(existing.size());

			for (IProject project : existing.values()) {
				Job job = Job.create("remove " + project, (m) -> {
					try {
						removeProject(project, m);
					} finally {
						existingCounter.countDown();
					}
				});
				job.setRule(null);
				job.schedule();
			}

			subMonitor.checkCanceled();

			for (Project project : missing) {
				Job job = Job.create("add " + project, (m) -> {
					try {
						createProject(project.getBase(), project, m);
					} finally {
						missingCounter.countDown();
					}
				});
				job.setRule(null);
				job.schedule();
			}

			if (!refresh) {
				setAutobuild(previous);
				atend.run();
				return;
			}

			subMonitor.checkCanceled();

			Job job = Job.create("build", (m) -> {
				try {
					int totalWork = existing.size() + missing.size() + 100;
					SubMonitor forBuild = SubMonitor.convert(m, totalWork);
					forBuild.setTaskName("Waiting for the projects to be created");
					while (!missingCounter.await(500, TimeUnit.MILLISECONDS)) {
						forBuild.checkCanceled();
						forBuild.setWorkRemaining(
							(int) (totalWork - missingCounter.getCount() - existingCounter.getCount()));
					}
					forBuild.setWorkRemaining(60);
					forBuild.setTaskName("Waiting for the projects to be deleted");
					while (!existingCounter.await(500, TimeUnit.MILLISECONDS)) {
						forBuild.checkCanceled();
						forBuild.setWorkRemaining(
							(int) (totalWork - missingCounter.getCount() - existingCounter.getCount()));
					}
					forBuild.setWorkRemaining(100);
					build(ws, forBuild);
				} catch (Exception e) {
					Status status = new Status(Status.ERROR, "bndtools.builder", e.getMessage());
					throw new CoreException(status);
				} finally {
					atend.run();
				}
			});
			job.setRule(null);
			job.schedule();
		} catch (Exception e) {
			Status status = new Status(Status.ERROR, "bndtools.builder", e.getMessage());
			throw new CoreException(status);
		}
	}

	private boolean hasNature(IProject project) {
		try {
			return project.hasNature(Plugin.BNDTOOLS_NATURE);
		} catch (CoreException e) {
			logger.logError("cannot get nature from " + project, e);
			return false;
		}
	}

	private void refresh(IProgressMonitor monitor) throws CoreException {
		eclipse.getRoot()
			.refreshLocal(IResource.DEPTH_INFINITE, monitor);
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
				// System.out.println("Not in workspace " + project);
				continue;
			}

			result.put(project.getName(), project);
		}
		return result;
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
