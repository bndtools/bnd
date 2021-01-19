package bndtools.explorer;

import java.io.File;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.eclipse.EclipseUtil;
import bndtools.Plugin;
import bndtools.central.Central;

class Synchronizer {

	private static final ILogger logger = Logger.getLogger(Synchronizer.class);

	static void sync(Workspace workspace, IProgressMonitor monitor) throws CoreException {
		File base = workspace.getBase();

		IWorkspace eclipse = ResourcesPlugin.getWorkspace();
		IWorkspaceDescription description = eclipse.getDescription();

		description.setMaxConcurrentBuilds(10);
		boolean original = description.isAutoBuilding();
		description.setAutoBuilding(false);
		eclipse.setDescription(description);

		try {
			IFile build = Central.getWorkspaceBuildFile();
			IContainer workspaceContainer = build.getParent()
				.getParent();

			Set<IProject> eclipseProjects = Stream.of(ResourcesPlugin.getWorkspace()
				.getRoot()
				.getProjects())
				.filter(p -> !p.isHidden())
				.filter(p -> p.isOpen())
				.filter(p -> {
					try {
						return p.hasNature(Plugin.BNDTOOLS_NATURE);
					} catch (CoreException e) {
						return false;
					}
				})
				.filter(p -> inWorkspace(p, base))
				.filter(p -> !Workspace.CNFDIR.equals(p.getName()))
				.collect(Collectors.toSet());

			workspace.refreshProjects();

			for (Project model : workspace.getAllProjects()) {

				Optional<IProject> project = Central.getProject(model);
				if (project.isPresent()) {
					IProject current = project.get();
					eclipseProjects.remove(current);
					model.clean();
				} else {
					monitor.subTask("Create " + model);
					createProject(model, monitor);
				}
				model.clean();

				EclipseUtil.createClasspath(model);
			}

			for (IProject p : eclipseProjects) {

				if (p.getName()
					.equals(Workspace.CNFDIR))
					continue;

				monitor.subTask("Remove " + p);
				remove(p, monitor);
			}

			String[] buildOrder = workspace.getBuildOrder()
				.stream()
				.map(Project::getName)
				.toArray(String[]::new);

			description.setBuildOrder(buildOrder);
			eclipse.setDescription(description);

			build.touch(monitor);

			monitor.subTask("Refresh");
			workspaceContainer.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			workspaceContainer.deleteMarkers(BndtoolsConstants.MARKER_BND_PROBLEM, true, IResource.DEPTH_INFINITE);
			monitor.subTask("Build");
			eclipse.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (original) {
				description.setAutoBuilding(original);
				eclipse.setDescription(description);
			}
		}
	}

	private static boolean inWorkspace(IProject p, File baseDir) {
		IPath location = p.getLocation();
		if (location == null)
			return false;

		File dir = location.toFile();
		return baseDir.equals(dir.getParentFile());
	}

	private static void remove(IProject project, IProgressMonitor monitor) {
		try {
			project.delete(false, true, monitor);
		} catch (CoreException e) {
			logger.logError("Unable to remove project", e);
		}
	}

	private static void createProject(Project model, IProgressMonitor monitor) {
		IPath location = new Path(model.getBase()
			.getAbsolutePath());

		try {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IProject project = workspace.getRoot()
				.getProject(model.getName());

			IProjectDescription description = workspace.newProjectDescription(model.getName());
			description.setLocation(location);
			project.create(description, monitor);
			project.open(monitor);
		} catch (CoreException e) {
			logger.logError("Unable to add project " + model, e);
		}
	}
}
