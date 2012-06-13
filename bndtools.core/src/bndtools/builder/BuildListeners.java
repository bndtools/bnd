package bndtools.builder;

import java.util.ArrayList;
import java.util.List;

import org.bndtools.build.api.BuildListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;

import bndtools.Plugin;

public class BuildListeners {

    private final List<BuildListener> listeners;
    
    public BuildListeners() {
        IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(Plugin.PLUGIN_ID, "buildListeners");
        listeners = new ArrayList<BuildListener>(elements.length);

        for (IConfigurationElement elem : elements) {
            try {
                BuildListener listener = (BuildListener) elem.createExecutableExtension("class");
                listeners.add(listener);
            } catch (Exception e) {
                Plugin.logError("Unable to instantiate build listener: " + elem.getAttribute("name"), e);
            }
        }
    }

    public void fireBuildStarting(IProject project) {
        for (BuildListener listener : listeners) {
            listener.buildStarting(project);
        }
    }
    
    public void fireBuiltBundles(IProject project, IPath[] paths) {
        for (BuildListener listener : listeners) {
            listener.builtBundles(project, paths);
        }
    }

    /**
     * Call this to make sure that any references to the listeners are no longer held.
     */
    public void release() {
        listeners.clear();
    }

}
