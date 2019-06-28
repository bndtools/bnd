package org.bndtools.builder.indexer;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.HashSet;
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
import org.osgi.resource.Capability;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.repository.SimpleIndexer;
import aQute.bnd.osgi.resource.CapabilityBuilder;
import aQute.lib.io.IO;
import bndtools.central.Central;
import bndtools.central.WorkspaceR5Repository;

public class BuiltBundleIndexer extends AbstractBuildListener {

	private static final String	INDEX_FILENAME	= ".index";

	private final ILogger		logger			= Logger.getLogger(BuiltBundleIndexer.class);

	@Override
	public void builtBundles(final IProject project, IPath[] paths) {
		IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace()
			.getRoot();
		final URI workspaceRootUri = wsroot.getLocationURI();

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

		// Generate the index file
		File indexFile;
		try {
			Project model = Central.getProject(project);
			File target = model.getTarget();
			indexFile = new File(target, INDEX_FILENAME);

			IFile indexPath = wsroot.getFile(Central.toPath(indexFile));

			new SimpleIndexer().files(files)
				.base(project.getLocation()
					.toFile()
					.toURI())
				.name(project.getName())
				.analyzer((f, rb) -> {
					Capability cap = new CapabilityBuilder("bndtools.workspace")
						.addAttribute("bndtools.workspace", workspaceRootUri.toString())
						.addAttribute("project.path", project.getFullPath()
							.toString())
						.buildSyntheticCapability();
					rb.addCapability(cap);
				})
				.index(indexFile);
			indexPath.refreshLocal(IResource.DEPTH_ZERO, null);
			if (indexPath.exists())
				indexPath.setDerived(true, null);
		} catch (Exception e) {
			logger.logError(
				MessageFormat.format("Failed to generate index file for bundles in project {0}.", project.getName()),
				e);
			return;
		}

		// Parse the index and add to the workspace repository
		try (InputStream input = IO.stream(indexFile)) {
			WorkspaceR5Repository workspaceRepo = Central.getWorkspaceR5Repository();
			workspaceRepo.loadProjectIndex(project, input, project.getLocation()
				.toFile()
				.toURI());
		} catch (Exception e) {
			logger.logError("Failed to update workspace index.", e);
		}
	}

}
