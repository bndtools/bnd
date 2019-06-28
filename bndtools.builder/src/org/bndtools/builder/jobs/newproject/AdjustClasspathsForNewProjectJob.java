package org.bndtools.builder.jobs.newproject;

import java.util.ArrayList;
import java.util.List;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.builder.BndtoolsBuilder;
import org.bndtools.builder.classpath.BndContainerInitializer;
import org.bndtools.utils.workspace.WorkspaceUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import aQute.bnd.build.Project;
import bndtools.central.Central;

class AdjustClasspathsForNewProjectJob extends WorkspaceJob {
	private static final ILogger	logger	= Logger.getLogger(AdjustClasspathsForNewProjectJob.class);

	private final List<IProject>	addedProjects;

	AdjustClasspathsForNewProjectJob(List<IProject> addedProjects) {
		super("Adjusting classpaths for new projects");
		this.addedProjects = addedProjects;
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) {
		List<Project> projects;
		SubMonitor progress;
		try {
			projects = new ArrayList<>(Central.getWorkspace()
				.getAllProjects());
			progress = SubMonitor.convert(monitor, addedProjects.size());
		} catch (Exception e) {
			return Status.CANCEL_STATUS;
		}

		IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace()
			.getRoot();
		for (Project project : projects) {
			IProject eclipseProject = WorkspaceUtils.findOpenProject(wsroot, project);
			if (eclipseProject != null && addedProjects.contains(eclipseProject)) {
				try {
					project.propertiesChanged();
					IJavaProject javaProject = JavaCore.create(eclipseProject);
					if (javaProject != null) {
						BndContainerInitializer.requestClasspathContainerUpdate(javaProject);
					}
				} catch (CoreException e) {
					IStatus result = new Status(e.getStatus()
						.getSeverity(), BndtoolsBuilder.PLUGIN_ID,
						"Failure to update classpath for project " + eclipseProject, e);
					logger.logStatus(result);
				}
				progress.worked(1);
			}
			if (progress.isCanceled())
				return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}

}
