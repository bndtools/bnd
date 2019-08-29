package bndtools.release;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.differ.Baseline;
import aQute.bnd.osgi.Builder;
import bndtools.release.nl.Messages;
import bndtools.release.ui.WorkspaceReleaseDialog;

public class ReleaseDialogJob extends Job {

	protected final Shell		shell;
	protected final Project		project;
	private final List<File>	subBundles;

	public ReleaseDialogJob(Project project, List<File> subBundles) {
		super(Messages.releaseJob);
		this.project = project;
		this.shell = PlatformUI.getWorkbench()
			.getDisplay()
			.getActiveShell();
		this.subBundles = subBundles;
		setUser(true);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {

		try {
			monitor.beginTask(Messages.cleaningProject, 100);
			monitor.setTaskName(Messages.releasing);
			monitor.worked(33);
			monitor.subTask(Messages.checkingExported);

			final List<Baseline> diffs = new ArrayList<>();

			try (ProjectBuilder pb = project.getBuilder(null)) {
				for (Builder builder : pb.getSubBuilders()) {

					if (subBundles != null) {
						if (!subBundles.contains(builder.getPropertiesFile())) {
							continue;
						}
					}

					Baseline diff = DiffHelper.createBaseline(builder);
					if (diff != null) {
						diffs.add(diff);
					}
				}
			}
			if (diffs.isEmpty()) {
				// TODO: message
				return Status.OK_STATUS;
			}
			monitor.worked(33);

			Runnable runnable = () -> {
				List<ProjectDiff> projectDiffs = new ArrayList<>();
				projectDiffs.add(new ProjectDiff(project, diffs));
				ReleaseHelper.initializeProjectDiffs(projectDiffs);
				WorkspaceReleaseDialog dialog = new WorkspaceReleaseDialog(shell, projectDiffs, true);
				if (dialog.open() == Window.OK) {
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

			if (Display.getCurrent() == null) {
				Display.getDefault()
					.asyncExec(runnable);
			} else {
				runnable.run();
			}

			monitor.worked(33);
			return Status.OK_STATUS;
		} catch (Exception e) {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error : " + e.getMessage(), e);
		} finally {

			monitor.done();
		}

	}

}
