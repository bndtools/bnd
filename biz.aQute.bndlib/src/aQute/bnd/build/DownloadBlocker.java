package aQute.bnd.build;

import java.io.*;

import aQute.bnd.service.*;
import aQute.service.reporter.*;

public class DownloadBlocker implements RepositoryPlugin.DownloadListener {
	public enum Stage {
		INIT, SUCCESS, FAILURE
	};

	private volatile Stage	stage	= Stage.INIT;
	private String			failure;
	private File			file;
	final Reporter			reporter;

	public DownloadBlocker(Reporter reporter) {
		this.reporter = reporter;
	}

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

	public boolean progress(File file, int percentage) throws Exception {
		assert stage == Stage.INIT;
		return true;
	}

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

	public Stage getStage() {
		return stage;
	}

	public String toString() {
		return "DownloadBlocker(" + stage + "," + file + ", " + failure + ")";
	}
}
