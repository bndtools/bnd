package bndtools.builder;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import aQute.bnd.build.Project;
import bndtools.Plugin;

public class BndBuildJob extends Job {

    private static final Map<String, Job> instances = new HashMap<String, Job>();

    public static void scheduleBuild(IFile bndFile, Project model, Set<File> deliverableJars) {
        BndBuildJob newJob = new BndBuildJob(bndFile, model, deliverableJars);

        replaceJob(bndFile.getFullPath().toString(), newJob);
        newJob.schedule();
    }

    private static void replaceJob(String key, Job newJob) {
        Job oldJob = null;
        synchronized (instances) {
            oldJob = instances.remove(key);
            instances.put(key, newJob);
        }
        if (oldJob != null) {
            final long cancelledTime = System.currentTimeMillis();
            JobChangeAdapter jobListener = new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    long timeToDie = System.currentTimeMillis() - cancelledTime;
                    String message = MessageFormat.format("---> Cancelled job took {0}ms to die.", timeToDie);

                    System.out.println(message);
                    if (timeToDie > 1000) {
                        Plugin.getDefault().getLog().log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, message, null));
                    }
                }
            };
            oldJob.addJobChangeListener(jobListener);
            oldJob.cancel();
        }
    }

    private final IFile bndFile;
    private final Project model;
    private final Set<File> deliverableJars;

    private BndBuildJob(IFile bndFile, Project model, Set<File> deliverableJars) {
        super("Bnd Build : " + bndFile.getFullPath().toString());
        this.bndFile = bndFile;

        this.model = model;
        this.deliverableJars = deliverableJars;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        SubMonitor progress = SubMonitor.convert(monitor, 2);

        final IWorkspace workspace = bndFile.getWorkspace();

        try {
            // Do the build
            if (Thread.interrupted()) return Status.CANCEL_STATUS;
            model.build();
            progress.worked(1);
            if (Thread.interrupted()) return Status.CANCEL_STATUS;

            final File targetDir = model.getTarget();
            IWorkspaceRunnable refreshAndDeleteAction = new IWorkspaceRunnable() {
                public void run(IProgressMonitor monitor) throws CoreException {
                    // Refresh target directory resources
                    IContainer target = workspace.getRoot().getContainerForLocation(new Path(targetDir.getAbsolutePath()));
                    target.refreshLocal(IResource.DEPTH_INFINITE, null);

                    // Clear any JARs in the target directory that have not just been built by Bnd
                    IResource[] members = target.members();
                    for (IResource targetFile : members) {
                        if (targetFile.getType() == IResource.FILE && "jar".equalsIgnoreCase(targetFile.getFileExtension())) {
                            File fsfile = new File(targetFile.getLocationURI());
                            if (!deliverableJars.contains(fsfile))
                                targetFile.delete(true, null);
                        }
                    }
                }
            };
            workspace.run(refreshAndDeleteAction, progress.newChild(1));

            // Report errors
            List<String> errors = new ArrayList<String>(model.getErrors());
            for (String errorMessage : errors) {
                IMarker marker = bndFile.createMarker(BndIncrementalBuilder.MARKER_BND_PROBLEM);
                marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
                marker.setAttribute(IMarker.MESSAGE, errorMessage);
                marker.setAttribute(IMarker.LINE_NUMBER, 1);
                model.clear();
            }

            return Status.OK_STATUS;
        } catch (InterruptedException e) {
            return Status.CANCEL_STATUS;
        } catch (Exception e) {
            return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error building Bnd project.", e);
        }
    }

}
