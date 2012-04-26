package bndtools.release;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;

import aQute.bnd.service.RepositoryPlugin;
import bndtools.release.api.ReleaseContext;
import bndtools.release.nl.Messages;

public class WorkspaceReleaseJob extends Job {

	private List<ProjectDiff> projectDiffs;
	private boolean updateOnly;
	
	public WorkspaceReleaseJob(List<ProjectDiff> projectDiffs, boolean updateOnly) {
		super(Messages.workspaceReleaseJob);
		this.projectDiffs = projectDiffs;
		this.updateOnly = updateOnly;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		monitor.beginTask("Releasing projects...", projectDiffs.size());
		for (ProjectDiff projectDiff : projectDiffs) {
			if (projectDiff.isRelease()) {
				
				RepositoryPlugin release = null;
				if (projectDiff.getReleaseRepository() != null) {
					release = Activator.getRepositoryPlugin(projectDiff.getReleaseRepository());
				}

				ReleaseContext context = new ReleaseContext(projectDiff.getProject(), projectDiff.getJarDiffs(), release, updateOnly);
				ReleaseJob job = new ReleaseJob(context, false);
				job.run(new SubProgressMonitor(monitor, 1));
			}
			monitor.worked(1);
		}
		monitor.done();
		
		return Status.OK_STATUS;
	}

}
