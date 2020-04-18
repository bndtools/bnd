package bndtools.m2e;

import org.bndtools.api.RunListener;
import org.eclipse.core.resources.IResource;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;

@Component
public class MavenWorkspaceRunListener implements MavenRunListenerHelper, RunListener {

	@Override
	public void create(Run run) throws Exception {
	}

	@Override
	public void end(Run run) throws Exception {
		IResource resource = getResource(run);

		if (!isMavenProject(resource)) {
			return;
		}

		Workspace workspace = run.getWorkspace();

		MavenWorkspaceRepository repo = workspace.getPlugin(MavenWorkspaceRepository.class);

		if (repo != null) {
			repo.cleanup();
		}
	}
}
