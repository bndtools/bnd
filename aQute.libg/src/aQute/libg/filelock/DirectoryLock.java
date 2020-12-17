package aQute.libg.filelock;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class DirectoryLock {
	final File					lock;
	final long					timeout;
	final public static String	LOCKNAME	= ".lock";

	public DirectoryLock(File directory, long timeout) {
		this.lock = new File(directory, LOCKNAME);
		this.lock.deleteOnExit();
		this.timeout = TimeUnit.MILLISECONDS.toNanos(timeout);
	}

	public void release() {
		lock.delete();
	}

	public void lock() throws InterruptedException {
		if (lock.mkdir())
			return;

		final long startNanos = System.nanoTime();
		while ((System.nanoTime() - startNanos) < timeout) {
			if (lock.mkdir())
				return;
			Thread.sleep(50L);
		}
	}
}
