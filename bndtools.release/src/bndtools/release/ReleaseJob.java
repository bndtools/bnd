package bndtools.release;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import bndtools.release.api.ReleaseContext;
import bndtools.release.api.ReleaseOption;
import bndtools.release.api.ReleaseUtils;
import bndtools.release.nl.Messages;

public class ReleaseJob extends Job {

	private final ReleaseContext	context;
	private final boolean			showMessage;

	public ReleaseJob(ReleaseContext context, boolean showMessage) {
		super(Messages.bundleReleaseJob);
		this.context = context;
		this.showMessage = showMessage;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {

		try {

			context.setProgressMonitor(monitor);

			IProject proj = ReleaseUtils.getProject(context.getProject());
			proj.refreshLocal(IResource.DEPTH_INFINITE, monitor);

			boolean success = ReleaseHelper.release(context, context.getBaselines());

			ResourcesPlugin.getWorkspace()
				.getRoot()
				.getProject(context.getProject()
					.getName())
				.refreshLocal(IResource.DEPTH_INFINITE, context.getProgressMonitor());

			if (context.getReleaseRepository() != null) {
				File f = Activator.getLocalRepoLocation(context.getReleaseRepository());
				if (f != null && f.exists()) {
					Activator.refreshFile(f);
				}
			}
			if (success && showMessage) {
				StringBuilder sb = new StringBuilder();
				sb.append(Messages.project2);
				sb.append(" : "); //$NON-NLS-1$
				sb.append(context.getProject()
					.getName());
				sb.append("\n\n"); //$NON-NLS-1$
				if (context.getReleaseOption() == ReleaseOption.UPDATE) {
					sb.append(Messages.updatedVersionInfo);
				} else {
					sb.append(Messages.released);
					sb.append(" :\n"); //$NON-NLS-1$
				}

				for (String jarInfo : context.getReleaseSummaries()) {
					sb.append(jarInfo)
						.append('\n');
				}

				if (context.getReleaseOption() != ReleaseOption.UPDATE) {
					sb.append("\n\n"); //$NON-NLS-1$
					sb.append(Messages.releasedTo);
					sb.append(" : "); //$NON-NLS-1$
					sb.append(context.getReleaseRepository()
						.getName());
				}

				Activator.message(sb.toString());
			}

		} catch (Exception e) {
			// for (Baseline spec : context.getBaselines()) {
			// context.getErrorHandler().error(spec.getBsn(),
			// jarDiff.getSuggestedVersion() != null ?
			// jarDiff.getSuggestedVersion().toString() : "0.0.0",
			// e.getMessage());
			// }
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
		}

		return Status.OK_STATUS;
	}
}
