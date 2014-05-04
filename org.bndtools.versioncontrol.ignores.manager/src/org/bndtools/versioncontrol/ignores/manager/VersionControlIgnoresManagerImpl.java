package org.bndtools.versioncontrol.ignores.manager;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.NamedPlugin;
import org.bndtools.versioncontrol.ignores.manager.api.VersionControlIgnoresManager;
import org.bndtools.versioncontrol.ignores.manager.api.VersionControlIgnoresPlugin;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

@Component
public class VersionControlIgnoresManagerImpl implements VersionControlIgnoresManager {
    private final ILogger logger = Logger.getLogger(this.getClass());

    private final Map<String,VersionControlIgnoresPlugin> plugins = new TreeMap<String,VersionControlIgnoresPlugin>();
    private final Map<String,NamedPlugin> pluginsInformation = new TreeMap<String,NamedPlugin>();

    @Reference(type = '+')
    void addPlugin(VersionControlIgnoresPlugin plugin) {
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

    void removePlugin(VersionControlIgnoresPlugin plugin) {
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
     * VersionControlIgnoresManager
     */

    public String sanitiseGitIgnoreGlob(boolean rooted, String ignoreGlob, boolean directory) {
        /* trim */
        String newPath = ignoreGlob.trim();

        /* replace all consecutive slashes with a single slash */
        newPath = newPath.replaceAll("/+", "/");

        /* remove all leading slashes */
        newPath = newPath.replaceAll("^/+", "");

        /* remove all trailing slashes */
        newPath = newPath.replaceAll("/+$", "");

        return String.format("%s%s%s", rooted ? "/" : "", newPath, directory ? "/" : "");
    }

    public void addIgnores(Set<String> plugins, File dstDir, String ignores) {
        List<String> ignoredEntries = null;
        if (ignores != null && ignores.trim() != null) {
            String[] entries = ignores.trim().split("\\s*,\\s*");
            ignoredEntries = new LinkedList<String>();
            for (String entry : entries) {
                ignoredEntries.add(entry);
            }
            if (ignoredEntries.isEmpty()) {
                ignoredEntries = null;
            }
        }

        addIgnores(plugins, dstDir, ignoredEntries);
    }

    public void addIgnores(Set<String> plugins, File dstDir, List<String> ignores) {
        if (plugins == null || plugins.isEmpty()) {
            return;
        }

        for (String pluginName : plugins) {
            VersionControlIgnoresPlugin plugin;
            synchronized (this.plugins) {
                plugin = this.plugins.get(pluginName);
            }
            if (plugin == null) {
                continue;
            }

            try {
                plugin.addIgnores(dstDir, ignores);
            } catch (Throwable e) {
                logger.logError(String.format("Unable to add %s ignores %s to directory %s", plugin.getInformation().getName(), ignores, dstDir), e);
            }
        }
    }

    public Set<String> getPluginsForProjectRepositoryProviderId(String repositoryProviderId) {
        if (repositoryProviderId == null || repositoryProviderId.length() == 0) {
            return null;
        }

        Set<String> matches = new HashSet<String>();
        synchronized (plugins) {
            for (Map.Entry<String,VersionControlIgnoresPlugin> entry : plugins.entrySet()) {
                VersionControlIgnoresPlugin plugin = this.plugins.get(entry.getKey());
                if (plugin == null) {
                    continue;
                }

                if (plugin.matchesRepositoryProviderId(repositoryProviderId)) {
                    matches.add(entry.getKey());
                }
            }
        }

        if (!matches.isEmpty()) {
            return matches;
        }

        return null;
    }

    public Collection<NamedPlugin> getAllPluginsInformation() {
        synchronized (plugins) {
            return Collections.unmodifiableCollection(pluginsInformation.values());
        }
    }

    public void createProjectIgnores(Set<String> plugins, File projectDir, Map<String,String> sourceOutputLocations, String targetDir) {
        if (projectDir == null || plugins == null || plugins.isEmpty()) {
            return;
        }

        for (String pluginName : plugins) {
            VersionControlIgnoresPlugin plugin;
            synchronized (this.plugins) {
                plugin = this.plugins.get(pluginName);
            }
            if (plugin == null) {
                continue;
            }

            List<String> projectIgnores = new LinkedList<String>();

            if (sourceOutputLocations != null) {
                List<String> emptyIgnores = new LinkedList<String>();
                for (Map.Entry<String,String> sourceOutputLocation : sourceOutputLocations.entrySet()) {
                    String srcDir = sourceOutputLocation.getKey();
                    String binDir = sourceOutputLocation.getValue();
                    assert (srcDir != null);
                    assert (binDir != null);

                    File srcDirFile = new File(projectDir, srcDir);

                    /*
                     * when the version control system can't store empty directories and
                     * the source directory doesn't exist or is empty, then add empty ignores
                     */
                    if (!plugin.canStoreEmptyDirectories() && (!srcDirFile.exists() || (srcDirFile.list().length == 0))) {
                        try {
                            plugin.addIgnores(srcDirFile, emptyIgnores);
                        } catch (Throwable e) {
                            logger.logError(String.format("Unable to add empty %s ignores to the project in %s", plugin.getInformation().getName(), projectDir), e);
                        }
                    }

                    /* add the corresponding output location to the project ignores */
                    projectIgnores.add(sanitiseGitIgnoreGlob(true, binDir, true));
                }
            }

            if (targetDir != null && !targetDir.isEmpty()) {
                /* add the target directory to the project ignores */
                projectIgnores.add(sanitiseGitIgnoreGlob(true, targetDir, true));
            }

            if (!projectIgnores.isEmpty()) {
                try {
                    plugin.addIgnores(projectDir, projectIgnores);
                } catch (Throwable e) {
                    logger.logError(String.format("Unable to add %s ignores %s to the project in %s", plugin.getInformation().getName(), projectIgnores, projectDir), e);
                }
            }
        }
    }
}