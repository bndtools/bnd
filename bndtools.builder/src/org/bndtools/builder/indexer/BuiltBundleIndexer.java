package org.bndtools.builder.indexer;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.bndtools.build.api.AbstractBuildListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import bndtools.central.Central;
import bndtools.central.EclipseWorkspaceRepository;

public class BuiltBundleIndexer extends AbstractBuildListener {
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

		EclipseWorkspaceRepository workspaceRepo = Central.getEclipseWorkspaceRepository();
		workspaceRepo.index(project, files);
	}
}
