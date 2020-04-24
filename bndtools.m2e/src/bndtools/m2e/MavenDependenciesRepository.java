package bndtools.m2e;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.maven.lib.resolve.BndrunContainer;
import aQute.bnd.maven.lib.resolve.DependencyResolver;
import aQute.bnd.service.RepositoryPlugin;

@Component(service = {
	IMavenProjectChangedListener.class, RepositoryPlugin.class
})
public class MavenDependenciesRepository extends MavenWorkspaceRepository {

	private final static Logger			logger	= LoggerFactory.getLogger(MavenDependenciesRepository.class);

	private final Set<String>			allScopes	= new HashSet<>(
		Arrays.asList("compile", "provided", "runtime", "system", "test"));
	private final MavenBndrunContainer	mbc		= new MavenBndrunContainer();

	@Override
	public String getName() {
		return "Maven Dependencies";
	}

	@Override
	Supplier<Set<File>> collect(IMavenProjectFacade projectFacade, IProgressMonitor monitor) {
		return () -> {
			try {
				Set<File> files = new HashSet<>();
				MavenProject mavenProject = projectFacade.getMavenProject(monitor);
				BndrunContainer bndrunContainer = mbc.getBndrunContainer(projectFacade, null, true, true, allScopes,
					monitor);
				DependencyResolver dependencyResolver = bndrunContainer.getDependencyResolver(mavenProject);
				files.addAll(dependencyResolver.resolve()
					.keySet());

				logger.debug("{}: Collected files {} for project {}", getName(), files, projectFacade.getFullPath());

				return files;
			} catch (Exception e) {
				logger.error("{}: Failed to collect files for project {}", getName(), projectFacade.getFullPath(), e);
				return Collections.emptySet();
			}
		};
	}

}
