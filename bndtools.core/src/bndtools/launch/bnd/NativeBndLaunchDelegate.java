package bndtools.launch.bnd;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.internal.launching.JavaRemoteApplicationLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMConnector;
import org.eclipse.jdt.launching.JavaRuntime;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import bndtools.launch.util.LaunchUtils;

/**
 * The link between the Eclipse launching subsystem and the bnd launcher. We bypass the standard Eclipse launching and
 * will always setup the launch as a bnd launch and attach the debugger if necessary. This has the advantage we can add
 * features in bnd and we are sure fidelity is maintained.
 */
public class NativeBndLaunchDelegate extends JavaRemoteApplicationLaunchConfigurationDelegate {
    private static final String DEFAULT_PORT = "17654";
    private static final String DEFAULT_HOST = "localhost";

    //  private static final ILogger logger = Logger.getLogger(NativeBndLaunchDelegate.class);

    private static final Pattern NUMMERIC_P = Pattern.compile("(\\d+)");

    /*
     * The Eclipse launch interface.
     */
    @Override
    public void launch(ILaunchConfiguration configuration, String mode, final ILaunch launch, IProgressMonitor m) throws CoreException {
        IProgressMonitor monitor = m == null ? new NullProgressMonitor() : m;
        LaunchThread launchThread = null;

        try {
            try {

                final Project model = LaunchUtils.getBndProject(configuration);
                if (model == null)
                    throw new LaunchException("Cannot locate model", IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_PROJECT);

                ProjectLauncher projectLauncher = model.getProjectLauncher();
                if ("debug".equals(mode))
                    launchThread = doDebug(configuration, launch, monitor, model, projectLauncher);
                else
                    launchThread = launch(projectLauncher, launch);

                launch.addProcess(launchThread);

            } catch (LaunchException ie) {
                abort(ie.getMessage(), null, ie.getErr());
            }
        } catch (Exception e) {
            if (launchThread != null)
                launchThread.terminate();
            IStatus status = new Status(Status.ERROR, "", "launching native bnd", e);
            throw new CoreException(status);
        }
    }

    /*
     * Setup a debug session
     */
    @SuppressWarnings("unchecked")
    private LaunchThread doDebug(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor, Project model, final ProjectLauncher projectLauncher) throws CoreException, LaunchException, InterruptedException {
        setDefaultSourceLocator(launch, configuration);
        Map<String,String> argMap = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, (Map<String,String>) null);
        @SuppressWarnings("deprecation")
        int connectTimeout = JavaRuntime.getPreferences().getInt(JavaRuntime.PREF_CONNECT_TIMEOUT);

        IVMConnector connector = getConnector(configuration);
        String port = model.getProperty("-runjdb", DEFAULT_PORT);
        String host = model.getProperty("-runjdbhost", DEFAULT_HOST);

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

        argMap.put("port", port);
        argMap.put("hostname", host);
        argMap.put("timeout", Integer.toString(connectTimeout == 0 ? 30000 : connectTimeout)); //$NON-NLS-1$

        //
        // Make sure Java allows us to attach
        //

        projectLauncher.addRunVM("-Xdebug");
        projectLauncher.addRunVM(String.format("-Xrunjdwp:transport=dt_socket,server=y,address=%s", port));

        LaunchThread launchThread = launch(projectLauncher, launch);
        tryConnect(launch, monitor, connector, argMap);
        return launchThread;
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

    private LaunchThread launch(ProjectLauncher projectLauncher, ILaunch launch) {
        LaunchThread lt = new LaunchThread(projectLauncher, launch);
        lt.start();
        return lt;
    }
}
