package bndtools.launch.bnd;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jdt.internal.launching.JavaRemoteApplicationLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMConnector;
import org.eclipse.jdt.launching.JavaRuntime;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import bndtools.launch.TerminationListener;
import bndtools.launch.util.LaunchUtils;

/**
 * The link between the Eclipse launching subsystem and the bnd launcher. We bypass the standard Eclipse launching and
 * will always setup the launch as a bnd launch and attach the debugger if necessary. This has the advantage we can add
 * features in bnd and we are sure fidelity is maintained.
 */
public class NativeBndLaunchDelegate extends JavaRemoteApplicationLaunchConfigurationDelegate {
    private static final String DEFAULT_PORT = "17654";
    private static final String DEFAULT_HOST = "localhost";

    private static final ILogger logger = Logger.getLogger(NativeBndLaunchDelegate.class);

    private static final Pattern NUMMERIC_P = Pattern.compile("(\\d+)");
    private volatile LaunchThread launchThread;
    private ILaunch launch;

    /*
     * The Eclipse launch interface.
     */
    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor m) throws CoreException {
        try {

            IProgressMonitor monitor = m == null ? new NullProgressMonitor() : m;
            boolean debug = "debug".equals(mode);

            //
            // Keep so we can cancel it at any time
            //

            this.launch = launch;

            if (monitor.isCanceled()) {
                return;
            }

            try {
                monitor.beginTask("Launching bnd native mode", debug ? 4 : 1);

                Project model = LaunchUtils.getBndProject(configuration);
                if (model == null)
                    throw new LaunchException("Cannot locate model", IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_PROJECT);

                progress(monitor, "Getting project launcher");

                final ProjectLauncher projectLauncher = model.getProjectLauncher();

                if (debug) {
                    doDebug(configuration, launch, monitor, model, projectLauncher);
                } else {
                    launchThread = launch(projectLauncher);
                }

                if (monitor.isCanceled()) {
                    close();
                    return;
                }

            } catch (LaunchException ie) {
                abort(ie.getMessage(), null, ie.getErr());
            } finally {
                monitor.done();
            }
        } catch (Exception e) {
            close();
            logger.logError("Failed to initialize launcg", e);
            IStatus status = new Status(Status.ERROR, "", "launching native bnd", e);
            throw new CoreException(status);
        }
    }

    /*
     * Setup a debug session
     */
    private void doDebug(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor, Project model, final ProjectLauncher projectLauncher) throws CoreException, LaunchException, InterruptedException {
        progress(monitor, "Setting up debug launch for " + model);

        //
        // Make sure we die when the debugger dies
        //

        DebugPlugin.getDefault().addDebugEventListener(new TerminationListener(launch, new Runnable() {

            @Override
            public void run() {
                close();
            }

        }));

        progress(monitor, "Getting source locations for " + model);
        setDefaultSourceLocator(launch, configuration);

        progress(monitor, "Getting parameters for " + model);

        @SuppressWarnings("unchecked")
        Map<String,String> argMap = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, (Map<String,String>) null);
        @SuppressWarnings("deprecation")
        int connectTimeout = JavaRuntime.getPreferences().getInt(JavaRuntime.PREF_CONNECT_TIMEOUT);

        IVMConnector connector = getConnector(configuration);
        String port = model.getProperty("-runjdb", DEFAULT_PORT);
        String host = model.getProperty("-runjdbhost", DEFAULT_HOST);

        progress(monitor, "Validating parameters for " + model);

        if (argMap == null) {
            argMap = new HashMap<String,String>();
        }

        if (connectTimeout < 5000)
            connectTimeout = 5000;

        if (!NUMMERIC_P.matcher(port).matches()) {
            throw new LaunchException("-runjdb is set but not to an integer " + port, IJavaLaunchConfigurationConstants.ERR_INVALID_PORT);
        }

        try {
            InetAddress.getByName(host);
        } catch (Exception e) {
            throw new LaunchException("Invalid hostname specified in -runjdbhost ", IJavaLaunchConfigurationConstants.ERR_INVALID_HOSTNAME);
        }

        progress(monitor, "Setting parameters for " + model);

        argMap.put("port", port);
        argMap.put("hostname", host);
        argMap.put("timeout", Integer.toString(connectTimeout == 0 ? 30000 : connectTimeout)); //$NON-NLS-1$

        //
        // Make sure Java allows us to attach
        //

        projectLauncher.addRunVM("-Xdebug");
        projectLauncher.addRunVM(String.format("-Xrunjdwp:transport=dt_socket,server=y,address=%s", port));

        progress(monitor, "Initiating launch " + model);
        launchThread = launch(projectLauncher);

        progress(monitor, "Attaching debugger to " + host + ":" + port);

        tryConnect(launch, monitor, connector, argMap);
    }

    private void progress(IProgressMonitor monitor, String string) {
        monitor.worked(1);
        monitor.subTask(string);
    }

    private IVMConnector getConnector(ILaunchConfiguration configuration) throws CoreException, LaunchException {
        String connectorId = getVMConnectorId(configuration);
        IVMConnector connector = null;
        if (connectorId == null) {
            connector = JavaRuntime.getDefaultVMConnector();
        } else {
            connector = JavaRuntime.getVMConnector(connectorId);
        }
        if (connector == null)
            throw new LaunchException("Cannot locate connector for connectorId " + connectorId, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_PROJECT);

        return connector;
    }

    private void tryConnect(ILaunch launch, IProgressMonitor monitor, IVMConnector connector, Map<String,String> argMap) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10000;

        do {

            if (monitor.isCanceled())
                break;

            try {
                connector.connect(argMap, monitor, launch);
            } catch (Exception e) {
                Thread.sleep(500);
            }

        } while (System.currentTimeMillis() < deadline);
    }

    private void close() {
        try {
            if (launchThread != null)
                launchThread.close();

            IDebugTarget[] debugTargets = launch.getDebugTargets();
            for (int i = 0; i < debugTargets.length; i++) {
                IDebugTarget target = debugTargets[i];
                if (target.canDisconnect()) {
                    target.disconnect();
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private LaunchThread launch(ProjectLauncher projectLauncher) {
        LaunchThread lt = new LaunchThread(projectLauncher);
        lt.start();
        return lt;
    }

}
