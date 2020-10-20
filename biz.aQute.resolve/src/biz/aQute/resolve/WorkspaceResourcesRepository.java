package biz.aQute.resolve;

import static aQute.lib.exceptions.SupplierWithException.asSupplierOrElse;
import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.File;
import java.util.Collections;
import java.util.function.BiFunction;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.repository.AbstractIndexingRepository;
import aQute.bnd.osgi.repository.WorkspaceRepositoryMarker;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.lib.io.IO;

public class WorkspaceResourcesRepository extends AbstractIndexingRepository<Project, File>
	implements WorkspaceRepositoryMarker {
	private final Workspace workspace;

	public WorkspaceResourcesRepository(Workspace workspace) {
		super();
		this.workspace = workspace;
		initialize();
	}

	private void initialize() {
		for (Project project : workspace.getAllProjects()) {
			File target = project.getTargetDir();
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
			}
		}
	}

	@Override
	protected boolean isValid(Project project) {
		return workspace.isPresent(project.getName()) && project.isValid();
	}

	@Override
	protected BiFunction<ResourceBuilder, File, ResourceBuilder> indexer(Project project) {
		String name = project.getName();
		return (rb, file) -> {
			rb = fileIndexer(rb, file);
			if (rb == null) {
				return null; // file is not a file
			}
			// Add a capability specific to the workspace so that we can
			// identify this fact later during resource processing.
			rb.addWorkspaceNamespace(name);
			return rb;
		};
	}

	@Override
	public String toString() {
		return NAME;
	}
}
