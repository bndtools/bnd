package bndtools.m2e;

import org.bndtools.api.RunListener;
import org.eclipse.core.resources.IResource;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;

@Component(property = Constants.SERVICE_RANKING + ":Integer=1000")
public class MavenImplicitProjectRunListener implements MavenRunListenerHelper, RunListener {

    @Override
    public void create(Run run) throws Exception {
        IResource resource = getResource(run);

        if (!isMavenProject(resource)) {
            return;
        }

        Workspace workspace = run.getWorkspace();

        MavenImplicitProjectRepository implicitRepo = workspace.getPlugin(MavenImplicitProjectRepository.class);

        if (implicitRepo == null) {
            implicitRepo = new MavenImplicitProjectRepository(resource);
            workspace.getRepositories()
                .add(0, implicitRepo);
            workspace.addBasicPlugin(implicitRepo);
        }
    }

    @Override
    public void end(Run run) throws Exception {
        IResource resource = getResource(run);

        if (!isMavenProject(resource)) {
            return;
        }

        Workspace workspace = run.getWorkspace();

        MavenImplicitProjectRepository implicitRepo = workspace.getPlugin(MavenImplicitProjectRepository.class);

        if (implicitRepo != null) {
            workspace.getRepositories()
                .remove(implicitRepo);
            workspace.removeBasicPlugin(implicitRepo);
            implicitRepo.cleanup();
        }
    }

}
