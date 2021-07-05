package bndtools.core.test.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.bndtools.api.BndtoolsConstants;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import aQute.bnd.build.Workspace;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.exceptions.SupplierWithException;
import bndtools.central.Central;

public class TaskUtils {

	static IResourceVisitor VISITOR = resource -> {
		IPath path = resource.getFullPath();
		log(path == null ? "null" : path.toString());
		return true;
	};

	private TaskUtils() {}

	public static void log(SupplierWithException<String> msg) {
		try {
			log(msg.get());
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	public static void log(String msg) {
	//	System.err.println(System.currentTimeMillis() + ": " + msg);
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

	public static boolean setAutobuild(boolean on) {
		try {
			IWorkspace eclipse = ResourcesPlugin.getWorkspace();
			IWorkspaceDescription description = eclipse.getDescription();
			boolean original = description.isAutoBuilding();
			description.setAutoBuilding(on);
			eclipse.setDescription(description);
			return original;
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
	}

	// Synchronously build the workspace
	public static void buildFull() {
		try {
			ResourcesPlugin.getWorkspace()
				.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
	}

	public static void buildIncremental() {
		try {
			ResourcesPlugin.getWorkspace()
				.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new NullProgressMonitor());
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
	}

	public static void updateWorkspace(String context) throws Exception {
		log(context + ": Updating Bnd workspace");
		Workspace bndWs = Central.getWorkspace();
		bndWs.clear();
		bndWs.forceRefresh();
		bndWs.getPlugins();
	}

	public static void requestClasspathUpdate(String context) {
		try {
			log(context + ": Initiating classpath update");
			ClasspathContainerInitializer initializer = JavaCore
				.getClasspathContainerInitializer(BndtoolsConstants.BND_CLASSPATH_ID.segment(0));
			if (initializer != null) {
				IJavaModel javaModel = JavaCore.create(ResourcesPlugin.getWorkspace()
					.getRoot());

				for (IJavaProject project : javaModel.getJavaProjects()) {
					initializer.requestClasspathContainerUpdate(BndtoolsConstants.BND_CLASSPATH_ID, project, null);
				}
			}
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
		log(context + ": done classpath update");
	}

	public static void waitForBuild(String context) throws InterruptedException {
		log(context + ": Initiating build");
		buildIncremental();
		log(context + ": done waiting for build to complete");
	}
}
