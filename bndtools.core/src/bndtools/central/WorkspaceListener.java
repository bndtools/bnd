package bndtools.central;

import java.io.File;
import java.util.Collection;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.ResourcesPlugin;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.service.BndListener;

@Deprecated
public final class WorkspaceListener extends BndListener {
	private static final ILogger logger = Logger.getLogger(WorkspaceListener.class);

	public WorkspaceListener(Workspace workspace) {}

	@Override
	public void changed(File file) {
		try {
			if (ResourcesPlugin.getWorkspace()
				.isTreeLocked()) {
				// Sometimes we may be called from a resource delta event
				// handler
				// so we use a job to refresh the file
				final RefreshFileJob job = new RefreshFileJob(file, true);
				if (job.needsToSchedule()) {
					Central.onAnyWorkspace(workspace -> job.schedule());
				}
			} else {
				Central.refreshFile(file, null, true);
			}
		} catch (Exception e) {
			logger.logError("Error refreshing file " + file, e);
		}
	}

	@Override
	public void built(Project model, Collection<File> files) {
		Central.getProject(model)
			.ifPresent(project -> {
				EclipseWorkspaceRepository eclipseWorkspaceRepository = Central.getEclipseWorkspaceRepository();
				eclipseWorkspaceRepository.index(project, files);
			});
	}

}
