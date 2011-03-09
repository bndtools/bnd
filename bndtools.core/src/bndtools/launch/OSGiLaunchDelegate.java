package bndtools.launch;

import java.io.File;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.JavaLaunchDelegate;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.Workspace;
import bndtools.Central;
import bndtools.Plugin;
import bndtools.builder.BndBuildJob;
import bndtools.builder.BndProjectNature;

public class OSGiLaunchDelegate extends JavaLaunchDelegate {

    private ProjectLauncher bndLauncher = null;

    @Override
    public void launch(final ILaunchConfiguration configuration, String mode, final ILaunch launch, IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor, 2);

        waitForBuilds(progress.newChild(1, SubMonitor.SUPPRESS_NONE));

        try {
            Project project = getBndProject(configuration);
            synchronized (project) {
                bndLauncher = project.getProjectLauncher();
            }
            configureLauncher(configuration);
            bndLauncher.prepare();

            boolean dynamic = configuration.getAttribute(LaunchConstants.ATTR_DYNAMIC_BUNDLES, LaunchConstants.DEFAULT_DYNAMIC_BUNDLES);
            if (dynamic)
                registerLaunchPropertiesRegenerator(project, launch);
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error obtaining OSGi project launcher.", e));
        }


        super.launch(configuration, mode, launch, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
    }

    private void configureLauncher(ILaunchConfiguration configuration) throws CoreException {
        assertBndLauncher();

        boolean clean = configuration.getAttribute(LaunchConstants.ATTR_CLEAN, LaunchConstants.DEFAULT_CLEAN);
        bndLauncher.setKeep(!clean);

        boolean trace = enableTraceOption(configuration);
        bndLauncher.setTrace(trace);
    }

    protected boolean enableTraceOption(ILaunchConfiguration configuration) throws CoreException {
        Level logLevel = Level.parse(configuration.getAttribute(LaunchConstants.ATTR_LOGLEVEL, LaunchConstants.DEFAULT_LOGLEVEL));
        return logLevel.intValue() <= Level.FINE.intValue();
    }

    protected void waitForBuilds(IProgressMonitor monitor) {
        SubMonitor progress = SubMonitor.convert(monitor, "Waiting for background Bnd builds to complete...", 1);
        try {
            Job.getJobManager().join(BndBuildJob.class, progress.newChild(1));
        } catch (OperationCanceledException e) {
            // Ignore
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    /**
     * Registers a resource listener with the project model file to update the
     * launcher when the model or any of the run-bundles changes. The resource
     * listener is automatically unregistered when the launched process
     * terminates.
     *
     * @param project
     * @param launch
     * @throws CoreException
     */
    protected void registerLaunchPropertiesRegenerator(final Project project, final ILaunch launch) throws CoreException {
        final IPath propsPath = Central.toPath(project, project.getPropertiesFile());

        final IPath targetPath;
        try {
            targetPath = Central.toPath(project, project.getTarget());
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error querying project output folder", e));
        }
        final IResourceChangeListener resourceListener = new IResourceChangeListener() {
            public void resourceChanged(IResourceChangeEvent event) {
                try {
                    final AtomicBoolean update = new AtomicBoolean(false);

                    // Was the properties file (bnd.bnd or *.bndrun) included in the delta?
                    IResourceDelta propsDelta = event.getDelta().findMember(propsPath);
                    if (propsDelta != null) {
                        if (propsDelta.getKind() == IResourceDelta.CHANGED) {
                            update.set(true);
                        }
                    }

                    // Check for bundles included in the launcher's runbundles list
                    if (!update.get()) {
                        final Set<String> runBundleSet = new HashSet<String>(bndLauncher.getRunBundles());
                        event.getDelta().accept(new IResourceDeltaVisitor() {
                            public boolean visit(IResourceDelta delta) throws CoreException {
                                // Short circuit if we have already found a match
                                if (update.get())
                                    return false;

                                IResource resource = delta.getResource();
                                if (resource.getType() == IResource.FILE) {
                                    boolean isRunBundle = runBundleSet.contains(resource.getLocation().toPortableString());
                                    update.compareAndSet(false, isRunBundle);
                                    return false;
                                }

                                // Recurse into containers
                                return true;
                            }
                        });
                    }

                    // Was the target path included in the delta? This might mean that sub-bundles have changed
                    boolean targetPathChanged = event.getDelta().findMember(targetPath) != null;
                    update.compareAndSet(false, targetPathChanged);

                    if(update.get()) {
                        bndLauncher.update();
                    }
                } catch (Exception e) {
                    IStatus status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error updating launch properties file.", e);
                    Plugin.log(status);
                }
            }
        };
        ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener);

        // Register a listener for termination of the launched process
        Runnable onTerminate = new Runnable() {
            public void run() {
                ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceListener);
            }
        };
        DebugPlugin.getDefault().addDebugEventListener(new TerminationListener(launch, onTerminate));
    }

