package aQute.bnd.build;

import java.io.*;

import aQute.bnd.service.*;
import aQute.service.reporter.*;

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
	public enum Stage {
		INIT, SUCCESS, FAILURE
	};

	private volatile Stage	stage	= Stage.INIT;
	private String			failure;
	private File			file;
	private final Reporter	reporter;

	public DownloadBlocker(Reporter reporter) {
		this.reporter = reporter;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * aQute.bnd.service.RepositoryPlugin.DownloadListener#success(java.io.File)
	 */
	public void success(File file) throws Exception {
		synchronized (this) {
			assert stage == Stage.INIT;
			stage = Stage.SUCCESS;
			this.file = file;
			notifyAll();
		}
		if (reporter != null)
			reporter.trace("successfully downloaded %s", file);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * aQute.bnd.service.RepositoryPlugin.DownloadListener#failure(java.io.File,
	 * java.lang.String)
	 */
	public void failure(File file, String reason) throws Exception {
		synchronized (this) {
			assert stage == Stage.INIT;
			stage = Stage.FAILURE;
			this.failure = reason;
			this.file = file;
			notifyAll();
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
	public synchronized String getReason() {
		try {
			while (stage == Stage.INIT)
				wait();
		}
		catch (InterruptedException e) {
			return "Interrupted";
		}

		return failure;
	}

	/**
	 * Return the stage we're in
	 * 
	 * @return the current stage
	 */
	public Stage getStage() {
		return stage;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "DownloadBlocker(" + stage + "," + file + ", " + failure + ")";
	}
}
