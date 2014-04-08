package org.bndtools.api;

import java.io.File;
import java.util.List;
import java.util.Set;

import aQute.bnd.annotation.ProviderType;

/**
 * <p>
 * The (non-Eclipse API) interface of the version control ignores manager.
 * </p>
 * <p>
 * Its purpose is to allow plugins to add ignores.
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
	 * <p>
	 * Apply ignore globs to a directory.
	 * </p>
	 * <p>
	 * This is a convenience method for {@link #addIgnores(Set, File, List)}:
	 * </p>
	 * 
	 * @param plugins
	 *            the set of plugins to involve in applying ignore globs.
	 *            Usually only the plugins that are enabled through the
	 *            preferences should be involved: it is strongly advised to get
	 *            these through the BndPreferences (the burden is on the caller
	 *            of this method to avoid class cycles).
	 * @param dstDir
	 *            the directory which to apply the ignore globs to
	 * @param ignores
	 *            A comma-separated list of ignore globs to apply to the
	 *            directory. The ignore globs must in Git ignore format and it's
	 *            strongly advised to call
	 *            {@link #sanitiseGitIgnoreGlob(boolean, String, boolean)} for
	 *            each ignore glob. Can be null, which is the same as ""
	 */
	void addIgnores(Set<String> plugins, File dstDir, String ignores);

	/**
	 * Apply ignore globs to a directory.
	 *
	 * @param plugins
	 *            the set of plugins to involve in applying ignore globs.
	 *            Usually only the plugins that are enabled through the
	 *            preferences should be involved: it is strongly advised to get
	 *            these through the BndPreferences (the burden is on the caller
	 *            of this method to avoid class cycles).
	 * @param dstDir
	 *            the directory which to apply the ignore globs to
	 * @param ignores
	 *            A list of ignore globs to apply to the directory. The ignore
	 *            globs must in Git ignore format and it's strongly advised to
	 *            call {@link #sanitiseGitIgnoreGlob(boolean, String, boolean)}
	 *            for each ignore glob. Can be null, which is the same as an
	 *            empty list.
	 */
	void addIgnores(Set<String> plugins, File dstDir, List<String> ignoredEntries);
}
