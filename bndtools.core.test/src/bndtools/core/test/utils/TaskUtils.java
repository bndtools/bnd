package bndtools.core.test.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;

import aQute.lib.exceptions.Exceptions;

public class TaskUtils {

	static IResourceVisitor VISITOR = resource -> {
		IPath path = resource.getFullPath();
		log(path == null ? "null" : path.toString());
		return true;
	};

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

	public static void dumpWorkspace() {
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		try {
		ws.getRoot()
			.accept(VISITOR);
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
	}
}
