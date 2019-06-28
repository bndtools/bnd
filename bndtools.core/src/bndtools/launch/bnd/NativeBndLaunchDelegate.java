package bndtools.launch.bnd;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.internal.launching.JavaRemoteApplicationLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.Run;
import aQute.bnd.build.RunSession;
import aQute.bnd.osgi.Processor;
import aQute.lib.io.IO;
import bndtools.Plugin;
import bndtools.central.Central;
import bndtools.launch.LaunchConstants;
import bndtools.launch.ui.internal.LaunchStatusHandler;
import bndtools.launch.util.LaunchUtils;
import bndtools.preferences.BndPreferences;

/**
 * The link between the Eclipse launching subsystem and the bnd launcher. We
 * bypass the standard Eclipse launching and will always setup the launch as a
 * bnd launch and attach the debugger if necessary. This has the advantage we
 * can add features in bnd and we are sure fidelity is maintained.
 */
public class NativeBndLaunchDelegate extends JavaRemoteApplicationLaunchConfigurationDelegate {
	volatile boolean canceled = false;

	/*
	 * The Eclipse launch interface.
	 */
	@Override
	public void launch(ILaunchConfiguration configuration, String mode, final ILaunch launch, IProgressMonitor m)
		throws CoreException {
		final IProgressMonitor monitor = m == null ? new NullProgressMonitor() : m;

		Callable<Boolean> isCancelled = () -> canceled || monitor.isCanceled();

		Processor p = new Processor();

		try {

			monitor.setTaskName("Detecting if configuration is already launched");
			if (isAlreadyRunning(configuration)) {
				return;
			}

			String target = configuration.getAttribute(LaunchConstants.ATTR_LAUNCH_TARGET, (String) null);
			if (target == null || target.length() == 0) {
				p.error("No target specified in the launch configuration");
				return;
			}

			IResource targetResource = ResourcesPlugin.getWorkspace()
				.getRoot()
				.findMember(target);
			if (targetResource == null) {
				p.error("No actual resource found for " + target);
				return;
			}

			IProject parent = targetResource.getProject();
			if (parent == null) {
				p.error("Not part of a project " + targetResource);
				return;
			}

			Project parentModel = Central.getProject(parent);
			if (parentModel == null) {
				p.error("Cannot locate Bnd project for " + targetResource);
				return;
			}

			Project model;
			if (targetResource.getName()
				.equals(Project.BNDFILE)) {
				model = parentModel;
			} else {

				File file = targetResource.getLocation()
					.toFile();
				if (file == null || !file.isFile()) {
					p.error("No file associated with the entry " + targetResource);
					return;
				}

				model = new Run(parentModel.getWorkspace(), parentModel.getBase(), file);
			}

			monitor.setTaskName("Target is " + model);

			boolean debug = "debug".equals(mode);
			try {
				List<LaunchThread> lts = new ArrayList<>();
				ProjectLauncher projectLauncher = model.getProjectLauncher();
				try {

					List<? extends RunSession> sessions = projectLauncher.getRunSessions();
					if (sessions == null) {
						projectLauncher.error("This launcher for %s cannot handle the new style", target);
						return;
					}

					for (RunSession session : sessions)
						try {

							monitor.setTaskName("validating session " + session.getLabel());
							if (!session.validate(isCancelled)) {
								continue;
							}

							LaunchThread lt = new LaunchThread(projectLauncher, session, launch);

							if (debug) {
								lt.doDebug(monitor);
							}

							if (monitor.isCanceled())
								return;

							launch.addProcess(lt);
							lts.add(lt);

						} catch (Exception e) {
							projectLauncher.exception(e, "Starting session %s in project %s", session.getName(), model);
						}

				} catch (Exception e) {
					projectLauncher.exception(e, "starting processes");
				} finally {
					p.getInfo(projectLauncher);
				}

				if (!p.isOk()) {
					IStatus status = Central.toStatus(projectLauncher, "Errors detected during the launch");
					IStatusHandler prompter = DebugPlugin.getDefault()
						.getStatusHandler(status);
					Boolean cont = (Boolean) prompter.handleStatus(status, null);
					if (cont == null || !cont || monitor.isCanceled()) {
						launch.terminate();
						return;
					}
				}

				for (LaunchThread lt : lts) {
					lt.start();
				}

			} catch (Exception e) {
				launch.terminate();
				abort("Internal error", e, IJavaLaunchConfigurationConstants.ERR_INTERNAL_ERROR);
			}
		} catch (Exception e) {
			p.exception(e, "While starting a launch %s", configuration);
		} finally {
			if (!p.isOk()) {
				IStatus status = Central.toStatus(p, "Errors detected during the launch");
				IStatusHandler prompter = new LaunchStatusHandler();
				prompter.handleStatus(status, null);
				launch.terminate();
			}

			monitor.done();
			IO.close(p);
		}
	}

	/**
	 * Check if we already have a configuration running
	 */

	public boolean isAlreadyRunning(ILaunchConfiguration configuration) throws CoreException {
		// Check for existing launches of same resource
		BndPreferences prefs = new BndPreferences();
		if (prefs.getWarnExistingLaunches()) {
			IResource launchResource = LaunchUtils.getTargetResource(configuration);
			if (launchResource == null)
				return false;

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
					return !okay;
				}
			}
		}
		return false;
	}
}
