package aQute.bnd.util.repository;

import java.io.File;

import org.osgi.util.promise.Failure;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;

import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin.DownloadListener;
import aQute.libg.reporter.slf4j.Slf4jReporter;
import aQute.service.reporter.Reporter;

/**
 * Uses promises to signal the Download Listener from {@link RepositoryPlugin}
 */
public class DownloadListenerPromise implements Success<File,Void>, Failure {
	DownloadListener	dls[];
	Promise<File>		promise;
	private Reporter	reporter;
	private String		task;

	/**
	 * Use the promise to signal the Download Listeners
	 * 
	 * @param reporter a reporter or null (will use a SLF4 in that case)
	 * @param task
	 * @param promise
	 * @param downloadListeners
	 */
	public DownloadListenerPromise(Reporter reporter, String task, Promise<File> promise,
			DownloadListener... downloadListeners) {
		this.reporter = Slf4jReporter.getAlternative(DownloadListenerPromise.class, reporter);
		this.task = task;
		this.promise = promise;
		this.dls = downloadListeners;
		reporter.trace("%s: starting", task);
		promise.then(this, this);
	}

	@Override
	public Promise<Void> call(Promise<File> resolved) throws Exception {
		reporter.trace("%s: success", task);
		for (DownloadListener dl : dls) {
			try {
				dl.success(resolved.getValue());
			} catch (Throwable e) {
				reporter.warning("%s: Success callback failed to %s: %s", this, dl, e);
			}
		}
		return null;
	}

	@Override
	public void fail(Promise< ? > resolved) throws Exception {
		reporter.trace("%s: fail", task);
		for (DownloadListener dl : dls) {
			try {
				reporter.warning("%s: Download failed: %s", this, resolved.getFailure());
			} catch (Throwable e) {
				e.printStackTrace(); // TODO
				reporter.warning("%s: Fail callback failed to %s: %s", this, dl, e);
			}
		}
	}

	@Override
	public String toString() {
		return task;
	}
}