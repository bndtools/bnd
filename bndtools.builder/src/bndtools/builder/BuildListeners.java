package bndtools.builder;

import java.util.ArrayList;
import java.util.List;

import org.bndtools.build.api.BuildListener;
import org.bndtools.utils.Function;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

import bndtools.Logger;
import bndtools.Plugin;
import bndtools.api.ILogger;

public class BuildListeners {
    private static final ILogger logger = Logger.getLogger();

    private final List<BuildListener> listeners;
    private final ServiceTracker listenerTracker;

    public BuildListeners() {
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

    public void fireBuildStarting(final IProject project) {
        forEachListener(new Function<BuildListener,Object>() {
            public Object run(BuildListener listener) {
                listener.buildStarting(project);
                return null;
            }
        });
    }

    public void fireBuiltBundles(final IProject project, final IPath[] paths) {
        forEachListener(new Function<BuildListener,Object>() {
            public Object run(BuildListener listener) {
                listener.builtBundles(project, paths);
                return null;
            }
        });
    }

    private void forEachListener(Function<BuildListener, ? extends Object> function) {
        for (BuildListener listener : listeners)
            function.run(listener);

        Object[] services = listenerTracker.getServices();
        if (services != null) {
            for (Object service : services) {
                if (service != null)
                    function.run((BuildListener) service);
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
