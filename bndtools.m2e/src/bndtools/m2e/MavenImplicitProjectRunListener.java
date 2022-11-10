package bndtools.m2e;

import static bndtools.m2e.MavenRunListenerHelper.getResource;
import static bndtools.m2e.MavenRunListenerHelper.isMavenProject;

import org.bndtools.api.RunListener;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;

@Component(property = Constants.SERVICE_RANKING + ":Integer=1000")
public class MavenImplicitProjectRunListener implements RunListener {

	@Reference
	IMavenProjectRegistry mavenProjectRegistry;

	@Override
	public void create(Run run) throws Exception {}

	@Override
	public void end(Run run) throws Exception {
		IResource resource = getResource(run);

		if (!isMavenProject(mavenProjectRegistry, resource)) {
			return;
		}

		Workspace workspace = run.getWorkspace();

		MavenImplicitProjectRepository repo = workspace.getPlugin(MavenImplicitProjectRepository.class);

		if (repo != null) {
			mavenProjectRegistry.removeMavenProjectChangedListener(repo);
			ResourcesPlugin.getWorkspace()
				.removeResourceChangeListener(repo);
		}
	}

}
