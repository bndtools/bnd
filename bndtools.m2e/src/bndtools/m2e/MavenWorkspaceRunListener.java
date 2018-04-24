package bndtools.m2e;

import org.bndtools.api.RunListener;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;

@Component
public class MavenWorkspaceRunListener implements RunListener {

    @Override
    public void create(Run run) throws Exception {
        Workspace workspace = run.getWorkspace();
        MavenWorkspaceRepository repo = workspace.getPlugin(MavenWorkspaceRepository.class);

        if (repo == null) {
            repo = new MavenWorkspaceRepository();
            workspace.getRepositories()
                .add(0, repo);
            workspace.addBasicPlugin(repo);
        }
    }

    @Override
    public void end(Run run) throws Exception {
        Workspace workspace = run.getWorkspace();
        MavenWorkspaceRepository repo = workspace.getPlugin(MavenWorkspaceRepository.class);

        if (repo != null) {
            workspace.getRepositories()
                .remove(repo);
            workspace.removeBasicPlugin(repo);
            repo.cleanup();
        }
    }
}
