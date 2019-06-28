package aQute.remote.main;

import java.util.Collection;
import java.util.Map;

/**
 * An Envoy only implements the Agent API partially. These APIs are coupled
 * because the supervisor should not see how we switch from an Envoy to an
 * Agent.
 */
public interface Envoy {

	/**
	 * We return true as an Envoy
	 */
	boolean isEnvoy();

	/**
	 * Create a framework
	 *
	 * @param name the name of the framework
	 * @param runpath The SHAs for the -runpath
	 * @param properties the fw properties
	 * @return true if a new fw was created, false if an existing fw
	 */
	boolean createFramework(String name, Collection<String> runpath, Map<String, Object> properties) throws Exception;

	boolean ping();
}
