package org.bndtools.versioncontrol.ignores.manager.api;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bndtools.api.NamedPlugin;

import aQute.bnd.annotation.ProviderType;

/**
 * <p>
 * The interface of the version control ignores manager.
 * </p>
 * <p>
 * Its purpose is to allow clients to add version control ignores to
 * directories.
 * </p>
 */
@ProviderType
public interface VersionControlIgnoresManager {
	/**
	 * Sanitise a Git ignore glob so that it is a properly formatted ignore glob
	 * in Git format.
	 * 
	 * @param rooted
	 *            true when the ignore glob must be rooted in the directory to
	 *            which the ignore glob is applied
	 * @param ignoreGlob
	 *            The ignore glob in Git format
	 * @param directory
	 *            true when the ignore glob denotes a directory
	 * @return The sanitised ignore glob in Git format
	 */
	String sanitiseGitIgnoreGlob(boolean rooted, String ignoreGlob, boolean directory);

	/**
	 * @return an unmodifiable collection containing the information for all
	 *         plugins
	 */
	Collection<NamedPlugin> getAllPluginsInformation();

	/**
	 * <p>
	 * Apply ignore globs to a directory.
	 * </p>
	 * <p>
	 * This is a convenience method for {@link #addIgnores(Set, File, List)}:
	 * </p>
	 * 
	 * @param plugins
	 *            the set of plugins (their names) to involve in applying ignore
	 *            globs. Usually only the plugins that are enabled through the
	 *            preferences should be involved: it is strongly advised to get
	 *            these through the BndPreferences (the burden is on the caller
	 *            of this method).
	 * @param dstDir
	 *            the directory which to apply the ignore globs to
	 * @param ignores
	 *            A comma-separated list of ignore globs to apply to the
	 *            directory. The ignore globs must in Git ignore format. Can be
	 *            null, which is the same as "" which will create an empty
	 *            ignore file when needed.
	 */
	void addIgnores(Set<String> plugins, File dstDir, String ignores);

	/**
	 * Apply ignore globs to a directory.
	 *
	 * @param plugins
	 *            the set of plugins (their names) to involve in applying ignore
	 *            globs. Usually only the plugins that are enabled through the
	 *            preferences should be involved: it is strongly advised to get
	 *            these through the BndPreferences (the burden is on the caller
	 *            of this method).
	 * @param dstDir
	 *            the directory which to apply the ignore globs to
	 * @param ignores
	 *            A list of ignore globs to apply to the directory. The ignore
	 *            globs must in Git ignore format and it's strongly advised to
	 *            call {@link #sanitiseGitIgnoreGlob(boolean, String, boolean)}
	 *            for each ignore glob. Can be null, which is the same as an
	 *            empty list which will create an empty ignore file when needed.
	 */
	void addIgnores(Set<String> plugins, File dstDir, List<String> ignoredEntries);

	/**
	 * <p>
	 * Create the (default) ignores for a project.
	 * </p>
	 * <p>
	 * It will setup:
	 * <ul>
	 * <li>empty ignores for each empty source directory of the project when the
	 * version control system of a plugin can't store empty directories</li>
	 * <li>ignores for each output directory belonging to a source directory of
	 * the project</li>
	 * </p>
	 * 
	 * @param plugins
	 *            the set of plugins (their names) to involve in applying ignore
	 *            globs. Usually only the plugins that are enabled through the
	 *            preferences should be involved: it is strongly advised to get
	 *            these through the BndPreferences (the burden is on the caller
	 *            of this method).
	 * @param projectDir
	 *            the project directory. For Eclipse projects usually obtained
	 *            by invoking IProject.getLocation().toFile()
	 * @param sourceOutputLocations
	 *            a map of source folders against their output folders, all
	 *            relative to the project directory
	 * @param targetDir
	 *            the target (usually 'generated' for bnd projects) directory,
	 *            relative to the project directory
	 */
	void createProjectIgnores(Set<String> plugins, File projectDir, Map<String, String> sourceOutputLocations, String targetDir);

	/**
	 * Determine which of the plugins can apply ignore globs for the version
	 * control system that is managing the project.
	 * 
	 * @param repositoryProviderId
	 *            the id of the repository provider that is managing the project
	 * @return a set of plugins that can apply ignore globs for the version
	 *         control system that is managing the project. null when
	 *         repositoryProviderId is null or empty or when there are no such
	 *         plugins.
	 */
	public Set<String> getPluginsForProjectRepositoryProviderId(String repositoryProviderId);
}
