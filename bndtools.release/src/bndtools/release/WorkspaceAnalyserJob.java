/*******************************************************************************
 * Copyright (c) 2012 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package bndtools.release;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.differ.Baseline;
import aQute.bnd.osgi.Builder;
import aQute.bnd.service.diff.Delta;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Diff.Ignore;
import bndtools.release.api.ReleaseUtils;
import bndtools.release.nl.Messages;
import bndtools.release.ui.WorkspaceReleaseDialog;

public class WorkspaceAnalyserJob extends Job {

	protected final Shell shell;

	public WorkspaceAnalyserJob() {
		super(Messages.workspaceReleaseJob1);
		this.shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
		setUser(true);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		IProgressMonitor mon = monitor;
		if (mon == null) {
			mon = new NullProgressMonitor();
		}

		try {
			Collection<Project> projects = Activator.getWorkspace()
					.getAllProjects();

			mon.beginTask(Messages.workspaceReleaseJob, projects.size() * 2);

			List<Project> orderedProjects = getBuildOrder(mon,
					Activator.getWorkspace());
			if (mon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			final List<ProjectDiff> projectDiffs = new ArrayList<ProjectDiff>();
			mon.setTaskName(Messages.processingProjects);
			for (Project project : orderedProjects) {
				IProject eProject = ReleaseUtils.getProject(project);
				if (!eProject.isOpen() || !eProject.isAccessible()) {
					continue;
				}
				List<Builder> builders = project.getBuilder(null)
						.getSubBuilders();
				List<Baseline> jarDiffs = null;
				for (Builder b : builders) {
					mon.subTask(String.format(Messages.processingProject, b.getBsn()));

					Baseline jarDiff = DiffHelper.createBaseline(project,
							b.getBsn());
					if (jarDiff != null) {
						if (jarDiffs == null) {
							jarDiffs = new ArrayList<Baseline>();
						}

						Delta delta = jarDiff.getDiff().getDelta(new Ignore() {
                            public boolean contains(Diff diff) {
                               if ("META-INF/MANIFEST.MF".equals(diff.getName())) { //$NON-NLS-1$
                                   return true;
                               }
                                return false;
                            }});
						if (delta != Delta.UNCHANGED && delta != Delta.IGNORED) {
							jarDiffs.add(jarDiff);
						}
					}
				}
				if (jarDiffs != null && jarDiffs.size() > 0) {
					projectDiffs.add(new ProjectDiff(project, jarDiffs));
				}
				if (mon.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				mon.worked(1);
			}

			if (projectDiffs.size() == 0) {
				Runnable runnable = new Runnable() {
					public void run() {
						MessageDialog.openInformation(shell, Messages.releaseWorkspaceBundles, Messages.noBundlesRequireRelease);
					}
				};
				if (Display.getCurrent() == null) {
					Display.getDefault().syncExec(runnable);
				} else {
					runnable.run();
				}
				return Status.OK_STATUS;
			}

			ReleaseHelper.initializeProjectDiffs(projectDiffs);

			Runnable runnable = new Runnable() {
				public void run() {
					WorkspaceReleaseDialog dialog = new WorkspaceReleaseDialog(
							shell, projectDiffs);
					int ret = dialog.open();
					if (ret == WorkspaceReleaseDialog.OK) {
						WorkspaceReleaseJob releaseJob = new WorkspaceReleaseJob(
								projectDiffs, false);
						releaseJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
						releaseJob.schedule();
					}
				}
			};

			if (Display.getCurrent() == null) {
				Display.getDefault().asyncExec(runnable);
			} else {
				runnable.run();
			}

		} catch (Exception e) {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					e.getMessage(), e);
		}
		return Status.OK_STATUS;
	}

	private static List<Project> getBuildOrder(IProgressMonitor monitor,
			Workspace workspace) throws Exception {

		List<Project> outlist = new ArrayList<Project>();
		monitor.setTaskName(Messages.calculatingBuildPath);
		for (Project project : workspace.getAllProjects()) {

			monitor.subTask(String.format(Messages.resolvingDependenciesForProject, project.getName()));
			Collection<Project> dependsOn = project.getDependson();

			getBuildOrder(dependsOn, outlist);

			if (!outlist.contains(project)) {
				outlist.add(project);
			}
			monitor.worked(1);
		}
		return outlist;
	}

	private static void getBuildOrder(Collection<Project> dependsOn,
			List<Project> outlist) throws Exception {

		for (Project project : dependsOn) {
			Collection<Project> subProjects = project.getDependson();
			for (Project subProject : subProjects) {
				if (!outlist.contains(subProject)) {
					outlist.add(subProject);
				}
			}
            if (!outlist.contains(project)) {
                outlist.add(project);
            }
		}
	}
}
