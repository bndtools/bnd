package bndtools.central;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.bnd.stream.MapStream;
import aQute.lib.io.IO;
import aQute.lib.memoize.Memoize;
import biz.aQute.resolve.WorkspaceRepositoryMarker;

public class EclipseWorkspaceRepository extends BaseRepository implements WorkspaceRepositoryMarker {
	private static final String							NAME			= "Workspace";
	public static final String							INDEX_FILENAME	= ".index";
	private final Map<IProject, Collection<Resource>>	repositories;
	private final Supplier<ResourcesRepository>			repository;

	/**
	 * Can only be instantiated within the package.
	 */
	EclipseWorkspaceRepository() {
		repositories = new ConcurrentHashMap<>();
		repository = Memoize.supplier(this::aggregate, 500, TimeUnit.MILLISECONDS);
		Central.onWorkspace(this::setupProjects);
	}

	private void setupProjects(Workspace workspace) throws Exception {
		IProject[] projects = ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProjects();
		for (IProject project : projects) {
			Project model = Central.getProject(project);
			if (model != null) {
				File indexFile = new File(getTarget(model), INDEX_FILENAME);
				if (indexFile.isFile()) {
					URI base = project.getLocation()
						.toFile()
						.toURI();
					List<Resource> resources = XMLResourceParser.getResources(indexFile, base);
					update(project, resources);
				}
			}
		}
	}

	// This is equivalent to Project.getTarget0(). It gets the target dir
	// without a prepare, which would initialise the plugins too early.
	private File getTarget(Project project) throws IOException {
		File target = project.getTargetDir();
		if (!target.exists()) {
			IO.mkdirs(target);
			project.getWorkspace()
				.changedFile(target);
		}
		return target;
	}

	public void update(IProject project, Collection<Resource> resources) {
		repositories.put(project, resources);
	}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		Map<Requirement, Collection<Capability>> result = repository.get()
			.findProviders(requirements);
		return result;
	}

	private ResourcesRepository aggregate() {
		ResourcesRepository aggregate = MapStream.of(repositories)
			.filterKey(IProject::isOpen)
			.values()
			.flatMap(Collection::stream)
			.collect(ResourcesRepository.toResourcesRepository());
		return aggregate;
	}

	@Override
	public String toString() {
		return NAME;
	}
}
