package bndtools.core.test.utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import aQute.lib.exceptions.Exceptions;

public class TaskUtils {

	private TaskUtils() {}

	public static void log(String msg) {
		// System.err.println(System.currentTimeMillis() + ": " + msg);
	}

	public static IProgressMonitor countDownMonitor(CountDownLatch flag) {
		return new NullProgressMonitor() {
			@Override
			public void done() {
				flag.countDown();
			}
		};
	}

	public static void synchronously(String msg, MonitoredTask task) {
		try {
			String suffix = msg == null ? "" : ": " + msg;
			CountDownLatch flag = new CountDownLatch(1);
			log("Synchronously executing" + suffix);
			task.run(countDownMonitor(flag));
			log("Waiting for flag" + suffix);
			if (!flag.await(10000, TimeUnit.MILLISECONDS)) {
				log("WARN: timed out waiting for operation to finish" + suffix);
			} else {
				log("Finished waiting for flag" + suffix);
			}
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	public static void synchronously(MonitoredTask task) {
		synchronously(null, task);
	}
}
