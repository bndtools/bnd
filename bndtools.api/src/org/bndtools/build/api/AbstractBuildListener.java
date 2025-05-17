package org.bndtools.build.api;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

/**
 * Provided as a convenience for implementations to extend, where they do not
 * wish to provide implementations for all methods in {@link BuildListener}.
 *
 * @author Neil Bartlett <njbartlett@gmail.com>
 */
public class AbstractBuildListener implements BuildListener {

	/**
	 * Default implementation does nothing.
	 *
	 * @see BuildListener#buildStarting(IProject)
	 */
	@Override
	public void buildStarting(IProject project) {}

	/**
	 * Default implementation does nothing.
	 *
	 * @see BuildListener#builtBundles(IProject, IPath[])
	 */
	@Override
	public void builtBundles(IProject project, IPath[] paths) {}

	/**
	 * Default implementation does nothing.
	 *
	 * @see BuildListener#released(IProject)
	 */
	@Override
	public void released(IProject project) {}
}
