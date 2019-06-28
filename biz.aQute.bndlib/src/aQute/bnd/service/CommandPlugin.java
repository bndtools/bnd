package aQute.bnd.service;

import aQute.bnd.build.Project;

/**
 * A plugin that makes it possible to
 *
 * @author aqute
 */
public interface CommandPlugin {
	/**
	 * Is run before a command is executed. These plugins are called in the
	 * order of declaration.
	 *
	 * @param project The project for which the command runs
	 * @param command the command name
	 */
	void before(Project project, String command);

	/**
	 * Is run after a command is executed. These plugins are called in the
	 * reverse order of declaration.
	 *
	 * @param project The project for which the command runs
	 * @param command the command name
	 */
	void after(Project project, String command, Throwable outcome);
}
