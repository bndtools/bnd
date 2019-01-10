package org.bndtools.headless.build.manager;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.NamedPlugin;
import org.bndtools.headless.build.manager.api.HeadlessBuildManager;
import org.bndtools.headless.build.manager.api.HeadlessBuildPlugin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component
public class HeadlessBuildManagerImpl implements HeadlessBuildManager {
    private final ILogger logger = Logger.getLogger(this.getClass());

    private final Map<String, HeadlessBuildPlugin> plugins = new TreeMap<String, HeadlessBuildPlugin>();
    private final Map<String, NamedPlugin> pluginsInformation = new TreeMap<String, NamedPlugin>();

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
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

        String name = plugin.getInformation()
            .getName();
        synchronized (plugins) {
            pluginsInformation.remove(name);
            plugins.remove(name);
        }
    }

    /*
     * HeadlessBuildManager
     */

    @Override
    public Collection<NamedPlugin> getAllPluginsInformation() {
        synchronized (plugins) {
            return Collections.unmodifiableCollection(pluginsInformation.values());
        }
    }

    @Override
    @Deprecated
    public void setup(Set<String> plugins, boolean cnf, File projectDir, boolean add, Set<String> enabledIgnorePlugins) {
        setup(plugins, cnf, projectDir, add, enabledIgnorePlugins, new LinkedList<String>());
    }

    @Override
    public void setup(Set<String> plugins, boolean cnf, File projectDir, boolean add, Set<String> enabledIgnorePlugins, List<String> warnings) {
        if (plugins == null || plugins.isEmpty()) {
            return;
        }

        for (String pluginName : plugins) {
            HeadlessBuildPlugin plugin = null;
            synchronized (this.plugins) {
                plugin = this.plugins.get(pluginName);
            }
            if (plugin == null) {
                continue;
            }

            try {
                plugin.setup(cnf, projectDir, add, enabledIgnorePlugins, warnings);
            } catch (Throwable e) {
                logger.logError(String.format("Unable to %s headless build file(s) for the %sproject in %s", add ? "add" : "remove", cnf ? "cnf " : "", projectDir.getAbsolutePath()), e);
            }
        }
    }
}