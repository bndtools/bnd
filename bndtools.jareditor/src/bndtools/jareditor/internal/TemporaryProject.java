package bndtools.jareditor.internal;

import java.io.File;
import java.util.Set;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.versioncontrol.ignores.manager.api.VersionControlIgnoresManager;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import bndtools.preferences.BndPreferences;

public class TemporaryProject {

	public TemporaryProject() {}

	private void checkForSupportProject() throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject project = root.getProject(BndtoolsConstants.BNDTOOLS_JAREDITOR_TEMP_PROJECT_NAME);
		IProgressMonitor monitor = new NullProgressMonitor();

		if (!project.exists()) {
			createProject();
		} else if (!project.isOpen()) {
			try {
				project.open(monitor);
			} catch (Exception e) {
				// recreate project since there is something wrong with this one
				project.delete(true, monitor);
				createProject();
			}
		}

		makeFolders(project.getFolder("temp"));
	}

	private IProject createProject() throws CoreException {
		IProgressMonitor monitor = new NullProgressMonitor();
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot()
			.getProject(BndtoolsConstants.BNDTOOLS_JAREDITOR_TEMP_PROJECT_NAME);

		if (project.exists()) {
			if (!project.isOpen()) {
				project.open(monitor);
			}

			return project;
		}

		IProjectDescription description = workspace
			.newProjectDescription(BndtoolsConstants.BNDTOOLS_JAREDITOR_TEMP_PROJECT_NAME);

		IPath stateLocation = Plugin.getInstance()
			.getStateLocation();

		description.setLocation(stateLocation.append(BndtoolsConstants.BNDTOOLS_JAREDITOR_TEMP_PROJECT_NAME));

		project.create(description, monitor);
		project.open(monitor);

		try {
			VersionControlIgnoresManager versionControlIgnoresManager = bndtools.Plugin.getDefault()
				.getVersionControlIgnoresManager();
			IJavaProject javaProject = JavaCore.create(project);
			Set<String> enabledIgnorePlugins = new BndPreferences()
				.getVersionControlIgnoresPluginsEnabled(versionControlIgnoresManager, javaProject, null);
			versionControlIgnoresManager.addIgnores(enabledIgnorePlugins,
				new File(project.getLocationURI()).getParentFile(),
				BndtoolsConstants.BNDTOOLS_JAREDITOR_TEMP_PROJECT_NAME);
		} catch (Exception e) {
			// best effort
		}

		return project;
	}

	public IProject getProject() throws CoreException {
		checkForSupportProject();
		IProject project = ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProject(BndtoolsConstants.BNDTOOLS_JAREDITOR_TEMP_PROJECT_NAME);

		if (project.exists() && project.isOpen()) {
			return project;
		}
		return null;
	}

	private void makeFolders(IFolder folder) throws CoreException {
		if (folder == null) {
			return;
		}

		IContainer parent = folder.getParent();

		if (parent instanceof IFolder) {
			makeFolders((IFolder) parent);
		}

		if (!folder.exists()) {
			folder.create(true, true, null);
		}
	}
}
