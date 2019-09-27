package bndtools.launch.bnd;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.jdt.launching.IVMConnector;
import org.eclipse.jdt.launching.JavaRuntime;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.RunSession;
import bndtools.launch.OSGiRunLaunchDelegate;
import bndtools.launch.UpdateGuard;

class LaunchThread extends Thread implements IProcess {
	private static final ILogger		logger		= Logger.getLogger(OSGiRunLaunchDelegate.class);
	private final ProjectLauncher		launcher;
	private final AtomicBoolean			terminated	= new AtomicBoolean(false);
	private final BundleContext			context		= FrameworkUtil.getBundle(LaunchThread.class)
		.getBundleContext();
	private final ILaunch				launch;
	private final Map<String, String>	attributes	= new HashMap<>();
	private int							exitValue;
	private BndStreamsProxy				sproxy;
	private final RunSession			session;

	LaunchThread(ProjectLauncher pl, RunSession session, ILaunch launch) {
		super("bnd::launch-" + pl.getProject());

		super.setDaemon(true);

		this.launcher = pl;
		this.launch = launch;
		this.session = session;

		attributes.put(IProcess.ATTR_PROCESS_TYPE, session.getName());
		attributes.put(IProcess.ATTR_PROCESS_LABEL, session.getLabel());
		attributes.put(IProcess.ATTR_CMDLINE, session.getLabel());

	}

	void doDebug(IProgressMonitor monitor) throws InterruptedException {
		monitor.setTaskName(
			"Connecting debugger " + session.getName() + " to " + session.getHost() + ":" + session.getJdb());

		Map<String, String> parameters = new HashMap<>();
		parameters.put("hostname", session.getHost());
		parameters.put("port", session.getJdb() + "");
		parameters.put("timeout", session.getTimeout() + "");
		IVMConnector connector = JavaRuntime.getDefaultVMConnector();

		while (!monitor.isCanceled()) {

			try {
				connector.connect(parameters, monitor, launch);
				break;
			} catch (Exception e) {
				Thread.sleep(500);
			}
		}
	}

	/**
	 * This is the reason for this thread. We launch the remote process and wait
	 * until it returns.
	 */

	@Override
	public void run() {
		fireCreationEvent();

		//
		// We wait for build changes. We never update during a build
		// and we will wait a bit after a build ends.
		//

		UpdateGuard guard = new UpdateGuard(context) {
			@Override
			protected void update() {
				LaunchThread.this.update();
			}
		};

		guard.open();

		try {
			exitValue = session.launch();
		} catch (Exception e) {
			logger.logWarning("Exception from launcher", e);
			e.printStackTrace();
		} finally {
			guard.close();
			terminate();
		}
	}

	private void update() {
		if (isTerminated())
			return;

		try {
			//
			// TODO Should use listener
			//

			launcher.update();
		} catch (Exception e) {
			logger.logWarning("Exception from update", e);
		}
		fireChangeEvent();
	}

	@Override
	public void terminate() {
		if (terminated.getAndSet(true))
			return;

		if (sproxy != null)
			sproxy.close();

		try {
			launcher.cancel();
		} catch (Exception e) {
			// ignore
		} finally {
			fireTerminateEvent();
		}
		IDebugTarget[] debugTargets = launch.getDebugTargets();
		for (int i = 0; i < debugTargets.length; i++) {
			IDebugTarget target = debugTargets[i];
			if (target.canDisconnect()) {
				try {
					target.disconnect();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter.equals(IProcess.class)) {
			return (T) this;
		}
		if (adapter.equals(IDebugTarget.class)) {
			ILaunch launch = getLaunch();
			IDebugTarget[] targets = launch.getDebugTargets();
			for (int i = 0; i < targets.length; i++) {
				if (this.equals(targets[i].getProcess())) {
					return (T) targets[i];
				}
			}
			return null;
		}
		if (adapter.equals(ILaunch.class)) {
			return (T) getLaunch();
		}
		if (adapter.equals(ILaunchConfiguration.class)) {
			return (T) getLaunch().getLaunchConfiguration();
		}
		return null;
	}

	@Override
	public boolean canTerminate() {
		return !isTerminated();
	}

	@Override
	public boolean isTerminated() {
		return terminated.get();
	}

	@Override
	public String getLabel() {
		return launcher.getProject()
			.toString();
	}

	@Override
	public ILaunch getLaunch() {
		return launch;
	}

	@Override
	public IStreamsProxy getStreamsProxy() {
		if (sproxy == null) {
			sproxy = new BndStreamsProxy(launcher, session);
		}
		return sproxy;
	}

	@Override
	public void setAttribute(String key, String value) {
		attributes.put(key, value);
	}

	@Override
	public String getAttribute(String key) {
		return attributes.get(key);
	}

	@Override
	public int getExitValue() throws DebugException {
		if (!terminated.get())
			throw new DebugException(new Status(IStatus.ERROR, BndtoolsConstants.CORE_PLUGIN_ID, ""));
		return exitValue;
	}

	/**
	 * Fires a creation event.
	 */
	protected void fireCreationEvent() {
		fireEvent(new DebugEvent(this, DebugEvent.CREATE));
	}

	/**
	 * Fires a terminate event.
	 */
	protected void fireTerminateEvent() {
		fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
	}

	/**
	 * Fires a change event.
	 */
	protected void fireChangeEvent() {
		fireEvent(new DebugEvent(this, DebugEvent.CHANGE));
	}

	/**
	 * Fires the given debug event.
	 *
	 * @param event debug event to fire
	 */
	protected void fireEvent(DebugEvent event) {
		DebugPlugin manager = DebugPlugin.getDefault();
		if (manager != null) {
			manager.fireDebugEventSet(new DebugEvent[] {
				event
			});
		}
	}

}
