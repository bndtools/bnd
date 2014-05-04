package org.bndtools.versioncontrol.ignores.manager.api;

import java.io.File;
import java.util.List;

import org.bndtools.api.NamedPlugin;

import aQute.bnd.annotation.ConsumerType;

/**
 * <p>
 * The interface of a version control ignores plugin.
 * </p>
 * <p>
 * A version control ignores plugin can apply ignore globs (in Git format) to a directory that is under version control
 * of a certain version control system.
 * </p>
 * <p>
 * Bndtools internally always works with ignore globs in Git format. It is the responsibility of the plugin to interpret
 * those globs and translate them into the appropriate format.
 * </p>
 */
@ConsumerType
public interface VersionControlIgnoresPlugin {
    /**
     * @return generic plugin information
     */
    NamedPlugin getInformation();

    /**
     * @return true when the version control system can store empty directories, false otherwise
     */
    boolean canStoreEmptyDirectories();

    /**
     * @param repositoryProviderId
     *            the Eclipse plugin id that provides version control for a project
     * @return true when this plugin provides can apply version control ignore globs for the specified
     *         repositoryProviderId
     */
    boolean matchesRepositoryProviderId(String repositoryProviderId);

    /**
     * Apply version control ignore globs to a directory.
     * 
     * @param dstDir
     *            the destination directory
     * @param ignores
     *            a list of ignore globs (in Git format). Can be empty, which specifically is the case when the
     *            destination directory contains no files and when {@link #canStoreEmptyDirectories()} returned false.
     * @throws Exception
     *             upon error(s)
     */
    void addIgnores(File dstDir, List<String> ignores) throws Exception;
}
