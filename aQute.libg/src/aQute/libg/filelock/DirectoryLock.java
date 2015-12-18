package aQute.libg.filelock;

import java.io.File;

public class DirectoryLock {
	final File					lock;
	final long					timeout;
	final public static String	LOCKNAME	= ".lock";

	public DirectoryLock(File directory, long timeout) {
		this.lock = new File(directory, LOCKNAME);
		this.lock.deleteOnExit();
		this.timeout = timeout;
	}

	public void release() {
		lock.delete();
	}

	public void lock() throws InterruptedException {
		if (lock.mkdir())
			return;

		long deadline = System.currentTimeMillis() + timeout;
		while (System.currentTimeMillis() < deadline) {
			if (lock.mkdir())
				return;
			Thread.sleep(50);
		}
	}
}
