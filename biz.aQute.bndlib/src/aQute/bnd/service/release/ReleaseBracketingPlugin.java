package aQute.bnd.service.release;

import aQute.bnd.build.Project;

/**
 * This plugin brackets a workspace release so that plugins know when a
 * workspace release starts and ends. This plugin provides the bracketing of a
 * workspace release. Before a release starts the {@link #begin(Project)} method
 * is called, at the end the {@link #end(Project)} method.
 * <p>
 * Only one release cycle
 * <p>
 * The plugin was made for the MavenBndRepository that will create a an
 * classifier artifact for an OSGi index. However, these are details of the
 * repository plugin and maybe project settings. The sole purpose of this plugin
 * is to provide bracketing of a release cycle.
 */
public interface ReleaseBracketingPlugin {

	/**
	 * Indicate that a release cycle is about to start.
	 *
	 * @param project The project that should be associated with product created
	 *            at the end of the release cycle.
	 */
	void begin(Project project);

	/**
	 * Indicate that a release cycle has ended
	 *
	 * @param project The project that should be associated with product created
	 *            at the end of the release cycle.
	 */
	void end(Project p);
}
