package org.bndtools.build.api;

import org.eclipse.core.resources.IProject;

/**
 * Provided as a convenience for implementations to extend, where they do not
 * wish to provide implementations for all methods in {@link BuildListener}.
 *
 * @author Neil Bartlett
 *
 */
public class AbstractBuildListener implements BuildListener {

    /**
     * Default implementation does nothing.
     *
     * @see BuildListener#buildStarting(IProject)
     */
    public void buildStarting(IProject project) {
    }

}
