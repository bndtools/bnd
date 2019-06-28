package aQute.bnd.service.lifecycle;

import java.util.Map;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;

/**
 * The base class for a plugin that wants to intercept workspace life cycle
 * events.
 */
public abstract class LifeCyclePlugin {

	/**
	 * Called when the plugin is setup. This plugin will be added to the setup
	 * but the workspace is not yet refreshed.
	 *
	 * @throws Exception
	 */
	public void init(Workspace ws) throws Exception {}

	public void opened(Project project) throws Exception {}

	public void close(Project project) throws Exception {}

	public void created(Project project) throws Exception {}

	public void delete(Project project) throws Exception {}

	public void addedPlugin(Workspace workspace, String name, String alias, Map<String, String> parameters)
		throws Exception {

	}

	public void removedPlugin(Workspace workspace, String alias) throws Exception {

	}

	public String augmentSetup(String setup, String alias, Map<String, String> parameters) throws Exception {
		return setup;
	}
}
