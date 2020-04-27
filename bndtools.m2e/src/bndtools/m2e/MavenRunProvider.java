package bndtools.m2e;

import java.io.File;
import java.util.function.Predicate;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.bndtools.api.RunMode;
import org.bndtools.api.RunProvider;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Workspace;
import aQute.bnd.maven.lib.configuration.Bndruns;
import aQute.bnd.maven.lib.resolve.BndrunContainer;
import biz.aQute.resolve.Bndrun;

/*
 * This provider must be processed before the DefaultRunProvider because
 * that one does not check to see if the project is M2E.
 */
@Component(property = Constants.SERVICE_RANKING + ":Integer=3000")
public class MavenRunProvider implements MavenRunListenerHelper, RunProvider {

	private static final Logger			logger	= LoggerFactory.getLogger(MavenRunProvider.class);

	private final MavenBndrunContainer mbc = new MavenBndrunContainer();

	private MavenWorkspaceRepository	mavenWorkspaceRepository;

	@Override
	public Bndrun create(IResource targetResource, RunMode mode) throws Exception {
		if (!isMavenProject(targetResource)) {
			return null;
		}

		final IMavenProjectFacade projectFacade = getMavenProjectFacade(targetResource);

		Bndrun bndrun = create0(targetResource, projectFacade, mode);

		Workspace workspace = bndrun.getWorkspace();

		final MavenImplicitProjectRepository implicitRepo = new MavenImplicitProjectRepository( //
			projectFacade, bndrun);

		mavenProjectRegistry.addMavenProjectChangedListener( //
			implicitRepo);
		iWorkspace.addResourceChangeListener( //
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
			bndrunMatchs = mojoExecution -> containsBndrun(mojoExecution, mavenProject, location, monitor);
		}

		MojoExecution mojoExecution;

		if (((mode == RunMode.LAUNCH) || (mode == RunMode.EDIT))
			&& ((mojoExecution = getBndResolverMojoExecution(projectFacade, bndrunMatchs, monitor)) != null)) {

			if (bndrunFile == null) {
				Bndruns bndruns = maven.getMojoParameterValue(mavenProject, mojoExecution, "bndruns", Bndruns.class,
					monitor);

				bndrunFile = bndruns.getFiles(mavenProject.getBasedir())
					.get(0);
			}

			BndrunContainer bndrunContainer = mbc.getBndrunContainer(projectFacade, mojoExecution, monitor);

			return bndrunContainer.init(bndrunFile, mode.name(),
				new File(mavenProject.getBuild()
					.getDirectory()));
		} else if ((mode == RunMode.TEST)
			&& ((mojoExecution = getBndTestingMojoExecution(projectFacade, bndrunMatchs, monitor)) != null)) {

			if (bndrunFile == null) {
				Bndruns bndruns = maven.getMojoParameterValue(mavenProject, mojoExecution, "bndruns", Bndruns.class,
					monitor);

				bndrunFile = bndruns.getFiles(mavenProject.getBasedir())
					.get(0);
			}

			BndrunContainer bndrunContainer = mbc.getBndrunContainer(projectFacade, mojoExecution, monitor);

			return bndrunContainer.init(bndrunFile, mode.name(),
				new File(mavenProject.getBuild()
					.getDirectory()));
		} else {
			return null;
		}
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
