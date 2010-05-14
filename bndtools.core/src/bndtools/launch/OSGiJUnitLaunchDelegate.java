package bndtools.launch;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import java.io.File;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.SocketUtil;

import aQute.bnd.build.Project;
import bndtools.Plugin;

public class OSGiJUnitLaunchDelegate extends OSGiLaunchDelegate {

    static final String ATTR_JUNIT_PORT = "org.eclipse.jdt.junit.PORT";

    private static final String BNDTOOLS_RUNTIME_JUNIT_BSN = LaunchConstants.JUNIT_PREFIX;

    int port = -1;

    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        String reporter = configuration.getAttribute(LaunchConstants.ATTR_JUNIT_REPORTER, LaunchConstants.DEFAULT_JUNIT_REPORTER);
        if("port".equals(reporter)) {
            // Find the JUnit port
            port = SocketUtil.findFreePort();
            launch.setAttribute(ATTR_JUNIT_PORT, Integer.toString(port));
        }
        super.launch(configuration, mode, launch, monitor);
    }

    @Override
    protected Properties generateLaunchProperties(ILaunchConfiguration configuration) throws CoreException {
        Properties props = super.generateLaunchProperties(configuration);

        String reporter = configuration.getAttribute(LaunchConstants.ATTR_JUNIT_REPORTER, LaunchConstants.DEFAULT_JUNIT_REPORTER);
        if("port".equals(reporter)) {
            props.setProperty(LaunchConstants.PROP_LAUNCH_JUNIT_REPORTER, "port:" + Integer.toString(port));
        } else {
            props.setProperty(LaunchConstants.PROP_LAUNCH_JUNIT_REPORTER, reporter);
        }

        String keepAlive = configuration.getAttribute(LaunchConstants.PROP_LAUNCH_JUNIT_KEEP_ALIVE, TRUE.toString());
        props.setProperty(LaunchConstants.PROP_LAUNCH_JUNIT_KEEP_ALIVE, keepAlive);

        // For testing, always clean the framework
        props.setProperty(LaunchConstants.PROP_LAUNCH_CLEAN, TRUE.toString());
        props.setProperty(LaunchConstants.PROP_LAUNCH_DYNAMIC_BUNDLES, FALSE.toString());
        props.setProperty(LaunchConstants.PROP_LAUNCH_SHUTDOWN_ON_ERROR, TRUE.toString());

        return props;
    }

    @Override
    protected Collection<String> calculateRunBundlePaths(Project model) throws CoreException {
        Collection<String> runBundles = super.calculateRunBundlePaths(model);

        File junitBundle = findBundle(model, BNDTOOLS_RUNTIME_JUNIT_BSN, "0");
        if(junitBundle == null)
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Could not find JUnit bundle {0}.", BNDTOOLS_RUNTIME_JUNIT_BSN), null));
        runBundles.add(junitBundle.getAbsolutePath());

        return runBundles;
    }
}