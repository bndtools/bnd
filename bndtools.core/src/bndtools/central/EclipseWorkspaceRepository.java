package bndtools.central;

import static aQute.lib.exceptions.FunctionWithException.asFunction;
import static aQute.lib.exceptions.SupplierWithException.asSupplierOrElse;
import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.stream.MapStream;
import aQute.lib.io.IO;
import aQute.lib.memoize.Memoize;
import biz.aQute.resolve.WorkspaceRepositoryMarker;

public class EclipseWorkspaceRepository extends BaseRepository implements WorkspaceRepositoryMarker {
	private final static ILogger											logger	= Logger
		.getLogger(EclipseWorkspaceRepository.class);
	private static final String												NAME	= "Workspace";
	private final Map<IProject, Supplier<? extends Collection<Resource>>>	resources;
	private volatile Supplier<ResourcesRepository>							repository;

	/**
	 * Can only be instantiated within the package.
	 */
	EclipseWorkspaceRepository() {
		resources = new ConcurrentHashMap<>();
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
				File buildfiles = new File(target, Constants.BUILDFILES);
				if (buildfiles.isFile()) {
					index(project, asSupplierOrElse(() -> {
						try (BufferedReader rdr = IO.reader(buildfiles)) {
							return rdr.lines()
								.map(line -> IO.getFile(target, line.trim()))
								.filter(File::isFile)
								.collect(toList());
						}
					}, Collections.emptyList()));
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
		index(project, () -> files);
	}

	public void index(IProject project, Supplier<? extends Collection<File>> files) {
		try {
			resources.keySet()
				.removeIf(p -> !p.isOpen());
			Project model = Central.getProject(project);
			if (model != null) {
				resources.put(project, Memoize.supplier(indexer(model.getName(), files)));
			} else {
				resources.remove(project);
			}
			repository = Memoize.supplier(this::aggregate);
		} catch (Exception e) {
			logger.logError(MessageFormat.format("Failed to index bundles in project {0}.", project.getName()), e);
		}
	}

	private Supplier<List<Resource>> indexer(String name, Supplier<? extends Collection<File>> files) {
		return () -> files.get()
			.stream()
			.filter(File::isFile)
			.map(asFunction(file -> {
				ResourceBuilder rb = new ResourceBuilder();
				rb.addFile(file, file.toURI());
				// Add a capability specific to the workspace so that we can
				// identify this fact later during resource processing.
				rb.addWorkspaceNamespace(name);
				return rb.build();
			}))
			.collect(toList());
	}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		Map<Requirement, Collection<Capability>> result = repository.get()
			.findProviders(requirements);
		return result;
	}

	private ResourcesRepository aggregate() {
		ResourcesRepository aggregate = MapStream.of(resources)
			.filterKey(IProject::isOpen)
			.values()
			.map(Supplier::get)
			.flatMap(Collection::stream)
			.collect(ResourcesRepository.toResourcesRepository());
		return aggregate;
	}

	@Override
	public String toString() {
		return NAME;
	}
}
