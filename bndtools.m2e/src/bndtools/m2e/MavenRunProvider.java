package bndtools.m2e;

import java.io.File;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Run;
import aQute.bnd.maven.lib.configuration.Bndruns;
import aQute.bnd.maven.lib.resolve.BndrunContainer;

/*
 * This provider must be processed before the DefaultRunProvider because
 * that one does not check to see if the project is M2E.
 */
@Component(property = Constants.SERVICE_RANKING + ":Integer=3000")
public class MavenRunProvider implements MavenRunListenerHelper, RunProvider {

	private static final Logger			logger	= LoggerFactory.getLogger(MavenRunProvider.class);

	private final MavenBndrunContainer mbc = new MavenBndrunContainer();

	@Override
	public Run create(IResource targetResource, RunMode mode) throws Exception {
		if (!isMavenProject(targetResource)) {
			return null;
		}

		logger.info("Creating a Run for IResource {}", targetResource);

		IMavenProjectFacade projectFacade = getMavenProjectFacade(targetResource);
		MavenProject mavenProject = getMavenProject(projectFacade);

		IProgressMonitor monitor = new NullProgressMonitor();
		MojoExecution mojoExecution;

		if (((mode == RunMode.LAUNCH) || (mode == RunMode.EDIT))
			&& ((mojoExecution = getBndResolverMojoExecution(projectFacade, monitor)) != null)) {

			Bndruns bndruns = maven.getMojoParameterValue(mavenProject, mojoExecution, "bndruns", Bndruns.class,
				monitor);

			BndrunContainer bndrunContainer = mbc.getBndrunContainer(projectFacade, mojoExecution, monitor);

			return bndrunContainer.init(bndruns.getFiles(mavenProject.getBasedir())
				.get(0), mode.name(),
				new File(mavenProject.getBuild()
					.getDirectory()));
		} else if ((mode == RunMode.TEST)
			&& ((mojoExecution = getBndTestingMojoExecution(projectFacade, monitor)) != null)) {

			Bndruns bndruns = maven.getMojoParameterValue(mavenProject, mojoExecution, "bndruns", Bndruns.class,
				monitor);

			BndrunContainer bndrunContainer = mbc.getBndrunContainer(projectFacade, mojoExecution, monitor);

			return bndrunContainer.init(bndruns.getFiles(mavenProject.getBasedir())
				.get(0), mode.name(),
				new File(mavenProject.getBuild()
					.getDirectory()));
		} else {
			return null;
		}
	}

}
