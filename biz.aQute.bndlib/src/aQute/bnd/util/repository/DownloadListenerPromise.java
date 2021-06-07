package aQute.bnd.util.repository;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Map;

import org.osgi.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin.DownloadListener;
import aQute.lib.io.IO;
import aQute.libg.reporter.slf4j.Slf4jReporter;
import aQute.service.reporter.Reporter;

/**
 * Uses promises to signal the Download Listener from {@link RepositoryPlugin}
 */
public class DownloadListenerPromise {
	private final static Logger	logger	= LoggerFactory.getLogger(DownloadListenerPromise.class);
	private final String		task;
	private File				linked;

	/**
	 * Use the promise to signal the Download Listeners
	 *
	 * @param r a reporter or null (will use a SLF4 in that case)
	 * @param task
	 * @param promise
	 * @param attrs
	 * @param downloadListeners
	 */
	public DownloadListenerPromise(Reporter r, String task, Promise<File> promise, Map<String, String> attrs,
		DownloadListener... downloadListeners) {
		Reporter reporter = Slf4jReporter.getAlternative(DownloadListenerPromise.class, r);
		this.task = task;
		logger.debug("{}: starting", task);
		promise.thenAccept(file -> {
			if (file == null) {
				throw new FileNotFoundException(task);
			}
			logger.debug("{}: success {}", this, file);

			if (linked != null) {
				IO.createSymbolicLinkOrCopy(linked, file);
			}

			for (DownloadListener dl : downloadListeners) {
				try {
					dl.success(file, attrs);
				} catch (Throwable e) {
					reporter.warning("%s: Success callback failed to %s: %s", this, dl, e);
				}
			}
		})
			.onFailure(failure -> {
				logger.debug("{}: failure", this, failure);
				String reason = failure.toString();
				for (DownloadListener dl : downloadListeners) {
					try {
						dl.failure(null, reason);
					} catch (Throwable e) {
						reporter.warning("%s: Fail callback failed to %s: %s", this, dl, e);
					}
				}
			});
	}

	public DownloadListenerPromise(Reporter r, String task, Promise<File> promise,
		DownloadListener... downloadListeners) {
		this(r, task, promise, Collections.emptyMap(), downloadListeners);
	}

	@Override
	public String toString() {
		return task;
	}

	public void linkTo(File linked) {
		this.linked = linked;
	}
}
