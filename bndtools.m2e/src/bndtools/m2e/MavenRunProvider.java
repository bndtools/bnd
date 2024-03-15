package bndtools.m2e;

import static bndtools.m2e.MavenRunListenerHelper.containsBndrun;
import static bndtools.m2e.MavenRunListenerHelper.getBndResolverMojoExecution;
import static bndtools.m2e.MavenRunListenerHelper.getBndTestingMojoExecution;
import static bndtools.m2e.MavenRunListenerHelper.getMavenProject;
import static bndtools.m2e.MavenRunListenerHelper.getMavenProjectFacade;
import static bndtools.m2e.MavenRunListenerHelper.isMavenProject;

import java.io.File;
import java.util.function.Predicate;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.bndtools.api.RunMode;
import org.bndtools.api.RunProvider;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Workspace;
import aQute.bnd.maven.lib.configuration.Bndruns;
import biz.aQute.resolve.Bndrun;

/*
 * This provider must be processed before the DefaultRunProvider because
 * that one does not check to see if the project is M2E.
 */
@Component(property = Constants.SERVICE_RANKING + ":Integer=3000")
public class MavenRunProvider implements RunProvider {

	private static final Logger			logger	= LoggerFactory.getLogger(MavenRunProvider.class);

	private MavenWorkspaceRepository	mavenWorkspaceRepository;

	@Reference
	IMaven								maven;
	@Reference
	IMavenProjectRegistry				mavenProjectRegistry;

	@Override
	public Bndrun create(IResource targetResource, RunMode mode) throws Exception {
		if (!isMavenProject(mavenProjectRegistry, targetResource)) {
			return null;
		}

		final IMavenProjectFacade projectFacade = getMavenProjectFacade(mavenProjectRegistry,
			targetResource);

		Bndrun bndrun = create0(targetResource, projectFacade, mode);
		if (bndrun == null) {
			return null;
		}
		Workspace workspace = bndrun.getWorkspace();

		final MavenImplicitProjectRepository implicitRepo = new MavenImplicitProjectRepository( //
			mavenProjectRegistry, projectFacade, bndrun);

		mavenProjectRegistry.addMavenProjectChangedListener( //
			implicitRepo);
		ResourcesPlugin.getWorkspace()
			.addResourceChangeListener( //
			implicitRepo, IResourceChangeEvent.POST_CHANGE);

		workspace.addBasicPlugin(implicitRepo);

		if (((mode == RunMode.LAUNCH) || (mode == RunMode.TEST)) && (mavenWorkspaceRepository != null)) {
			workspace.addBasicPlugin(mavenWorkspaceRepository);
		}

		workspace.refresh();

		if (mode == RunMode.EDIT) {
			new Job("Create implicit repo") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					implicitRepo.createRepo(projectFacade, monitor);
					return Status.OK_STATUS;
				}
			}.schedule();
		} else {
			implicitRepo.createRepo(projectFacade, new NullProgressMonitor());
		}

		return bndrun;
	}

	private Bndrun create0(IResource targetResource, IMavenProjectFacade projectFacade, RunMode mode) throws Exception {
		logger.info("Creating a Run for IResource {}", targetResource);

		final MavenProject mavenProject = getMavenProject(projectFacade);
		final IProgressMonitor monitor = new NullProgressMonitor();

		Predicate<MojoExecution> bndrunMatchs = f -> true;
		File bndrunFile = null;

		if (targetResource.getName()
			.endsWith(".bndrun")) {

			File location = targetResource.getLocation()
				.toFile();
			bndrunFile = location;
			bndrunMatchs = mojoExecution -> containsBndrun(maven, mojoExecution, mavenProject, location, monitor);
		}

		MojoExecution mojoExecution;

		switch (mode) {
			case LAUNCH :
			case EDIT :
			case EXPORT :
			case SOURCES :
				if ((mojoExecution = getBndResolverMojoExecution(maven, projectFacade, bndrunMatchs,
					monitor)) != null) {

					if (bndrunFile == null) {
						Bndruns bndruns = maven.getMojoParameterValue(mavenProject, mojoExecution, "bndruns",
							Bndruns.class, monitor);

						bndrunFile = bndruns.getFiles(mavenProject.getBasedir())
							.get(0);
					}

					MavenBndrunContainer mavenBndrunContainer = MavenBndrunContainer.getBndrunContainer(maven,
						mavenProjectRegistry, projectFacade, mojoExecution, monitor);

					return mavenBndrunContainer.init(bndrunFile, mode.name(), new File(mavenProject.getBuild()
						.getDirectory()));
				} else {
					throw new IllegalStateException(String
						.format("Cannot find a resolver configuration for '%s' in pom.xml.", targetResource.getName()));
				}
			case TEST :
				if ((mojoExecution = getBndTestingMojoExecution(maven, projectFacade, bndrunMatchs, monitor)) != null) {

					if (bndrunFile == null) {
						Bndruns bndruns = maven.getMojoParameterValue(mavenProject, mojoExecution, "bndruns",
							Bndruns.class, monitor);

						bndrunFile = bndruns.getFiles(mavenProject.getBasedir())
							.get(0);
					}

					MavenBndrunContainer mavenBndrunContainer = MavenBndrunContainer.getBndrunContainer(maven,
						mavenProjectRegistry, projectFacade, mojoExecution, monitor);

					return mavenBndrunContainer.init(bndrunFile, mode.name(), new File(mavenProject.getBuild()
						.getDirectory()));
				}
				break;
			default :
				break;
		}
		return null;
	}

	@Reference
	public void setMavenWorkspaceRepository(MavenWorkspaceRepository mavenWorkspaceRepository) {
		this.mavenWorkspaceRepository = mavenWorkspaceRepository;
	}

	public void unsetMavenWorkspaceRepository(MavenWorkspaceRepository mavenWorkspaceRepository) {
		if (this.mavenWorkspaceRepository == mavenWorkspaceRepository) {
			this.mavenWorkspaceRepository = null;
		}
	}

}
