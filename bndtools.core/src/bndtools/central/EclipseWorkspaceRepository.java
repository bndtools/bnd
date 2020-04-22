package bndtools.central;

import static aQute.lib.exceptions.SupplierWithException.asSupplierOrElse;
import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.repository.AbstractIndexingRepository;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.lib.io.IO;
import biz.aQute.resolve.WorkspaceRepositoryMarker;

public class EclipseWorkspaceRepository extends AbstractIndexingRepository<IProject> implements WorkspaceRepositoryMarker {
	EclipseWorkspaceRepository() {
		super();
		Central.onWorkspace(this::initialize);
	}

	private void initialize(Workspace workspace) throws Exception {
		List<IProject> projects = Arrays.stream(ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProjects())
			.filter(this::isValid)
			.collect(toList());
		for (IProject project : projects) {
			Project model = Central.getProject(project);
			File target = model.getTargetDir();
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
	protected boolean isValid(IProject project) {
		try {
			return project.isOpen() && (Central.getProject(project) != null);
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	protected Consumer<ResourceBuilder> customizer(IProject project) {
		// Add a capability specific to the workspace so that we can
		// identify this fact later during resource processing.
		String name = project.getName();
		return rb -> rb.addWorkspaceNamespace(name);
	}

	@Override
	public String toString() {
		return NAME;
	}
}
