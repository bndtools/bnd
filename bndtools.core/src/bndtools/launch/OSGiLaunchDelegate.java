package bndtools.launch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.JavaLaunchDelegate;

import aQute.bnd.build.Container;
import aQute.bnd.build.Container.TYPE;
import aQute.bnd.build.Project;
import aQute.bnd.plugin.Activator;
import aQute.bnd.plugin.Central;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Constants;
import aQute.lib.osgi.Processor;
import bndtools.Plugin;

public class OSGiLaunchDelegate extends JavaLaunchDelegate {

    private static final String LAUNCHER_BSN = "bndtools.launcher";
    private static final String LAUNCHER_MAIN_CLASS = LAUNCHER_BSN + ".Main";

    private File launchPropsFile;

    @Override
    public void launch(final ILaunchConfiguration configuration, String mode, final ILaunch launch, IProgressMonitor monitor) throws CoreException {
        final Project model = getBndProject(configuration);

        // Generate the initial launch properties file
        try {
            launchPropsFile = File.createTempFile("bndtools.launcher", ".properties");
            launchPropsFile.deleteOnExit();
            generateLaunchPropsFile(model, configuration);
        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error creating temporary launch properties file.", e));
        }

        // Add a resource listener to regenerate launch properties if the
        // project descriptor changes
        final IPath propsPath = Central.toPath(model, model.getPropertiesFile());
        final IResourceChangeListener resourceListener = new IResourceChangeListener() {
            public void resourceChanged(IResourceChangeEvent event) {
                try {
                    IResourceDelta delta = event.getDelta();
                    delta = delta.findMember(propsPath);
                    if (delta != null) {
                        if (delta.getKind() == IResourceDelta.CHANGED) {
                            generateLaunchPropsFile(model, configuration);
                        } else if (delta.getKind() == IResourceDelta.REMOVED) {
                            launchPropsFile.delete();
                        }
                    }
                } catch (Exception e) {
                    IStatus status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error updating launch properties file.", e);
                    Plugin.log(status);
                }
            }
        };
        ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener);

        Runnable onTerminate = new Runnable() {
            public void run() {
                System.out.println("Processes terminated.");
                ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceListener);
            }
        };
        DebugPlugin.getDefault().addDebugEventListener(new TerminationListener(launch, onTerminate));
        super.launch(configuration, mode, launch, monitor);
    }

    void generateLaunchPropsFile(Project model, ILaunchConfiguration configuration) throws IOException, CoreException {
        Properties outputProps = new Properties();

        // Expand -runbundles
        Collection<String> runBundlePaths;
        synchronized (model) {
            try {
                // Calculate physical paths for -runbundles from bnd.bnd
                Collection<Container> runbundles = model.getRunbundles();
                runBundlePaths = new ArrayList<String>(runbundles.size());
                MultiStatus resolveErrors = new MultiStatus(Plugin.PLUGIN_ID, 0, "One or more run bundles could not be resolved.", null);
                for (Container container : runbundles) {
                    if (container.getType() == TYPE.ERROR) {
                        resolveErrors.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Could not resolve run bundle {0}, version {1}.",
                                container.getBundleSymbolicName(), container.getVersion()), null));
                    } else {
                        runBundlePaths.add(container.getFile().getAbsolutePath());
                    }
                }
                if (!resolveErrors.isOK()) {
                    throw new CoreException(resolveErrors);
                }

                // Add the project's own output bundles
                Collection<? extends Builder> builders = model.getSubBuilders();
                for (Builder builder : builders) {
                    File bundlefile = new File(model.getTarget(), builder.getBsn() + ".jar");
                    runBundlePaths.add(bundlefile.getAbsolutePath());
                }
            } catch (Exception e) {
                throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error finding run bundles.", e));
            }
        }
        outputProps.put(Constants.RUNBUNDLES, Processor.join(runBundlePaths));

        // Copy misc properties
        outputProps.setProperty(Constants.RUNPROPERTIES, model.getProperties().getProperty(Constants.RUNPROPERTIES, ""));
        outputProps.setProperty(Constants.RUNSYSTEMPACKAGES, model.getProperties().getProperty(Constants.RUNSYSTEMPACKAGES, ""));
        outputProps.setProperty(Constants.RUNVM, model.getProperties().getProperty(Constants.RUNVM, ""));
        outputProps.setProperty(LaunchConstants.ATTR_DYNAMIC_BUNDLES, Boolean.toString(configuration.getAttribute(LaunchConstants.ATTR_DYNAMIC_BUNDLES,
                LaunchConstants.DEFAULT_DYNAMIC_BUNDLES)));
        outputProps.setProperty(LaunchConstants.ATTR_CLEAN, Boolean.toString(configuration.getAttribute(LaunchConstants.ATTR_CLEAN,
                LaunchConstants.DEFAULT_CLEAN)));

        // Write properties
        outputProps.store(new FileOutputStream(launchPropsFile), Processor.join(runBundlePaths));
    }

    @Override
    public String getMainTypeName(ILaunchConfiguration configuration) throws CoreException {
        return LAUNCHER_MAIN_CLASS;
    }

    @Override
    public String getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
        return launchPropsFile.getAbsolutePath();
    }

    protected Project getBndProject(ILaunchConfiguration configuration) throws CoreException {
        IJavaProject javaProject = getJavaProject(configuration);
        Project model = Activator.getDefault().getCentral().getModel(javaProject);
        return model;
    }

    @Override
    public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
        Project model = getBndProject(configuration);

        // Get the framework bundle
        String fwkBSN = configuration.getAttribute(LaunchConstants.ATTR_FRAMEWORK_BSN, LaunchConstants.DEFAULT_FRAMEWORK_BSN);
        File fwkBundle = findBundle(model, fwkBSN);
        if (fwkBundle == null) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Could not find framework bundle {0}.", fwkBSN), null));
        }

        // Get the launcher bundle
        File launcherBundle = findBundle(model, LAUNCHER_BSN);
        if (launcherBundle == null) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Could not find launcher bundle {0}.", LAUNCHER_BSN),
                    null));
        }

        // Add to the classpath
        String[] classpath = new String[2];
        // String[] newClasspath = new String[classpath.length + 2];
        // System.arraycopy(classpath, 0, newClasspath, 0, classpath.length);
        classpath[0] = launcherBundle.getAbsolutePath();
        classpath[1] = fwkBundle.getAbsolutePath();

        return classpath;
    }

    protected File findBundle(Project project, String bsn) throws CoreException {
        try {
            Container snapshotContainer = project.getBundle(bsn, "snapshot", Constants.STRATEGY_HIGHEST, null);
            if (snapshotContainer != null && snapshotContainer.getType() != TYPE.ERROR) {
                return snapshotContainer.getFile();
            }

            Container repoContainer = project.getBundle(bsn, "0", Constants.STRATEGY_HIGHEST, null);
            if (repoContainer != null && repoContainer.getType() != TYPE.ERROR) {
                return repoContainer.getFile();
            }
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format(
                    "An error occurred while searching the workspace or repositories for bundle {0}.", bsn), e));
        }
        return null;
    }
}