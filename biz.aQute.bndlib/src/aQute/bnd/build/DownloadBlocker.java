package aQute.bnd.build;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.service.RepositoryPlugin;
import aQute.service.reporter.Reporter;

/**
 * This class is intended to be used by the users of a {@link RepositoryPlugin}.
 * The
 * {@link RepositoryPlugin#get(String, aQute.bnd.version.Version, java.util.Map, aQute.bnd.service.RepositoryPlugin.DownloadListener...)}
 * method takes one or more Download Listeners. These are called back with the
 * success or failure of a download. This class is a simple implementation of
 * this model, just call {@link #getReason()} and it blocks until success or
 * failure is called.
 */
public class DownloadBlocker implements RepositoryPlugin.DownloadListener {
	private final static Logger logger = LoggerFactory.getLogger(DownloadBlocker.class);

	public enum Stage {
		INIT,
		SUCCESS,
		FAILURE
	}

	private volatile Stage			stage	= Stage.INIT;
	private String					failure;
	private File					file;
	private final Reporter			reporter;
	private final CountDownLatch	resolved;

	public DownloadBlocker(Reporter reporter) {
		this.reporter = reporter;
		resolved = new CountDownLatch(1);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * aQute.bnd.service.RepositoryPlugin.DownloadListener#success(java.io.File)
	 */
	@Override
	public void success(File file) throws Exception {
		synchronized (resolved) {
			if (resolved.getCount() == 0) {
				throw new IllegalStateException("already resolved");
			}
			assert stage == Stage.INIT;
			stage = Stage.SUCCESS;
			this.file = file;
			resolved.countDown();
		}
		logger.debug("successfully downloaded {}", file);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * aQute.bnd.service.RepositoryPlugin.DownloadListener#failure(java.io.File,
	 * java.lang.String)
	 */
	@Override
	public void failure(File file, String reason) throws Exception {
		synchronized (resolved) {
			if (resolved.getCount() == 0) {
				throw new IllegalStateException("already resolved");
			}
			assert stage == Stage.INIT;
			stage = Stage.FAILURE;
			this.failure = reason;
			this.file = file;
			resolved.countDown();
		}
		if (reporter != null)
			reporter.error("Download %s %s", reason, file);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * aQute.bnd.service.RepositoryPlugin.DownloadListener#progress(java.io.
	 * File, int)
	 */
	@Override
	public boolean progress(File file, int percentage) throws Exception {
		assert stage == Stage.INIT;
		return true;
	}

	/**
	 * Return a failure reason or null. This method will block until either
	 * {@link #success(File)} or {@link #failure(File, String)} has been called.
	 * It can be called many times.
	 *
	 * @return null or a reason for a failure
	 */
	public String getReason() {
		try {
			resolved.await();
			return failure;
		} catch (InterruptedException e) {
			return "Interrupted";
		}
	}

	/**
	 * Return the stage we're in
	 *
	 * @return the current stage
	 */
	public Stage getStage() {
		return stage;
	}

	public File getFile() {
		try {
			resolved.await();
			return file;
		} catch (InterruptedException e) {
			return null;
		}
	}

	@Override
	public String toString() {
		return "DownloadBlocker [stage=" + stage + ", failure=" + failure + ", file=" + file + "]";
	}
}
