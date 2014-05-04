package bndtools;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.bndtools.api.HeadlessBuildPlugin;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.NamedPlugin;
import org.bndtools.api.VersionControlIgnoresManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import bndtools.preferences.BndPreferences;

public class HeadlessBuildPluginTracker extends ServiceTracker<HeadlessBuildPlugin,HeadlessBuildPlugin> {
    private final ILogger logger = Logger.getLogger(this.getClass());

    private VersionControlIgnoresManager versionControlIgnoresManager = null;
    private final Map<String,ServiceReference<HeadlessBuildPlugin>> plugins = new TreeMap<String,ServiceReference<HeadlessBuildPlugin>>();
    private final Map<String,NamedPlugin> pluginsInformation = new TreeMap<String,NamedPlugin>();

    public HeadlessBuildPluginTracker(BundleContext context, VersionControlIgnoresManager versionControlIgnoresManager) {
        super(context, HeadlessBuildPlugin.class, null);
        this.versionControlIgnoresManager = versionControlIgnoresManager;
    }

    /*
     * ServiceTracker
     */

    @Override
    public void close() {
        synchronized (plugins) {
            plugins.clear();
            super.close();
        }
    }

    @Override
    public HeadlessBuildPlugin addingService(ServiceReference<HeadlessBuildPlugin> reference) {
        HeadlessBuildPlugin plugin = super.addingService(reference);
        NamedPlugin pluginInformation = plugin.getInformation();
        String name = pluginInformation.getName();
        synchronized (plugins) {
            plugins.put(name, reference);
            pluginsInformation.put(name, pluginInformation);
        }
        return plugin;
    }

    @Override
    public void remove(ServiceReference<HeadlessBuildPlugin> reference) {
        HeadlessBuildPlugin plugin = getService(reference);
        String name = plugin.getInformation().getName();
        synchronized (plugins) {
            pluginsInformation.remove(name);
            plugins.remove(name);
        }
        super.remove(reference);
    }

    /*
     * Manager
     */

    /**
     * @return a collection containing the information for all plugins
     */
    public Collection<NamedPlugin> getAllPluginsInformation() {
        synchronized (plugins) {
            return Collections.unmodifiableCollection(pluginsInformation.values());
        }
    }

    /**
     * Setup/remove files enabling headless build of a project.
     * 
     * @param plugins
     *            the plugins to involve in adding/removing the headless build of a project. Usually only the plugins
     *            that are enabled through the preferences should be involved: it is strongly advised to get these
     *            through {@link BndPreferences#getHeadlessBuildPluginsEnabled(HeadlessBuildPluginTracker, Set)} (the
     *            burden is on the caller of this method to avoid class cycles).
     * @param cnf
     *            true when the project directory is that of the cnf project
     * @param projectDir
     *            the project directory
     * @param add
     *            true to add/create the files, false to remove them
     * @param enabledIgnorePlugins
     *            set with enabled version control ignore plugins
     */
    public void setup(Set<String> plugins, boolean cnf, File projectDir, boolean add, Set<String> enabledIgnorePlugins) {
        if (plugins == null || plugins.isEmpty()) {
            return;
        }

        for (String pluginName : plugins) {
            ServiceReference<HeadlessBuildPlugin> pluginReference = null;
            synchronized (this.plugins) {
                pluginReference = this.plugins.get(pluginName);
            }
            if (pluginReference == null) {
                continue;
            }
            HeadlessBuildPlugin plugin = getService(pluginReference);
            if (plugin == null) {
                continue;
            }

            try {
                plugin.setup(cnf, projectDir, add, versionControlIgnoresManager, enabledIgnorePlugins);
            } catch (Throwable e) {
                logger.logError(String.format("Unable to %s headless build file(s) for the %sproject in %s", add ? "add" : "remove", cnf ? "cnf " : "", projectDir.getAbsolutePath()), e);
            }
        }
    }
}