package org.bndtools.core.jobs;

import java.io.File;
import java.io.OutputStream;
import java.util.Map;
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
import org.osgi.service.indexer.ResourceIndexer;

import aQute.lib.io.IO;
import bndtools.Plugin;

public class GenerateIndexJob extends Job {

    private final Set<File> files;
    private final File outputFile;
    private final Map<String,String> config;

    public GenerateIndexJob(Set<File> files, File outputFile, Map<String,String> config) {
        super("Generating index");
        this.files = files;
        this.outputFile = outputFile;
        this.config = config;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        SubMonitor progress = SubMonitor.convert(monitor);

        // Generate index
        try (OutputStream outputStream = IO.outputStream(outputFile)) {
            ResourceIndexer indexer = Plugin.getDefault().getResourceIndexer();
            indexer.index(files, outputStream, config);
        } catch (Exception e) {
            return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error indexing files.", e);
        }

        // Make eclipse aware of the new/changed resource
        final IWorkspace ws = ResourcesPlugin.getWorkspace();
        final IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
            @Override
            public void run(IProgressMonitor monitor) throws CoreException {
                IFile[] outputResources = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(outputFile.toURI());
                if (outputResources != null) {
                    for (IFile resource : outputResources) {
                        resource.refreshLocal(IResource.DEPTH_ZERO, monitor);
                    }
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
