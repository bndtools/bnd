package org.bndtools.headless.build.manager.api;

import java.io.File;
import java.util.Collection;
import java.util.Set;

import org.bndtools.api.NamedPlugin;

import aQute.bnd.annotation.ProviderType;

/**
 * <p>
 * The interface of a headless build manager.
 * </p>
 * <p>
 * Its purpose is to allow bndtools to setup headless build files for projects.
 * </p>
 */
@ProviderType
public interface HeadlessBuildManager {
    /**
     * @return an unmodifiable collection containing the information for all plugins
     */
    Collection<NamedPlugin> getAllPluginsInformation();

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
    public void setup(Set<String> plugins, boolean cnf, File projectDir, boolean add, Set<String> enabledIgnorePlugins);
}