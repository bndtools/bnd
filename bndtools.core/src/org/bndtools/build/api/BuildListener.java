package org.bndtools.build.api;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A listener for phases in the Bndtools build lifecycle.
 *
 * @author Neil Bartlett <njbartlett@gmail.com>
 */
@ProviderType
public interface BuildListener {

	enum BuildState {
		starting,
		built,
		released;
	}

	/**
	 * Bndtools is starting to build the specified project. The corresponding
	 * bnd project model in the bnd workspace has yet been created, and may not
	 * exist.
	 *
	 * @param project
	 */
	void buildStarting(IProject project);

	/**
	 * Bndtools has built one or more bundles in the specified project.
	 *
	 * @param project The Eclipse project for which the built has been executed.
	 * @param paths An array of workspace-relative paths.
	 */
	void builtBundles(IProject project, IPath[] paths);

	/**
	 * Listeners are released
	 */

	void released(IProject project);
}
