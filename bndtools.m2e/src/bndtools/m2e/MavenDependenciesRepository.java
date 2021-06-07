package bndtools.m2e;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.unmodifiable.Sets;

@Component(service = {
	IMavenProjectChangedListener.class, RepositoryPlugin.class
})
public class MavenDependenciesRepository extends MavenWorkspaceRepository {

	private final static Logger	logger		= LoggerFactory.getLogger(MavenDependenciesRepository.class);

	private final Set<String>	allScopes	= Sets.of("compile", "provided", "runtime", "system", "test");

	@Override
	public String getName() {
		return "Maven Dependencies";
	}

	@Override
	Set<Artifact> collect(IMavenProjectFacade projectFacade, IProgressMonitor monitor) {
		logger.debug("{}: Collecting files for project {}", getName(), projectFacade.getProject());

		Set<Artifact> files = new HashSet<>();

		if (!isValid(projectFacade.getProject())) {
			logger.debug("{}: Project {} determined invalid", getName(), projectFacade.getProject());

			return files;
		}

		try {
			MavenBndrunContainer mavenBndrunContainer = MavenBndrunContainer.getBndrunContainer(projectFacade, null,
				true, true, allScopes, monitor);

			mavenBndrunContainer.resolve()
				.values()
				.stream()
				.map(this::fromArtifactResult)
				.forEach(files::add);

			logger.debug("{}: Collected artifacts {} for project {}", getName(), files, projectFacade.getProject());
		} catch (Exception e) {
			logger.error("{}: Failed to collect artifacts for project {}", getName(), projectFacade.getProject(), e);
		}

		return files;
	}

	private Artifact fromArtifactResult(ArtifactResult ar) {
		ArtifactRepository repository = ar.getRepository();
		String from = "";

		if (repository instanceof LocalRepository) {
			LocalRepository localRepository = (LocalRepository) repository;

			from = localRepository.getBasedir()
				.getAbsolutePath();
		} else if (repository instanceof RemoteRepository) {
			RemoteRepository remoteRepository = (RemoteRepository) repository;

			from = remoteRepository.getUrl();
		} else if (repository instanceof WorkspaceRepository) {
			WorkspaceRepository workspaceRepository = (WorkspaceRepository) repository;

			from = workspaceRepository.getId();
		}

		return ar.getArtifact()
			.setProperties(Collections.singletonMap("from", from));
	}

}
