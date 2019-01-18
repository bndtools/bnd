package bndtools.m2e;

import java.io.File;
import java.nio.file.Paths;

import org.apache.maven.project.MavenProject;
import org.bndtools.api.RunListener;
import org.eclipse.core.resources.IResource;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.lib.io.IO;

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
            IMavenProjectFacade projectFacade = getMavenProjectFacade(resource);
            MavenProject mavenProject = getMavenProject(projectFacade);

            String bndrun = getNamePart(resource.getName());
            File temporaryDir = Paths.get(mavenProject.getBuild()
                .getDirectory(), "tmp", "resolve", bndrun)
                .toFile();
            File cnf = new File(temporaryDir, Workspace.CNFDIR);
            IO.mkdirs(cnf);

            run.setBase(temporaryDir);
            workspace.setBase(temporaryDir);
            workspace.setBuildDir(cnf);
            workspace.setOffline(isOffline());

            implicitRepo = new MavenImplicitProjectRepository(projectFacade);
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

    private String getNamePart(String nameExt) {
        int pos = nameExt.lastIndexOf('.');
        return (pos > 0) ? nameExt.substring(0, pos) : nameExt;
    }

}
