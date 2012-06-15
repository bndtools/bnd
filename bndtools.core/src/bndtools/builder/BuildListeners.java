package bndtools.builder;

import java.util.ArrayList;
import java.util.List;

import org.bndtools.build.api.BuildListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

import bndtools.Plugin;
import bndtools.api.ILogger;

public class BuildListeners {

    private final List<BuildListener> listeners;
    private final ServiceTracker listenerTracker;

    public BuildListeners(ILogger logger) {
        IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(Plugin.PLUGIN_ID, "buildListeners");
        listeners = new ArrayList<BuildListener>(elements.length);

        for (IConfigurationElement elem : elements) {
            try {
                BuildListener listener = (BuildListener) elem.createExecutableExtension("class");
                listeners.add(listener);
            } catch (Exception e) {
                logger.logError("Unable to instantiate build listener: " + elem.getAttribute("name"), e);
            }
        }

        BundleContext context = FrameworkUtil.getBundle(BuildListeners.class).getBundleContext();

        listenerTracker = new ServiceTracker(context, BuildListener.class.getName(), null);
        listenerTracker.open();
    }

    public void fireBuildStarting(IProject project) {
        for (BuildListener listener : listeners) {
            listener.buildStarting(project);
        }

        Object[] services = listenerTracker.getServices();
        for (Object service : services) {
            if (service != null) {
                BuildListener listener = (BuildListener) service;
                listener.buildStarting(project);
            }
        }
    }

    public void fireBuiltBundles(IProject project, IPath[] paths) {
        for (BuildListener listener : listeners) {
            listener.builtBundles(project, paths);
        }
        Object[] services = listenerTracker.getServices();
        for (Object service : services) {
            if (service != null) {
                BuildListener listener = (BuildListener) service;
                listener.builtBundles(project, paths);
            }
        }
    }

    /**
     * Call this to make sure that any references to the listeners are no longer held.
     */
    public void release() {
        listeners.clear();
        listenerTracker.close();
    }

}
