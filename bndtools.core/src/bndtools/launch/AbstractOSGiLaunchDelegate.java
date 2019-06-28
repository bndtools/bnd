package bndtools.launch;

import java.io.File;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.RunMode;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.Run;
import bndtools.Plugin;
import bndtools.StatusCode;
import bndtools.launch.util.LaunchUtils;
import bndtools.preferences.BndPreferences;

public abstract class AbstractOSGiLaunchDelegate extends JavaLaunchDelegate {
	@SuppressWarnings("deprecation")
	private static final String		ATTR_LOGLEVEL	= LaunchConstants.ATTR_LOGLEVEL;
	private static final ILogger	logger			= Logger.getLogger(AbstractOSGiLaunchDelegate.class);

	protected Run					run;

	protected abstract ProjectLauncher getProjectLauncher() throws CoreException;

	protected abstract void initialiseBndLauncher(ILaunchConfiguration configuration, Project model) throws Exception;

	protected abstract IStatus getLauncherStatus();

	protected abstract RunMode getRunMode();

	@Override
	public boolean preLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor)
		throws CoreException {
		// override AbstractJavaLaunchConfigurationDelegate#preLaunchCheck, to
		// avoid loading the Java
		// project (which is not required when we are using a bndrun file).
		return true;
	}

	@Override
	protected IProject[] getBuildOrder(ILaunchConfiguration configuration, String mode) throws CoreException {
		return new IProject[0];
	}

	@Override
	protected IProject[] getProjectsForProblemSearch(ILaunchConfiguration configuration, String mode)
		throws CoreException {
		return new IProject[0];
	}

	@Override
	public IVMInstall getVMInstall(ILaunchConfiguration configuration) throws CoreException {
		IExecutionEnvironmentsManager eeMgr = JavaRuntime.getExecutionEnvironmentsManager();

		// Look for a matching JVM install from the -runee setting
		String runee = run.getRunee();
		if (runee != null) {
			IExecutionEnvironment ee = eeMgr.getEnvironment(runee);
			if (ee != null) {
				// Return the default VM for this EE if the user has selected
				// one
				IVMInstall defaultVm = ee.getDefaultVM();
				if (defaultVm != null)
					return defaultVm;

				IVMInstall[] compatibleVMs = ee.getCompatibleVMs();
				if (compatibleVMs != null && compatibleVMs.length > 0) {
					// Return the strictly compatible VM (i.e. perfect match) if
					// there is one
					for (IVMInstall vm : compatibleVMs) {
						if (ee.isStrictlyCompatible(vm))
							return vm;
					}

					// No strictly compatible VM, just return the last in the
					// list.
					return compatibleVMs[compatibleVMs.length - 1];
				}
			}
			throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, StatusCode.NoVMForEE.getCode(),
				"Could not find JRE installation matching Execution Environment: " + runee, null));
		}

		// Still nothing? Use the default JVM from the workspace.
		// Eclipse tries really hard to force you to set a default VM, but this
		// can still be null if the default has
		// been disposed somehow.
		IVMInstall defaultVm = JavaRuntime.getDefaultVMInstall();
		if (defaultVm != null) {
			return defaultVm;
		}

		// You still here?? The superclass will look into the Java project, if
		// the run file is in one.
		try {
			return super.getVMInstall(configuration);
		} catch (CoreException e) {
			// ignore
		}

		throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, StatusCode.NoVMForEE.getCode(),
			"Could not select a JRE for launch. No Execution Environment is specified\n(using '-runee'), there is no default JRE in preferences and no relevant\nJava project settings.",
			null));
	}

	@Override
	public String[][] getBootpathExt(ILaunchConfiguration configuration) throws CoreException {
		// TODO: support deriving bootclasspath extensions from the bndrun file
		return new String[][] {
			null, null, null
		};
	}

	@Override
	public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor)
		throws CoreException {
		BndPreferences prefs = new BndPreferences();
		boolean result = !prefs.getBuildBeforeLaunch() || super.buildForLaunch(configuration, mode, monitor);

		try {
			run = LaunchUtils.createRun(configuration, getRunMode());

			initialiseBndLauncher(configuration, run);
		} catch (Exception e) {
			throw new CoreException(
				new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error initialising bnd launcher", e));
		}

		return result;
	}

	@Override
	public boolean finalLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor)
		throws CoreException {
		// Check for existing launches of same resource
		BndPreferences prefs = new BndPreferences();
		if (prefs.getWarnExistingLaunches()) {
			IResource launchResource = LaunchUtils.getTargetResource(configuration);
			if (launchResource == null)
				throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
					"Bnd launch target was not specified or does not exist.", null));

			int processCount = 0;
			for (ILaunch l : DebugPlugin.getDefault()
				.getLaunchManager()
				.getLaunches()) {
				// ... is it the same launch resource?
				ILaunchConfiguration launchConfig = l.getLaunchConfiguration();
				if (launchConfig == null) {
					continue;
				}
				if (launchResource.equals(LaunchUtils.getTargetResource(launchConfig))) {
					// Iterate existing processes
					for (IProcess process : l.getProcesses()) {
						if (!process.isTerminated())
							processCount++;
					}
				}
			}

			// Warn if existing processes running
			if (processCount > 0) {
				Status status = new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0,
					"One or more OSGi Frameworks have already been launched for this configuration. Additional framework instances may interfere with each other due to the shared storage directory.",
					null);
				IStatusHandler prompter = DebugPlugin.getDefault()
					.getStatusHandler(status);
				if (prompter != null) {
					boolean okay = (Boolean) prompter.handleStatus(status, launchResource);
					if (!okay)
						return okay;
				}
			}
		}

		IStatus launchStatus = getLauncherStatus();

		IStatusHandler prompter = DebugPlugin.getDefault()
			.getStatusHandler(launchStatus);
		if (prompter != null)
			return (Boolean) prompter.handleStatus(launchStatus, run);
		return true;
	}

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, final ILaunch launch, IProgressMonitor monitor)
		throws CoreException {
		// Register listener to clean up temp files on exit of launched JVM
		final ProjectLauncher launcher = getProjectLauncher();
		IDebugEventSetListener listener = new IDebugEventSetListener() {
			@Override
			public void handleDebugEvents(DebugEvent[] events) {
				for (DebugEvent event : events) {
					if (event.getKind() == DebugEvent.TERMINATE) {
						Object source = event.getSource();
						if (source instanceof IProcess) {
							ILaunch processLaunch = ((IProcess) source).getLaunch();
							if (processLaunch == launch) {
								// Not interested in any further events =>
								// unregister this listener
								DebugPlugin.getDefault()
									.removeDebugEventListener(this);

								// Cleanup. Guard with a draconian catch because
								// changes in the ProjectLauncher API
								// *may* cause LinkageErrors.
								try {
									launcher.cleanup();
								} catch (Throwable t) {
									logger.logError("Error cleaning launcher temporary files", t);
								}

								LaunchUtils.endRun((Run) launcher.getProject());
							}
						}
					}
				}
			}
		};
		DebugPlugin.getDefault()
			.addDebugEventListener(listener);

		// Now actually launch
		super.launch(configuration, mode, launch, monitor);
	}

	/*
	 * This method is deprecated in Eclipse 4.11 and no longer called there.
	 * Instead getClasspathAndModulepath is called. We need it here for older
	 * versions of Eclipse.
	 */
	@Override
	public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
		Collection<String> paths = getProjectLauncher().getClasspath();
		return paths.toArray(new String[0]);
	}

	/*
	 * This method has taken over getClasspath in 4.11. See
	 * https://github.com/eclipse/eclipse.jdt.debug/commit/
	 * 89530b29cb538a73f57f4b32cdf3f543258b9bc6#diff-
	 * 9010920d00bc21b4c2749643470ff0cfL95 See
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=529435
	 * https://github.com/bndtools/bnd/issues/2947
	 */
	@Override
	public String[][] getClasspathAndModulepath(ILaunchConfiguration config) throws CoreException {
		String[][] classpathAndModulepath = super.getClasspathAndModulepath(config);
		if (classpathAndModulepath == null) {
			classpathAndModulepath = new String[2][];
			classpathAndModulepath[1] = new String[0];
		}
		classpathAndModulepath[0] = getClasspath(config);
		return classpathAndModulepath;
	}

	@Override
	public String getMainTypeName(ILaunchConfiguration configuration) throws CoreException {
		return getProjectLauncher().getMainTypeName();
	}

	@Override
	public File verifyWorkingDirectory(ILaunchConfiguration configuration) throws CoreException {
		try {
			return (run != null) ? run.getBase() : null;
		} catch (Exception e) {
			throw new CoreException(
				new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error getting working directory for Bnd project.", e));
		}
	}

	@Override
	public String getVMArguments(ILaunchConfiguration configuration) throws CoreException {
		StringBuilder builder = new StringBuilder();
		Collection<String> runVM = getProjectLauncher().getRunVM();
		for (Iterator<String> iter = runVM.iterator(); iter.hasNext();) {
			builder.append(iter.next());
			if (iter.hasNext())
				builder.append(" ");
		}
		String args = builder.toString();
		return args;
	}

	@Override
	public String getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
		StringBuilder builder = new StringBuilder();

		Collection<String> args = getProjectLauncher().getRunProgramArgs();
		for (Iterator<String> iter = args.iterator(); iter.hasNext();) {
			builder.append(iter.next());
			if (iter.hasNext())
				builder.append(" ");
		}

		return builder.toString();
	}

	protected static void enableTraceOptionIfSetOnConfiguration(ILaunchConfiguration configuration,
		ProjectLauncher launcher) throws CoreException {
		if (configuration.hasAttribute(LaunchConstants.ATTR_TRACE)) {
			launcher.setTrace(configuration.getAttribute(LaunchConstants.ATTR_TRACE, LaunchConstants.DEFAULT_TRACE));
		}
		String logLevelStr = configuration.getAttribute(ATTR_LOGLEVEL, (String) null);
		if (logLevelStr != null) {
			Plugin.getDefault()
				.getLog()
				.log(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0,
					MessageFormat.format("The {0} attribute is no longer supported, use {1} instead.", ATTR_LOGLEVEL,
						LaunchConstants.ATTR_TRACE),
					null));
			Level logLevel = Level.parse(logLevelStr);
			launcher.setTrace(launcher.getTrace() || logLevel.intValue() <= Level.FINE.intValue());
		}
	}

	protected static MultiStatus createStatus(String message, List<String> errors, List<String> warnings) {
		MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, message, null);

		for (String error : errors) {
			status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, error, null));
		}
		for (String warning : warnings) {
			status.add(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0, warning, null));
		}

		return status;
	}
}
