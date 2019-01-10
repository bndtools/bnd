package bndtools.launch.util;

import java.io.File;
import java.util.Optional;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.RunListener;
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
import aQute.bnd.build.Workspace;
import bndtools.central.Central;
import bndtools.launch.LaunchConstants;

public final class LaunchUtils {

    public static enum Mode {
        /*
         * Mode used when editing the run
         */
        EDIT,
        /*
         * Mode used when exporting the run
         */
        EXPORT,
        /*
         * Mode used when launching the run
         */
        LAUNCH,
        /*
         * This mode when locating source containers from the run
         */
        SOURCES,
        /*
         * Mode was not set on the run
         */
        UNSET;

        public static Mode getMode(Run run) {
            return Optional.ofNullable(run.get(LaunchUtils.Mode.class.getName()))
                .map(Mode::valueOf)
                .orElse(UNSET);
        }

        public static void setMode(Run run, Mode mode) {
            run.set(LaunchUtils.Mode.class.getName(), mode.name());
        }
    }

    private static final ILogger logger = Logger.getLogger(LaunchUtils.class);

    private static ServiceTracker<RunListener, RunListener> runListeners;

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

    public static Run createRun(ILaunchConfiguration configuration, Mode mode) throws Exception {
        IResource targetResource = getTargetResource(configuration);
        if (targetResource == null) {
            String target = getTargetName(configuration);
            throw new IllegalArgumentException(String.format("The run descriptor '%s' could not be found.", target));
        }

        return createRun(targetResource, mode);
    }

    public static Run createRun(IResource targetResource, Mode mode) throws Exception {
        Run run;
        Workspace ws = null;

        if (isInBndWorkspaceProject(targetResource)) {
            ws = Central.getWorkspaceIfPresent();
        }

        if (targetResource.getType() == IResource.PROJECT) {
            // This is a bnd project --> find the bnd.bnd file
            IProject project = (IProject) targetResource;
            if (ws == null)
                throw new Exception(String.format("Cannot load Bnd project for directory %s: no Bnd workspace found", project.getLocation()));
            File bndFile = project.getFile(Project.BNDFILE)
                .getLocation()
                .toFile();
            if (bndFile == null || !bndFile.isFile())
                throw new Exception(String.format("Failed to load Bnd project for directory %s: %s does not exist or is not a file.", project.getLocation(), Project.BNDFILE));

            run = Run.createRun(ws, bndFile);
        } else if (targetResource.getType() == IResource.FILE) {
            // This is file, use directly
            File file = targetResource.getLocation()
                .toFile();
            if (file == null || !file.isFile())
                throw new Exception(String.format("Failed to create Bnd launch configuration: %s does not exist or is not a file.", file));
            run = Run.createRun(ws, file);
        } else {
            throw new Exception(String.format("Cannot create a Bnd launch configuration for %s: not a project or file resource.", targetResource.getLocation()));
        }

        for (RunListener runListener : getRunListeners()) {
            try {
                runListener.create(run);
            } catch (Throwable t) {
                logger.logError("Error in run listener", t);
            }
        }

        Mode.setMode(run, mode);

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

            runListeners = new ServiceTracker<RunListener, RunListener>(context, RunListener.class, null);
            runListeners.open();
        }

        return runListeners.getTracked()
            .values()
            .toArray(new RunListener[0]);
    }

    public static boolean isInBndWorkspaceProject(IResource resource) throws CoreException {
        if (resource == null) {
            return false;
        }

        IProject project = resource.getProject();

        return project.isOpen() && project.hasNature(BndtoolsConstants.NATURE_ID);
    }
}
