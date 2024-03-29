package bndtools.release;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.build.Workspace;
import aQute.bnd.differ.Baseline;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import bndtools.release.api.ReleaseUtils;
import bndtools.release.nl.Messages;
import bndtools.release.ui.WorkspaceReleaseDialog;

public class WorkspaceAnalyserJob extends Job {

	protected final Shell			shell;
	protected final Set<IProject>	projects;

	public WorkspaceAnalyserJob(Set<IProject> projects) {
		super(Messages.workspaceReleaseJob1);
		this.shell = PlatformUI.getWorkbench()
			.getDisplay()
			.getActiveShell();
		setUser(true);
		this.projects = projects;
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

			List<Project> orderedProjects = getBuildOrder(mon, Activator.getWorkspace());
			if (mon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			final List<ProjectDiff> projectDiffs = new ArrayList<>();
			mon.setTaskName(Messages.processingProjects);
			for (Project project : orderedProjects) {
				IProject eProject = ReleaseUtils.getProject(project);
				if (!isIncluded(eProject)) {
					mon.worked(1);
					continue;
				}
				if ("".equals(project.getProperty(Constants.RELEASEREPO, null))) {
					mon.worked(1);
					continue;
				}
				if (eProject == null || !eProject.isOpen() || !eProject.isAccessible()) {
					mon.worked(1);
					continue;
				}
				List<Baseline> jarDiffs = null;
				try (ProjectBuilder pb = project.getBuilder(null)) {
					for (Builder b : pb.getSubBuilders()) {
						mon.subTask(String.format(Messages.processingProject, b.getBsn()));

						Baseline jarDiff = DiffHelper.createBaseline(b);
						if (jarDiff != null) {
							if (jarDiffs == null) {
								jarDiffs = new ArrayList<>();
							}
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

			if (projectDiffs.isEmpty()) {
				Runnable runnable = () -> MessageDialog.openInformation(shell, Messages.releaseWorkspaceBundles,
					Messages.noBundlesRequireRelease);
				Display display = Display.getCurrent();
				if (display == null)
					display = Display.getDefault();
				display.syncExec(runnable);
				return Status.OK_STATUS;
			}

			ReleaseHelper.initializeProjectDiffs(projectDiffs);

			Runnable runnable = () -> {
				WorkspaceReleaseDialog dialog = new WorkspaceReleaseDialog(shell, projectDiffs, false);
				int ret = dialog.open();
				if (ret == Window.OK) {
					boolean runJob = false;
					for (ProjectDiff diff : projectDiffs) {
						if (diff.isRelease()) {
							runJob = true;
							break;
						}
					}
					if (!runJob) {
						return;
					}
					WorkspaceReleaseJob releaseJob = new WorkspaceReleaseJob(projectDiffs, dialog.getReleaseOption(),
						dialog.isShowMessage());
					releaseJob.setRule(ResourcesPlugin.getWorkspace()
						.getRoot());
					releaseJob.schedule();
				}
			};

			Display display = Display.getCurrent();
			if (display == null)
				display = Display.getDefault();
			display.asyncExec(runnable);
		} catch (Exception e) {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
		}
		return Status.OK_STATUS;
	}

	private static List<Project> getBuildOrder(IProgressMonitor monitor, Workspace workspace) throws Exception {

		List<Project> outlist = new ArrayList<>();
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

	private static void getBuildOrder(Collection<Project> dependsOn, List<Project> outlist) throws Exception {

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

	protected boolean isIncluded(IProject project) {
		if (projects == null) {
			return true;
		}
		return projects.contains(project);
	}
}
