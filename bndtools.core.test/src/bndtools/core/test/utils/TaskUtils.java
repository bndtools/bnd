package bndtools.core.test.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.jobs.Job;

import aQute.lib.exceptions.Exceptions;

public class TaskUtils {

	private TaskUtils() {}

	public static void log(String msg) {
		// System.err.println(System.currentTimeMillis() + ": " + msg);
	}

	public static void synchronously(String msg, MonitoredTask task) {
		try {
			String suffix = msg == null ? "" : ": " + msg;
			CountDownLatch flag = new CountDownLatch(1);
			log("Synchronously executing" + suffix);
			Job job = Job.create(msg, monitor -> {
				try {
					task.run(monitor);
				} catch (InvocationTargetException e) {
					throw Exceptions.duck(e.getTargetException());
				} catch (Exception e) {
					throw Exceptions.duck(e);
				} finally {
					flag.countDown();
				}
			});
			job.schedule();
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
