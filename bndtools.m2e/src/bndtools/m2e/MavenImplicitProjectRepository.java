package bndtools.m2e;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.maven.project.MavenProject;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import aQute.bnd.build.Run;
import aQute.bnd.maven.lib.resolve.BndrunContainer;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.bnd.service.Refreshable;
import aQute.bnd.version.Version;
import aQute.lib.exceptions.Exceptions;
import bndtools.central.Central;

public class MavenImplicitProjectRepository extends AbstractMavenRepository implements Refreshable {

	private static final ILogger						logger					= Logger
		.getLogger(MavenImplicitProjectRepository.class);

	private volatile FileSetRepository					fileSetRepository;

	private final IMavenProjectFacade					projectFacade;
	private final Run									run;

	public MavenImplicitProjectRepository(IMavenProjectFacade projectFacade, Run run) {
		this.projectFacade = projectFacade;
		this.run = run;
	}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		if (fileSetRepository == null) {
			return Collections.emptyMap();
		}
		return fileSetRepository.findProviders(requirements);
	}

	@Override
	public File get(final String bsn, final Version version, Map<String, String> properties,
		final DownloadListener... listeners) throws Exception {
		if (fileSetRepository == null) {
			return null;
		}
		return fileSetRepository.get(bsn, version, properties, listeners);
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
		if (fileSetRepository == null) {
			return Collections.emptyList();
		}
		return fileSetRepository.list(pattern);
	}

	@Override
	public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
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
	public boolean refresh() throws Exception {
		return true;
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		if (fileSetRepository == null) {
			return new TreeSet<Version>();
		}
		return fileSetRepository.versions(bsn);
	}

	protected void createRepo(IMavenProjectFacade projectFacade, IProgressMonitor monitor) {
		MavenProject mavenProject = getMavenProject(projectFacade);
		try {
			BndrunContainer bndrunContainer = run.getPlugin(BndrunContainer.class);

			fileSetRepository = bndrunContainer.getFileSetRepository(mavenProject);
			fileSetRepository.list(null);

			Central.refreshPlugin(this);
		} catch (Exception e) {
			logger.logError("Failed to create implicit repository for m2e project " + getName(), e);

			try {
				String name = mavenProject.getName()
					.isEmpty() ? mavenProject.getArtifactId() : mavenProject.getName();

				fileSetRepository = new FileSetRepository(name, Collections.emptyList());

				Central.refreshPlugin(this);
			} catch (Exception e2) {
				throw Exceptions.duck(e2);
			}
		}
	}

}
