package bndtools.m2e;

import static aQute.bnd.exceptions.ConsumerWithException.asConsumer;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Run;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.maven.lib.resolve.BndrunContainer;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.bnd.service.Refreshable;
import aQute.bnd.version.Version;
import bndtools.central.Central;

public class MavenImplicitProjectRepository extends AbstractMavenRepository
	implements IResourceChangeListener, Refreshable {

	private static final org.slf4j.Logger	logger	= LoggerFactory.getLogger(MavenImplicitProjectRepository.class);

	private final CompletableFuture<FileSetRepository>	initialRepository	= new CompletableFuture<>();
	private volatile FileSetRepository					fileSetRepository;
	private final IMavenProjectFacade					projectFacade;
	private final Run									run;
	private final IPath									bndrunFilePath;

	public MavenImplicitProjectRepository(IMavenProjectRegistry mavenProjectRegistry,
		IMavenProjectFacade projectFacade, Run run) {
		super(mavenProjectRegistry);
		this.projectFacade = projectFacade;
		this.run = run;

		bndrunFilePath = projectFacade.getFullPath(run.getPropertiesFile());
	}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		return getFileSetRepository().findProviders(requirements);
	}

	@Override
	public File get(final String bsn, final Version version, Map<String, String> properties,
		final DownloadListener... listeners) throws Exception {
		return getFileSetRepository().get(bsn, version, properties, listeners);
	}

	@Override
	public String getName() {
		return projectFacade.getProject()
			.getName() + " (implicit)";
	}

	@Override
	public File getRoot() throws Exception {
		return projectFacade.getPomFile();
	}

	@Override
	public List<String> list(String pattern) throws Exception {
		return getFileSetRepository().list(pattern);
	}

	@Override
	public void mavenProjectChanged(List<MavenProjectChangedEvent> events, IProgressMonitor monitor) {
		if (events == null)
			return;

		for (MavenProjectChangedEvent event : events) {
			final IMavenProjectFacade mavenProjectFacade = event.getMavenProject();

			if (!mavenProjectFacade.getProject()
				.equals(projectFacade.getProject())
				&& (event.getFlags() != MavenProjectChangedEvent.FLAG_DEPENDENCIES)) {

				continue;
			}

			createRepo(mavenProjectFacade, monitor);
		}
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if ((event != null) && event.getDelta()
			.findMember(bndrunFilePath) != null) {
			createRepo(projectFacade, new NullProgressMonitor());
		}
	}

	@Override
	public boolean refresh() throws Exception {
		return true;
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		return getFileSetRepository().versions(bsn);
	}

	protected void createRepo(IMavenProjectFacade projectFacade, IProgressMonitor monitor) {
		MavenProject mavenProject = MavenRunListenerHelper.getMavenProject(projectFacade);
		try {
			BndrunContainer bndrunContainer = run.getPlugin(BndrunContainer.class);

			FileSetRepository repository = bndrunContainer.getFileSetRepository(mavenProject);
			repository.list(null);
			fileSetRepository = repository;
			initialRepository.complete(repository);

			fullRefresh();
		} catch (Exception e) {
			logger.error("Failed to create implicit repository for m2e project {}", getName(), e);

			try {
				String name = mavenProject.getName()
					.isEmpty() ? mavenProject.getArtifactId() : mavenProject.getName();

				FileSetRepository repository = new FileSetRepository(name, Collections.emptyList());
				fileSetRepository = repository;
				initialRepository.complete(repository);

				fullRefresh();
			} catch (Exception e2) {
				initialRepository.completeExceptionally(e2);
				throw Exceptions.duck(e2);
			}
		}
	}

	private FileSetRepository getFileSetRepository() {
		FileSetRepository repository = fileSetRepository;
		return (repository != null) ? repository : initialRepository.join();
	}

	private void fullRefresh() throws Exception {
		Central.refreshPlugin(this);
		run.refresh();
		run.getWorkspace()
			.getRepositories()
			.stream()
			.filter(Refreshable.class::isInstance)
			.map(Refreshable.class::cast)
			.forEach(asConsumer(Central::refreshPlugin));
	}

}