    @Override
    public String getMainTypeName(ILaunchConfiguration configuration) throws CoreException {
        assertBndLauncher();
        return bndLauncher.getMainTypeName();
    }

    private void assertBndLauncher() throws CoreException {
        if (bndLauncher == null)
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Bnd launcher was not initialised.", null));
    }

    @Override
    public String getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
        assertBndLauncher();

        StringBuilder builder = new StringBuilder();

        Collection<String> args = bndLauncher.getArguments();
        for (Iterator<String> iter = args.iterator(); iter.hasNext();) {
            builder.append(iter.next());
            if (iter.hasNext()) builder.append(" ");
        }

        return builder.toString();
    }

    @Override
    public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
        assertBndLauncher();
        Collection<String> paths = bndLauncher.getClasspath();
        return paths.toArray(new String[paths.size()]);
    }

    @Override
    public String getVMArguments(ILaunchConfiguration configuration) throws CoreException {
        assertBndLauncher();

        StringBuilder builder = new StringBuilder();
        Collection<String> runVM = bndLauncher.getRunVM();
        for (Iterator<String> iter = runVM.iterator(); iter.hasNext();) {
            builder.append(iter.next());
            if (iter.hasNext()) builder.append(" ");
        }
        String args = builder.toString();

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

    @Override
    public File verifyWorkingDirectory(ILaunchConfiguration configuration) throws CoreException {
        try {
            Project project = getBndProject(configuration);
            return (project != null) ? project.getBase() : null;
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error getting working directory for Bnd project.", e));
        }
    }

    protected Project getBndProject(ILaunchConfiguration configuration) throws CoreException {
        Project result;

        String target = configuration.getAttribute(LaunchConstants.ATTR_LAUNCH_TARGET, (String) null);
        if(target == null) {
            // For compatibility with launches created in previous versions
            target = getJavaProjectName(configuration);
        }
        if(target == null) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Bnd launch target was not specified", null));
        }

        IResource targetResource = ResourcesPlugin.getWorkspace().getRoot().findMember(target);
        if(targetResource == null)
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Bnd launch target \"{0}\" does not exist.", target), null));

        IProject project = targetResource.getProject();
        File projectDir = project.getLocation().toFile();
        if(targetResource.getType() == IResource.FILE) {
            if(!targetResource.getName().endsWith(LaunchConstants.EXT_BNDRUN))
                throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Bnd launch target file \"{0}\" is not a .bndrun file.", target), null));

            // Get the synthetic "run" project (based on a .bndrun file)
            File runFile = targetResource.getLocation().toFile();
            try {
                result = new Project(Central.getWorkspace(), projectDir, runFile);
            } catch (Exception e) {
                throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Failed to create synthetic project for run file {0} in project {1}.", targetResource.getProjectRelativePath().toString(), project.getName()), e));
            }
        } else if(targetResource.getType() == IResource.PROJECT) {
            // Use the main project (i.e. bnd.bnd)
            if(!project.hasNature(BndProjectNature.NATURE_ID))
                throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("The configured run project \"{0}\"is not a Bnd project.", project.getName()), null));
            try {
                result = Workspace.getProject(projectDir);
            } catch (Exception e) {
                throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Failed to retrieve Bnd project model for project \"{0}\".", project.getName()), null));
            }
        } else {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("The specified launch target \"{0}\" is not recognised as a Bnd project or .bndrun file.", targetResource.getFullPath().toString()), null));
        }

        return result;
    }

}