package bndtools.launch;

import java.io.File;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

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
import org.eclipse.jdt.launching.JavaLaunchDelegate;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import bndtools.Logger;
import bndtools.Plugin;
import bndtools.api.ILogger;
import bndtools.launch.util.LaunchUtils;
import bndtools.preferences.BndPreferences;

public abstract class AbstractOSGiLaunchDelegate extends JavaLaunchDelegate {
    private static final ILogger logger = Logger.getLogger();

    protected Project model;

    protected abstract ProjectLauncher getProjectLauncher() throws CoreException;

    protected abstract void initialiseBndLauncher(ILaunchConfiguration configuration, Project model) throws Exception;

    protected abstract IStatus getLauncherStatus();

    @Override
    public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
        boolean result = super.buildForLaunch(configuration, mode, monitor);

        model = LaunchUtils.getBndProject(configuration);
        try {
            initialiseBndLauncher(configuration, model);
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error initialising bnd launcher", e));
        }

        return result;
    }

    @Override
    public boolean finalLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
        // Check for existing launches of same resource
        BndPreferences prefs = new BndPreferences();
        if (prefs.getWarnExistingLaunches()) {
            IResource launchResource = LaunchUtils.getTargetResource(configuration);
            if (launchResource == null)
                throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Bnd launch target was not specified or does not exist.", null));

            int processCount = 0;
            for (ILaunch l : DebugPlugin.getDefault().getLaunchManager().getLaunches()) {
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
                        "One or more OSGi Frameworks have already been launched for this configuration. Additional framework instances may interfere with each other due to the shared storage directory.", null);
                IStatusHandler prompter = DebugPlugin.getDefault().getStatusHandler(status);
                if (prompter != null) {
                    boolean okay = (Boolean) prompter.handleStatus(status, launchResource);
                    if (!okay)
                        return okay;
                }
            }
        }

        IStatus launchStatus = getLauncherStatus();

        IStatusHandler prompter = DebugPlugin.getDefault().getStatusHandler(launchStatus);
        if (prompter != null)
            return (Boolean) prompter.handleStatus(launchStatus, model);
        return true;
    }

    @Override
    public void launch(ILaunchConfiguration configuration, String mode, final ILaunch launch, IProgressMonitor monitor) throws CoreException {
        // Register listener to clean up temp files on exit of launched JVM
        final ProjectLauncher launcher = getProjectLauncher();
        IDebugEventSetListener listener = new IDebugEventSetListener() {
            public void handleDebugEvents(DebugEvent[] events) {
                for (DebugEvent event : events) {
                    if (event.getKind() == DebugEvent.TERMINATE) {
                        Object source = event.getSource();
                        if (source instanceof IProcess) {
                            ILaunch processLaunch = ((IProcess) source).getLaunch();
                            if (processLaunch == launch) {
                                // Not interested in any further events =>
                                // unregister this listener
                                DebugPlugin.getDefault().removeDebugEventListener(this);

                                // Cleanup. Guard with a draconian catch because
                                // changes in the ProjectLauncher API
                                // *may* cause LinkageErrors.
                                try {
                                    launcher.cleanup();
                                } catch (Throwable t) {
                                    logger.logError("Error cleaning launcher temporary files", t);
                                }
                            }
                        }
                    }
                }
            }
        };
        DebugPlugin.getDefault().addDebugEventListener(listener);

        // Now actually launch
        super.launch(configuration, mode, launch, monitor);
    }

    @Override
    public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
        Collection<String> paths = getProjectLauncher().getClasspath();
        return paths.toArray(new String[paths.size()]);
    }

    @Override
    public String getMainTypeName(ILaunchConfiguration configuration) throws CoreException {
        return getProjectLauncher().getMainTypeName();
    }

    @Override
    public File verifyWorkingDirectory(ILaunchConfiguration configuration) throws CoreException {
        try {
            return (model != null) ? model.getBase() : null;
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error getting working directory for Bnd project.", e));
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

        args = addJavaLibraryPath(configuration, args);
        return args;
    }

    @Override
    public String getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
        StringBuilder builder = new StringBuilder();

        Collection<String> args = getProjectLauncher().getArguments();
        for (Iterator<String> iter = args.iterator(); iter.hasNext();) {
            builder.append(iter.next());
            if (iter.hasNext())
                builder.append(" ");
        }

        return builder.toString();
    }

    protected String addJavaLibraryPath(ILaunchConfiguration configuration, String args) throws CoreException {
        String a = args;
        // Following code copied from AbstractJavaLaunchConfigurationDelegate
        int libraryPath = a.indexOf("-Djava.library.path"); //$NON-NLS-1$
        if (libraryPath < 0) {
            // if a library path is already specified, do not override
            String[] javaLibraryPath = getJavaLibraryPath(configuration);
            if (javaLibraryPath != null && javaLibraryPath.length > 0) {
                StringBuffer path = new StringBuffer(a);
                path.append(" -Djava.library.path="); //$NON-NLS-1$
                path.append("\""); //$NON-NLS-1$
                for (int i = 0; i < javaLibraryPath.length; i++) {
                    if (i > 0) {
                        path.append(File.pathSeparatorChar);
                    }
                    path.append(javaLibraryPath[i]);
                }
                path.append("\""); //$NON-NLS-1$
                a = path.toString();
            }
        }
        return a;
    }

    @SuppressWarnings("deprecation")
    protected static boolean enableTraceOption(ILaunchConfiguration configuration) throws CoreException {
        boolean trace = configuration.getAttribute(LaunchConstants.ATTR_TRACE, LaunchConstants.DEFAULT_TRACE);
        String logLevelStr = configuration.getAttribute(LaunchConstants.ATTR_LOGLEVEL, (String) null);
        if (logLevelStr != null) {
            Plugin.getDefault().getLog()
                    .log(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0, MessageFormat.format("The {0} attribute is no longer supported, use {1} instead.", LaunchConstants.ATTR_LOGLEVEL, LaunchConstants.ATTR_TRACE), null));
            Level logLevel = Level.parse(logLevelStr);
            trace |= logLevel.intValue() <= Level.FINE.intValue();
        }
        return trace;
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