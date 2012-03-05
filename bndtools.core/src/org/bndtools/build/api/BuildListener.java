package org.bndtools.build.api;

import org.eclipse.core.resources.IProject;

/**
 * A listener for phases in the Bndtools build lifecycle.
 *
 * @author Neil Bartlett
 *
 */
public interface BuildListener {

    /**
     * Bndtools is starting to build the specified IProject. The corresponding
     * bnd project model in the bnd workspace has yet been created, and may not
     * exist.
     *
     * @param project
     */
    void buildStarting(IProject project);

}
