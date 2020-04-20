package bndtools.central;

import static aQute.lib.exceptions.FunctionWithException.asFunction;
import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.stream.MapStream;
import aQute.lib.io.IO;
import aQute.lib.memoize.Memoize;
import biz.aQute.resolve.WorkspaceRepositoryMarker;

public class EclipseWorkspaceRepository extends BaseRepository implements WorkspaceRepositoryMarker {
	private final static ILogger						logger			= Logger
		.getLogger(EclipseWorkspaceRepository.class);
	private static final String							NAME			= "Workspace";
	private static final String							INDEX_FILENAME	= ".index.xml";
	private final Map<IProject, Collection<Resource>>	repositories;
	private volatile Supplier<ResourcesRepository>		repository;

	/**
	 * Can only be instantiated within the package.
	 */
	EclipseWorkspaceRepository() {
		repositories = new ConcurrentHashMap<>();
		repository = Memoize.supplier(this::aggregate);
		Central.onWorkspace(this::setupProjects);
	}

	private void setupProjects(Workspace workspace) throws Exception {
		IProject[] projects = ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProjects();
		for (IProject project : projects) {
			Project model = Central.getProject(project);
			if (model != null) {
				File target = getTarget(model);
				File indexFile = new File(target, INDEX_FILENAME);
				if (indexFile.isFile()) {
					URI base = project.getLocation()
						.toFile()
						.toURI();
					List<Resource> resources = XMLResourceParser.getResources(indexFile, base);
					update(project, resources);
					continue;
				}
				File buildfiles = new File(target, Constants.BUILDFILES);
				if (buildfiles.isFile()) {
					List<File> files;
					try (BufferedReader rdr = IO.reader(buildfiles)) {
						files = rdr.lines()
							.map(line -> IO.getFile(target, line.trim()))
							.filter(File::isFile)
							.collect(toList());
					}
					index(project, files);
					continue;
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

	public void index(IProject project, Collection<File> files) {
		try {
			Project model = Central.getProject(project);
			String name = model.getName();
			List<Resource> resources = files.stream()
				.map(asFunction(file -> {
					ResourceBuilder rb = new ResourceBuilder();
					rb.addFile(file, file.toURI());
					// Add a capability specific to the workspace so that we can
					// identify this fact later during resource processing.
					rb.addWorkspaceNamespace(name);
					return rb.build();
				}))
				.collect(toList());

			update(project, resources);

			File indexFile = new File(model.getTarget(), INDEX_FILENAME);
			XMLResourceGenerator xmlResourceGenerator = new XMLResourceGenerator().name(name)
				.base(project.getLocation()
					.toFile()
					.toURI())
				.resources(resources);
			xmlResourceGenerator.save(indexFile);
			IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace()
				.getRoot();
			IFile indexPath = wsroot.getFile(Central.toPath(indexFile));
			indexPath.refreshLocal(IResource.DEPTH_ZERO, null);
			if (indexPath.exists())
				indexPath.setDerived(true, null);
		} catch (Exception e) {
			logger.logError(MessageFormat.format("Failed to index bundles in project {0}.", project.getName()), e);
		}
	}

	private void update(IProject project, Collection<Resource> resources) {
		repositories.put(project, resources);
		repository = Memoize.supplier(this::aggregate);
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
