package bndtools;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import aQute.bnd.build.Project;

public class RepositoryIndexerJob extends Job {

    private static final AtomicReference<Job> jobRef = new AtomicReference<Job>(null);

    private RepositoryIndexerJob(String name) {
        super(name);
    }

    public static void runIfNeeded() {
        IProject cnfProject = ResourcesPlugin.getWorkspace().getRoot().getProject(Project.BNDCNF);
        IFile repoFile = cnfProject.getFile("repository.xml");
        if (!repoFile.exists()) {
            RepositoryIndexerJob job = new RepositoryIndexerJob("Indexing repositories...");
            job.setSystem(true);
            job.addJobChangeListener(new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    jobRef.set(null);
                }
            });

            if (jobRef.compareAndSet(null, job)) {
                job.schedule();
            }
        }
    }

    public static void joinRunningInstance(IProgressMonitor monitor) throws InterruptedException {
        Job job = jobRef.get();

        if (job == null)
            return;

        job.join();
        monitor.done();
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            LocalRepositoryTasks.indexRepositories(monitor);
            return Status.OK_STATUS;
        } catch (Exception e) {
            return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error indexing local repositories.", e);
        }
    }


}
