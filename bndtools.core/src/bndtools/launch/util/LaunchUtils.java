package bndtools.launch.util;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.RunListener;
import org.bndtools.api.RunMode;
import org.bndtools.api.RunProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import bndtools.central.Central;
import bndtools.launch.LaunchConstants;

public final class LaunchUtils {
	private static final ILogger							logger	= Logger.getLogger(LaunchUtils.class);

	private static ServiceTracker<RunListener, RunListener>	runListeners;
	private static ServiceTracker<RunProvider, RunProvider>	runProviders;

	private LaunchUtils() {}

	public static IResource getTargetResource(ILaunchConfiguration configuration) throws CoreException {
		String target = getTargetName(configuration);
		if (target == null)
			return null;

		IResource targetResource = ResourcesPlugin.getWorkspace()
			.getRoot()
			.findMember(target);
		return targetResource;
	}

	public static String getLaunchProjectName(IResource launchResource) {
		String result;

		IProject project = launchResource.getProject();
		Project bnd;
		try {
			bnd = Central.getWorkspace()
				.getProject(project.getName());
		} catch (Exception e) {
			bnd = null;
		}

		result = (bnd != null) ? bnd.getName() : Project.BNDCNF;
		return result;
	}

	public static Run createRun(ILaunchConfiguration configuration, RunMode mode) throws Exception {
		IResource targetResource = getTargetResource(configuration);
		if (targetResource == null) {
			String target = getTargetName(configuration);
			throw new IllegalArgumentException(String.format("The run descriptor '%s' could not be found.", target));
		}

		return createRun(targetResource, mode);
	}

	public static Run createRun(IResource targetResource, RunMode mode) throws Exception {
		Run run = null;
		for (RunProvider runProvider : getRunProviders()) {
			try {
				if ((run = runProvider.create(targetResource, mode)) != null) {
					break;
				}
			} catch (Throwable t) {
				logger.logError("Error in run listener", t);
			}
		}

		if (run == null) {
			throw new Exception(String.format("Cannot load Bnd project for directory %s: no Bnd workspace found",
				targetResource.getLocation()));
		}

		RunMode.set(run, mode);

		for (RunListener runListener : getRunListeners()) {
			try {
				runListener.create(run);
			} catch (Throwable t) {
				logger.logError("Error in run listener", t);
			}
		}

		return run;
	}

	private static String getTargetName(ILaunchConfiguration configuration) throws CoreException {
		String target = configuration.getAttribute(LaunchConstants.ATTR_LAUNCH_TARGET, (String) null);
		if (target != null && target.isEmpty()) {
			target = null;
		}
		return target;
	}

	public static void endRun(Run run) {
		for (RunListener runListener : getRunListeners()) {
			try {
				runListener.end(run);
			} catch (Throwable t) {
				logger.logError("Error in run listener", t);
			}
		}
	}

	private static synchronized RunListener[] getRunListeners() {
		if (runListeners == null) {
			final BundleContext context = FrameworkUtil.getBundle(LaunchUtils.class)
				.getBundleContext();

			if (context == null) {
				throw new IllegalStateException("Bundle context is null");
			}

			runListeners = new ServiceTracker<>(context, RunListener.class, null);
			runListeners.open();
		}

		return runListeners.getTracked()
			.values()
			.toArray(new RunListener[0]);
	}

	private static synchronized RunProvider[] getRunProviders() {
		if (runProviders == null) {
			final BundleContext context = FrameworkUtil.getBundle(LaunchUtils.class)
				.getBundleContext();

			if (context == null) {
				throw new IllegalStateException("Bundle context is null");
			}

			runProviders = new ServiceTracker<>(context, RunProvider.class, null);
			runProviders.open();
		}

		return runProviders.getTracked()
			.values()
			.toArray(new RunProvider[0]);
	}

	public static boolean isInBndWorkspaceProject(IResource resource) throws CoreException {
		if (resource == null) {
			return false;
		}

		IProject project = resource.getProject();

		return project.isOpen() && project.hasNature(BndtoolsConstants.NATURE_ID);
	}
}
