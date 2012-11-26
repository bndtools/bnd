package bndtools.launch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.osgi.framework.launch.FrameworkFactory;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.osgi.Jar;
import aQute.lib.io.IO;
import bndtools.Central;
import bndtools.Logger;
import bndtools.Plugin;
import bndtools.api.ILogger;
import bndtools.launch.util.LaunchUtils;

public class OSGiRunLaunchDelegate extends AbstractOSGiLaunchDelegate {
    private static final ILogger logger = Logger.getLogger();

    private ProjectLauncher bndLauncher = null;

    @Override
    protected void initialiseBndLauncher(ILaunchConfiguration configuration, Project model) throws Exception {
        synchronized (model) {
            bndLauncher = model.getProjectLauncher();
        }
        configureLauncher(configuration);
        bndLauncher.prepare();
    }

    @Override
    protected IStatus getLauncherStatus() {
        List<String> launcherErrors = bndLauncher.getErrors();
        List<String> projectErrors = bndLauncher.getProject().getErrors();
        List<String> errors = new ArrayList<String>(projectErrors.size() + launcherErrors.size());
        errors.addAll(launcherErrors);
        errors.addAll(projectErrors);

        List<String> launcherWarnings = bndLauncher.getWarnings();
        List<String> projectWarnings = bndLauncher.getProject().getWarnings();
        List<String> warnings = new ArrayList<String>(launcherWarnings.size() + projectWarnings.size());
        warnings.addAll(launcherWarnings);
        warnings.addAll(projectWarnings);

        String frameworkPath = validateClasspath(bndLauncher.getClasspath());
        if (frameworkPath == null)
            errors.add("No OSGi framework has been added to the run path.");

        return createStatus("Problem(s) preparing the runtime environment.", errors, warnings);
    }

    private static String validateClasspath(Collection<String> classpath) {
        for (String fileName : classpath) {
            Jar jar = null;
            try {
                jar = new Jar(new File(fileName));
                boolean frameworkExists = jar.exists("META-INF/services/" + FrameworkFactory.class.getName());
                if (frameworkExists)
                    return fileName;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (jar != null) {
                    jar.close();
                }
            }
        }
        return null;
    }

    @Override
    public void launch(final ILaunchConfiguration configuration, String mode, final ILaunch launch, IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor, 2);

        try {
            boolean dynamic = configuration.getAttribute(LaunchConstants.ATTR_DYNAMIC_BUNDLES, LaunchConstants.DEFAULT_DYNAMIC_BUNDLES);
            if (dynamic)
                registerLaunchPropertiesRegenerator(model, launch);
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error obtaining OSGi project launcher.", e));
        }

        super.launch(configuration, mode, launch, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
    }

    private void configureLauncher(ILaunchConfiguration configuration) throws CoreException {
        boolean clean = configuration.getAttribute(LaunchConstants.ATTR_CLEAN, LaunchConstants.DEFAULT_CLEAN);

        if (clean) {
            File storage = bndLauncher.getStorageDir();
            if (storage.exists()) {
                IO.delete(storage);
            }
        }

        bndLauncher.setKeep(!clean);

        bndLauncher.setTrace(enableTraceOption(configuration));
    }

    /**
     * Registers a resource listener with the project model file to update the launcher when the model or any of the
     * run-bundles changes. The resource listener is automatically unregistered when the launched process terminates.
     * 
     * @param project
     * @param launch
     * @throws CoreException
     */
    private void registerLaunchPropertiesRegenerator(final Project project, final ILaunch launch) throws CoreException {
        final IResource targetResource = LaunchUtils.getTargetResource(launch.getLaunchConfiguration());
        if (targetResource == null)
            return;

        final IPath bndbndPath;
        try {
            bndbndPath = Central.toPath(project.getPropertiesFile());
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error querying bnd.bnd file location", e));
        }

        final IPath targetPath;
        try {
            targetPath = Central.toPath(project.getTarget());
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error querying project output folder", e));
        }
        final IResourceChangeListener resourceListener = new IResourceChangeListener() {
            public void resourceChanged(IResourceChangeEvent event) {
                try {
                    final AtomicBoolean update = new AtomicBoolean(false);

                    // Was the properties file (bnd.bnd or *.bndrun) included in
                    // the delta?
                    IResourceDelta propsDelta = event.getDelta().findMember(bndbndPath);
                    if (propsDelta == null && targetResource.getType() == IResource.FILE)
                        propsDelta = event.getDelta().findMember(targetResource.getFullPath());
                    if (propsDelta != null) {
                        if (propsDelta.getKind() == IResourceDelta.CHANGED) {
                            update.set(true);
                        }
                    }

                    // Check for bundles included in the launcher's runbundles
                    // list
                    if (!update.get()) {
                        final Set<String> runBundleSet = new HashSet<String>(bndLauncher.getRunBundles());
                        event.getDelta().accept(new IResourceDeltaVisitor() {
                            public boolean visit(IResourceDelta delta) throws CoreException {
                                // Short circuit if we have already found a
                                // match
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

                    // Was the target path included in the delta? This might
                    // mean that sub-bundles have changed
                    boolean targetPathChanged = event.getDelta().findMember(targetPath) != null;
                    update.compareAndSet(false, targetPathChanged);

                    if (update.get()) {
                        project.forceRefresh();
                        project.setChanged();
                        bndLauncher.update();
                    }
                } catch (Exception e) {
                    logger.logError("Error updating launch properties file.", e);
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
    protected ProjectLauncher getProjectLauncher() throws CoreException {
        if (bndLauncher == null)
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Bnd launcher was not initialised.", null));
        return bndLauncher;
    }

}