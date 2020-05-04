package bndtools.m2e;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.service.RepositoryPlugin;

@Component(service = {
	IMavenProjectChangedListener.class, RepositoryPlugin.class
})
public class MavenDependenciesRepository extends MavenWorkspaceRepository {

	private final static Logger					logger		= LoggerFactory
		.getLogger(MavenDependenciesRepository.class);

	private final Set<String>			allScopes	= new HashSet<>(
		Arrays.asList("compile", "provided", "runtime", "system", "test"));

	@Override
	public String getName() {
		return "Maven Dependencies";
	}

	@Override
	Set<File> collect(IMavenProjectFacade projectFacade, IProgressMonitor monitor) {
		logger.debug("{}: Collecting files for project {}", getName(), projectFacade.getProject());

		Set<File> files = new HashSet<>();

		if (!isValid(projectFacade.getProject())) {
			logger.debug("{}: Project {} determined invalid", getName(), projectFacade.getProject());

			return files;
		}

		try {
			MavenBndrunContainer mavenBndrunContainer = MavenBndrunContainer.getBndrunContainer(projectFacade, null, true,
				true, allScopes, monitor);
			mavenBndrunContainer.resolve()
				.forEach(files::add);

			logger.debug("{}: Collected files {} for project {}", getName(), files, projectFacade.getProject());
		} catch (Exception e) {
			logger.error("{}: Failed to collect files for project {}", getName(), projectFacade.getProject(), e);
		}

		return files;
	}

}
