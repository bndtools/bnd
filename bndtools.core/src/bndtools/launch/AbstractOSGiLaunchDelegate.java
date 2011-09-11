package bndtools.launch;

import java.io.File;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.JavaLaunchDelegate;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import bndtools.Plugin;

public abstract class AbstractOSGiLaunchDelegate extends JavaLaunchDelegate {

    protected void waitForBuilds(IProgressMonitor monitor) {
        /*
        SubMonitor progress = SubMonitor.convert(monitor, "Waiting for background Bnd builds to complete...", 1);
        try {
            Job.getJobManager().join(BndBuildJob.class, progress.newChild(1));
        } catch (OperationCanceledException e) {
            // Ignore
        } catch (InterruptedException e) {
            // Ignore
        }
        */
    }



    protected abstract ProjectLauncher getProjectLauncher() throws CoreException;

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
            Project project = LaunchUtils.getBndProject(configuration);
            return (project != null) ? project.getBase() : null;
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
            if (iter.hasNext()) builder.append(" ");
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
            if (iter.hasNext()) builder.append(" ");
        }

        return builder.toString();
    }

    protected String addJavaLibraryPath(ILaunchConfiguration configuration, String args) throws CoreException {
        // Following code copied from AbstractJavaLaunchConfigurationDelegate
        int libraryPath = args.indexOf("-Djava.library.path"); //$NON-NLS-1$
        if (libraryPath < 0) {
            // if a library path is already specified, do not override
            String[] javaLibraryPath = getJavaLibraryPath(configuration);
            if (javaLibraryPath != null && javaLibraryPath.length > 0) {
                StringBuffer path = new StringBuffer(args);
                path.append(" -Djava.library.path="); //$NON-NLS-1$
                path.append("\""); //$NON-NLS-1$
                for (int i = 0; i < javaLibraryPath.length; i++) {
                    if (i > 0) {
                        path.append(File.pathSeparatorChar);
                    }
                    path.append(javaLibraryPath[i]);
                }
                path.append("\""); //$NON-NLS-1$
                args = path.toString();
            }
        }
        return args;
    }
    @SuppressWarnings("deprecation")
    protected boolean enableTraceOption(ILaunchConfiguration configuration) throws CoreException {
        boolean trace = configuration.getAttribute(LaunchConstants.ATTR_TRACE, LaunchConstants.DEFAULT_TRACE);
        String logLevelStr = configuration.getAttribute(LaunchConstants.ATTR_LOGLEVEL, (String) null);
        if (logLevelStr != null) {
            Plugin.getDefault().getLog().log(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0,
                    MessageFormat.format("The {0} attribute is no longer supported, use {1} instead.", LaunchConstants.ATTR_LOGLEVEL, LaunchConstants.ATTR_TRACE), null));
            Level logLevel = Level.parse(logLevelStr);
            trace |= logLevel.intValue() <= Level.FINE.intValue();
        }
        return trace;
    }
}