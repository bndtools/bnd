package bndtools.launch.bnd;

import java.io.File;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.build.api.BuildListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
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
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.bnd.service.RepositoryPlugin;
import bndtools.launch.OSGiRunLaunchDelegate;

class LaunchThread extends Thread implements IProcess {
    private static final int GRACE_PERIOD = 500;
    private static final ILogger logger = Logger.getLogger(OSGiRunLaunchDelegate.class);
    private final ProjectLauncher launcher;
    private final static Timer timer = new Timer(true);
    private TimerTask trigger;
    private final AtomicBoolean terminated = new AtomicBoolean(false);
    private final BundleContext context = FrameworkUtil.getBundle(LaunchThread.class).getBundleContext();
    private final ILaunch launch;
    private Map<String,String> attributes;
    private int exitValue;
    private final ServiceRegistration<BuildListener> buildListener;
    private final ServiceRegistration<RepositoryListenerPlugin> repositoryListener;
    private IStreamsProxy sproxy;

    LaunchThread(ProjectLauncher pl, ILaunch launch) {
        super("bnd::launch-" + pl.getProject());
        this.launch = launch;
        super.setDaemon(true);
        this.launcher = pl;

        //
        // We wait for build changes. We never update during a build
        // and we will wait a bit after a build ends.
        //

        buildListener = context.registerService(BuildListener.class, new BuildListener() {

            @Override
            public void buildStarting(IProject project) {
                off();
            }

            @Override
            public void builtBundles(IProject project, IPath[] paths) {
                on();
            }
        }, null);

        //
        // We also wait for repository changes, though they will generally cause a rebuild as well.
        //
        repositoryListener = context.registerService(RepositoryListenerPlugin.class, new RepositoryListenerPlugin() {

            @Override
            public void repositoryRefreshed(RepositoryPlugin repository) {
                on();
            }

            @Override
            public void repositoriesRefreshed() {
                on();
            }

            @Override
            public void bundleRemoved(RepositoryPlugin repository, Jar jar, File file) {
                on();
            }

            @Override
            public void bundleAdded(RepositoryPlugin repository, Jar jar, File file) {
                on();
            }
        }, null);

    }

    /**
     * This is the reason for this thread. We launch the remote process and wait until it returns.
     */

    @Override
    public void run() {
        try {

            fireCreationEvent();
            exitValue = launcher.launch();
        } catch (Exception e) {
            logger.logWarning("Exception from launcher", e);
        } finally {
            terminate();
        }
    }

    private void update() {
        if (isTerminated())
            return;

        try {
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

        try {
            launcher.cancel();
            IDebugTarget[] debugTargets = launch.getDebugTargets();
            for (int i = 0; i < debugTargets.length; i++) {
                IDebugTarget target = debugTargets[i];
                if (target.canDisconnect()) {
                    target.disconnect();
                }
            }
        } catch (Exception e) {
            // ignore
        } finally {
            buildListener.unregister();
            repositoryListener.unregister();
            fireTerminateEvent();
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdapter(Class adapter) {
        if (adapter.equals(IProcess.class)) {
            return this;
        }
        if (adapter.equals(IDebugTarget.class)) {
            ILaunch launch = getLaunch();
            IDebugTarget[] targets = launch.getDebugTargets();
            for (int i = 0; i < targets.length; i++) {
                if (this.equals(targets[i].getProcess())) {
                    return targets[i];
                }
            }
            return null;
        }
        if (adapter.equals(ILaunch.class)) {
            return getLaunch();
        }
        if (adapter.equals(ILaunchConfiguration.class)) {
            return getLaunch().getLaunchConfiguration();
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
        return launcher.getProject().toString();
    }

    @Override
    public ILaunch getLaunch() {
        return launch;
    }

    @Override
    public IStreamsProxy getStreamsProxy() {
        if (sproxy == null)
            sproxy = new BndStreamsProxy(launcher);
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

    private void off() {
        synchronized (timer) {
            if (trigger != null)
                trigger.cancel();
            trigger = null;
        }
    }

    private void on() {
        synchronized (timer) {
            if (trigger != null)
                trigger.cancel();
            trigger = new TimerTask() {

                @Override
                public void run() {
                    update();
                }
            };
            timer.schedule(trigger, GRACE_PERIOD);
        }
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
     * @param event
     *            debug event to fire
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
