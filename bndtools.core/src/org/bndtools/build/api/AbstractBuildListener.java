package org.bndtools.build.api;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

/**
 * Provided as a convenience for implementations to extend, where they do not
 * wish to provide implementations for all methods in {@link BuildListener}.
 * 
 * @author Neil Bartlett <njbartlett@gmail.com>
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

    /**
     * Default implementation does nothing.
     * 
     * @see BuildListener#builtBundles(IProject, IPath[])
     */
    public void builtBundles(IProject project, IPath[] paths) {
    }

}
