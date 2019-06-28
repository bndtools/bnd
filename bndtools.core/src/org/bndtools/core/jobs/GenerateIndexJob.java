package org.bndtools.core.jobs;

import java.io.File;
import java.net.URI;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

import aQute.bnd.osgi.repository.SimpleIndexer;
import bndtools.Plugin;

public class GenerateIndexJob extends Job {

	private final Set<File>	files;
	private final File		outputFile;
	private final URI		base;
	private final boolean	compress;
	private final String	name;

	public GenerateIndexJob(Set<File> files, File outputFile, URI base, boolean compress, String name) {
		super("Generating index");
		this.files = files;
		this.outputFile = outputFile;
		this.base = base;
		this.compress = compress;
		this.name = name;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor);

		// Generate index
		try {
			new SimpleIndexer().files(files)
				.base(base)
				.compress(compress)
				.name(name)
				.index(outputFile);
		} catch (Exception e) {
			return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error indexing files.", e);
		}

		// Make eclipse aware of the new/changed resource
		final IWorkspace ws = ResourcesPlugin.getWorkspace();
		final IWorkspaceRunnable runnable = monitor1 -> {
			IFile[] outputResources = ResourcesPlugin.getWorkspace()
				.getRoot()
				.findFilesForLocationURI(outputFile.toURI());
			if (outputResources != null) {
				for (IFile resource : outputResources) {
					resource.refreshLocal(IResource.DEPTH_ZERO, monitor1);
				}
			}
		};
		try {
			ws.run(runnable, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
		} catch (CoreException e) {
			return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error refreshing workspace files.", e);
		}

		return Status.OK_STATUS;
	}

}
