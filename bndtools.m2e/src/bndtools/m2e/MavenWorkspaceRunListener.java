package bndtools.m2e;

import org.bndtools.api.RunListener;
import org.eclipse.core.resources.IResource;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import bndtools.launch.util.LaunchUtils.Mode;

@Component
public class MavenWorkspaceRunListener implements MavenRunListenerHelper, RunListener {

    @Override
    public void create(Run run) throws Exception {
        IResource resource = getResource(run);

        if (!isMavenProject(resource)) {
            return;
        }

        Workspace workspace = run.getWorkspace();

        if (Mode.getMode(run) == Mode.LAUNCH) {
            MavenWorkspaceRepository repo = workspace.getPlugin(MavenWorkspaceRepository.class);

            if (repo == null) {
                repo = new MavenWorkspaceRepository();
                workspace.getRepositories()
                    .add(0, repo);
                workspace.addBasicPlugin(repo);
            }
        }
    }

    @Override
    public void end(Run run) throws Exception {
        IResource resource = getResource(run);

        if (!isMavenProject(resource)) {
            return;
        }

        Workspace workspace = run.getWorkspace();

        if (Mode.getMode(run) == Mode.LAUNCH) {
            MavenWorkspaceRepository repo = workspace.getPlugin(MavenWorkspaceRepository.class);

            if (repo != null) {
                workspace.getRepositories()
                    .remove(repo);
                workspace.removeBasicPlugin(repo);
                repo.cleanup();
            }
        }
    }
}
