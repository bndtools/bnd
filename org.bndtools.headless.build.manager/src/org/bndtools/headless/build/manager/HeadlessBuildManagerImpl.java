package org.bndtools.headless.build.manager;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import org.bndtools.api.HeadlessBuildManager;
import org.bndtools.api.HeadlessBuildPlugin;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.NamedPlugin;
import org.bndtools.api.VersionControlIgnoresManager;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

@Component
public class HeadlessBuildManagerImpl implements HeadlessBuildManager {
    private final ILogger logger = Logger.getLogger(this.getClass());

    private final AtomicReference<VersionControlIgnoresManager> versionControlIgnoresManager = new AtomicReference<VersionControlIgnoresManager>();

    @Reference(type = '?')
    public void setVersionControlIgnoresManager(VersionControlIgnoresManager versionControlIgnoresManager) {
        this.versionControlIgnoresManager.set(versionControlIgnoresManager);
    }

    public void unsetVersionControlIgnoresManager(VersionControlIgnoresManager versionControlIgnoresManager) {
        this.versionControlIgnoresManager.compareAndSet(versionControlIgnoresManager, null);
    }

    private final Map<String,HeadlessBuildPlugin> plugins = new TreeMap<String,HeadlessBuildPlugin>();
    private final Map<String,NamedPlugin> pluginsInformation = new TreeMap<String,NamedPlugin>();

    @Reference(type = '+')
    void addPlugin(HeadlessBuildPlugin plugin) {
        if (plugin == null) {
            return;
        }

        NamedPlugin pluginInformation = plugin.getInformation();
        String name = pluginInformation.getName();
        synchronized (plugins) {
            plugins.put(name, plugin);
            pluginsInformation.put(name, pluginInformation);
        }
    }

    void removePlugin(HeadlessBuildPlugin plugin) {
        if (plugin == null) {
            return;
        }

        String name = plugin.getInformation().getName();
        synchronized (plugins) {
            pluginsInformation.remove(name);
            plugins.remove(name);
        }
    }

    /*
     * HeadlessBuildManager
     */

    public Collection<NamedPlugin> getAllPluginsInformation() {
        synchronized (plugins) {
            return Collections.unmodifiableCollection(pluginsInformation.values());
        }
    }

    public void setup(Set<String> plugins, boolean cnf, File projectDir, boolean add, Set<String> enabledIgnorePlugins) {
        if (plugins == null || plugins.isEmpty()) {
            return;
        }

        VersionControlIgnoresManager versionControlIgnoresManager = this.versionControlIgnoresManager.get();
        for (String pluginName : plugins) {
            HeadlessBuildPlugin plugin = null;
            synchronized (this.plugins) {
                plugin = this.plugins.get(pluginName);
            }
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