package org.bndtools.builder.indexer;

import static aQute.lib.exceptions.FunctionWithException.asFunction;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.build.api.AbstractBuildListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.osgi.resource.Resource;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.resource.ResourceBuilder;
import bndtools.central.Central;
import bndtools.central.EclipseWorkspaceRepository;

public class BuiltBundleIndexer extends AbstractBuildListener {
	private final ILogger logger = Logger.getLogger(BuiltBundleIndexer.class);

	@Override
	public void builtBundles(final IProject project, IPath[] paths) {
		IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace()
			.getRoot();

		Set<File> files = new HashSet<>();
		for (IPath path : paths) {
			try {
				IFile ifile = wsroot.getFile(path);
				IPath location = ifile.getLocation();
				if (location != null)
					files.add(location.toFile());
			} catch (IllegalArgumentException e) {
				System.err.println("### Error processing path: " + path);
				e.printStackTrace();
			}
		}

		// Generate the index
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

			EclipseWorkspaceRepository workspaceRepo = Central.getEclipseWorkspaceRepository();
			workspaceRepo.update(project, resources);

			File indexFile = new File(model.getTarget(), EclipseWorkspaceRepository.INDEX_FILENAME);
			XMLResourceGenerator xmlResourceGenerator = new XMLResourceGenerator().name(name)
				.base(project.getLocation()
					.toFile()
					.toURI())
				.resources(resources);
			xmlResourceGenerator.save(indexFile);
			IFile indexPath = wsroot.getFile(Central.toPath(indexFile));
			indexPath.refreshLocal(IResource.DEPTH_ZERO, null);
			if (indexPath.exists())
				indexPath.setDerived(true, null);
		} catch (Exception e) {
			logger.logError(
				MessageFormat.format("Failed to generate index file for bundles in project {0}.", project.getName()),
				e);
		}
	}
}
