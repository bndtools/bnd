package org.bndtools.core.jobs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

import bndtools.central.Central;

public class JobUtil {

	public static Promise<IStatus> chainJobs(Job... jobs) {
		return chainJobs(0L, jobs);
	}

	public static Promise<IStatus> chainJobs(final long delay, Job... jobs) {
		// Shortcut when there are zero jobs
		if (jobs == null || jobs.length == 0)
			return Central.promiseFactory()
				.resolved(Status.OK_STATUS);

		final Deferred<IStatus> completion = Central.promiseFactory()
			.deferred();
		for (int i = 0; i < jobs.length; i++) {
			final Job currentJob = jobs[i];
			final Job nextJob = (i + 1 < jobs.length) ? jobs[i + 1] : null;

			currentJob.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					IStatus status = event.getResult();
					if (nextJob != null && status.isOK())
						// Current job succeeded -> schedule next job
						nextJob.schedule(delay);
					else
						// Current job failed or no next job -> resolve the
						// promise with the last status
						completion.resolve(status);
				}
			});
		}
		// Schedule the first job
		jobs[0].schedule(delay);
		return completion.getPromise();
	}

}
