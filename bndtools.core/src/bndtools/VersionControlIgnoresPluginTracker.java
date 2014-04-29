package bndtools;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.NamedPlugin;
import org.bndtools.api.ProjectPaths;
import org.bndtools.api.VersionControlIgnoresManager;
import org.bndtools.api.VersionControlIgnoresPlugin;
import org.bndtools.utils.javaproject.JavaProjectUtils;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.team.core.RepositoryProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import bndtools.preferences.BndPreferences;

public class VersionControlIgnoresPluginTracker extends ServiceTracker<VersionControlIgnoresPlugin,VersionControlIgnoresPlugin> implements VersionControlIgnoresManager {
    private final ILogger logger = Logger.getLogger(this.getClass());

    private final Map<String,ServiceReference<VersionControlIgnoresPlugin>> plugins = new TreeMap<String,ServiceReference<VersionControlIgnoresPlugin>>();
    private final Map<String,NamedPlugin> pluginsInformation = new TreeMap<String,NamedPlugin>();

    public VersionControlIgnoresPluginTracker(BundleContext context) {
        super(context, VersionControlIgnoresPlugin.class, null);
    }

    /*
     * ServiceTracker
     */

    @Override
    public void close() {
        synchronized (plugins) {
            plugins.clear();
        }
        super.close();
    }

    @Override
    public VersionControlIgnoresPlugin addingService(ServiceReference<VersionControlIgnoresPlugin> reference) {
        VersionControlIgnoresPlugin plugin = super.addingService(reference);
        NamedPlugin pluginInformation = plugin.getInformation();
        String name = pluginInformation.getName();
        synchronized (plugins) {
            plugins.put(name, reference);
            pluginsInformation.put(name, pluginInformation);
        }
        return plugin;
    }

    @Override
    public void remove(ServiceReference<VersionControlIgnoresPlugin> reference) {
        VersionControlIgnoresPlugin plugin = getService(reference);
        String name = plugin.getInformation().getName();
        synchronized (plugins) {
            pluginsInformation.remove(name);
            plugins.remove(name);
        }
        super.remove(reference);
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
            ServiceReference<VersionControlIgnoresPlugin> pluginReference = null;
            synchronized (this.plugins) {
                pluginReference = this.plugins.get(pluginName);
            }
            if (pluginReference == null) {
                continue;
            }
            VersionControlIgnoresPlugin plugin = getService(pluginReference);
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

    /*
     * Extra
     */

    /**
     * Determine which of the plugins can apply ignore globs for the version control system that is managing the
     * project.
     * 
     * @param project
     *            the project
     * @return a list of plugins that can apply ignore globs for the version control system that is managing the
     *         project. null when project is null, when no version control system is managing the project or when there
     *         are no such plugins.
     */
    public Set<String> getPluginsForProjectVersionControlSystem(IJavaProject project) {
        if (project == null) {
            return null;
        }

        RepositoryProvider repositoryProvider = RepositoryProvider.getProvider(project.getProject());
        if (repositoryProvider == null) {
            return null;
        }

        String repositoryProviderId = repositoryProvider.getID();

        Set<String> matches = new HashSet<String>();
        synchronized (plugins) {
            for (Map.Entry<String,ServiceReference<VersionControlIgnoresPlugin>> entry : plugins.entrySet()) {
                ServiceReference<VersionControlIgnoresPlugin> pluginReference = entry.getValue();
                if (pluginReference == null) {
                    continue;
                }
                VersionControlIgnoresPlugin plugin = getService(pluginReference);
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

    /**
     * @return a collection containing the information for all plugins
     */
    public Collection<NamedPlugin> getAllPluginsInformation() {
        synchronized (plugins) {
            return Collections.unmodifiableCollection(pluginsInformation.values());
        }
    }

    /**
     * <p>
     * Create the (default) ignores for a bnd project.
     * </p>
     * <p>
     * It will setup:
     * <ul>
     * <li>empty ignores for each empty source directory of the project when the version control system of a plugin
     * can't store empty directories</li>
     * <li>ignores for each output directory belonging to a source directory of the project</li>
     * </p>
     * 
     * @param plugins
     *            the set of plugins to involve in applying ignores. Usually only the plugins that are enabled through
     *            the preferences should be involved: it is strongly advised to get these through the
     *            {@link BndPreferences#getVersionControlIgnoresPluginsEnabled(VersionControlIgnoresPluginTracker, IJavaProject, Set)}
     *            method (the burden is on the caller of this method to avoid class cycles).
     * @param project
     *            the project
     * @param projectPaths
     *            the project paths. Used to retrieve the target directory from. Also used as fallback when the source
     *            directories could not be determined from the (Eclipse) classpath.
     */
    public void createProjectIgnores(Set<String> plugins, IJavaProject project, ProjectPaths projectPaths) {
        if (project == null || plugins == null || plugins.isEmpty()) {
            return;
        }

        for (String pluginName : plugins) {
            ServiceReference<VersionControlIgnoresPlugin> pluginReference = null;
            synchronized (this.plugins) {
                pluginReference = this.plugins.get(pluginName);
            }
            if (pluginReference == null) {
                continue;
            }
            VersionControlIgnoresPlugin plugin = getService(pluginReference);
            if (plugin == null) {
                continue;
            }

            Map<String,String> sourceOutputLocations = JavaProjectUtils.getSourceOutputLocations(project);
            if (sourceOutputLocations == null) {
                /* fallback to using defaults */
                sourceOutputLocations = new LinkedHashMap<String,String>();
                sourceOutputLocations.put(projectPaths.getSrc(), projectPaths.getBin());
                sourceOutputLocations.put(projectPaths.getTestSrc(), projectPaths.getTestBin());
            }

            List<String> emptyIgnores = new LinkedList<String>();
            List<String> projectIgnores = new LinkedList<String>();
            File projectDir = project.getProject().getLocation().toFile();

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

            /* add the target directory to the project ignores */
            projectIgnores.add(sanitiseGitIgnoreGlob(true, projectPaths.getTargetDir(), true));

            try {
                plugin.addIgnores(projectDir, projectIgnores);
            } catch (Throwable e) {
                logger.logError(String.format("Unable to add %s ignores %s to the project in %s", plugin.getInformation().getName(), projectIgnores, projectDir), e);
            }
        }
    }
}