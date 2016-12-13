package bndtools.m2e;

import org.bndtools.api.RunListener;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.build.Run;

@Component
public class MavenWorkspaceRunListener implements RunListener {

    @Override
    public void create(Run run) throws Exception {
        run.getWorkspace().addBasicPlugin(new MavenWorkspaceRepository());
    }

    @Override
    public void end(Run run) throws Exception {
        MavenWorkspaceRepository repo = run.getWorkspace().getPlugin(MavenWorkspaceRepository.class);

        if (repo != null) {
            run.getWorkspace().removeBasicPlugin(repo);
            repo.cleanup();
        }
    }
}
