package bndtools.m2e;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.bndtools.api.RunMode;
import org.bndtools.api.RunProvider;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.build.Run;
import aQute.bnd.maven.lib.configuration.Bndruns;

@Component(property = Constants.SERVICE_RANKING + ":Integer=1000")
public class MavenRunProvider implements MavenRunListenerHelper, RunProvider {

	@Override
	public Run create(IResource targetResource, RunMode mode) throws Exception {
		if (!isMavenProject(targetResource)) {
			return null;
		}

		IMavenProjectFacade projectFacade = getMavenProjectFacade(targetResource);

		IProgressMonitor monitor = new NullProgressMonitor();

		if ((mode == RunMode.LAUNCH) && hasBndResolverMavenPlugin(projectFacade)) {
			MojoExecution mojoExecution = projectFacade
				.getMojoExecutions("biz.aQute.bnd", "bnd-resolver-maven-plugin", monitor, "resolve")
				.stream()
				.findFirst()
				.orElse(null);

			MavenProject mavenProject = getMavenProject(projectFacade);

			Bndruns bndruns = maven.getMojoParameterValue(mavenProject, mojoExecution, "bndruns", Bndruns.class,
				monitor);
			return Run.createRun(null, bndruns.getFiles(mavenProject.getBasedir())
				.get(0));
		} else if ((mode == RunMode.TEST) && hasBndTestingMavenPlugin(projectFacade)) {
			MojoExecution mojoExecution = projectFacade
				.getMojoExecutions("biz.aQute.bnd", "bnd-testing-maven-plugin", monitor, "testing")
				.stream()
				.findFirst()
				.orElse(null);

			MavenProject mavenProject = getMavenProject(projectFacade);

			Bndruns bndruns = maven.getMojoParameterValue(mavenProject, mojoExecution, "bndruns", Bndruns.class,
				monitor);
			return Run.createRun(null, bndruns.getFiles(mavenProject.getBasedir())
				.get(0));
		} else {
			return null;
		}
	}

}
