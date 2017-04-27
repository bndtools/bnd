package aQute.bnd.util.repository;

import java.io.File;
import java.io.FileNotFoundException;

import org.osgi.util.promise.Failure;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin.DownloadListener;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.libg.reporter.slf4j.Slf4jReporter;
import aQute.service.reporter.Reporter;

/**
 * Uses promises to signal the Download Listener from {@link RepositoryPlugin}
 */
public class DownloadListenerPromise implements Success<File,Void>, Failure {
	private final static Logger	logger	= LoggerFactory.getLogger(DownloadListenerPromise.class);
	final DownloadListener		dls[];
	final Promise<File>			promise;
	private final Reporter		reporter;
	private final String		task;
	private File		linked;

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
		logger.debug("{}: starting", task);
		promise.then(this).then(null, this);
	}

	@Override
	public Promise<Void> call(Promise<File> resolved) throws Exception {
		File file = resolved.getValue();
		if (file == null) {
			throw new FileNotFoundException("Download failed");
		}
		logger.debug("{}: success {}", this, file);

		if (linked != null) {
			IO.createSymbolicLinkOrCopy(linked, file);
		}

		for (DownloadListener dl : dls) {
			try {
				dl.success(file);
			} catch (Throwable e) {
				reporter.warning("%s: Success callback failed to %s: %s", this, dl, e);
			}
		}
		return null;
	}

	@Override
	public void fail(Promise< ? > resolved) throws Exception {
		Throwable failure = resolved.getFailure();
		logger.debug("{}: failure", this, failure);
		String reason = Exceptions.toString(failure);
		for (DownloadListener dl : dls) {
			try {
				dl.failure(null, reason);
			} catch (Throwable e) {
				reporter.warning("%s: Fail callback failed to %s: %s", this, dl, e);
			}
		}
	}

	@Override
	public String toString() {
		return task;
	}

	public void linkTo(File linked) {
		this.linked = linked;
	}
}
