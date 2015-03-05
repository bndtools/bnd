package aQute.remote.api;

import java.util.List;
import java.util.Map;

/**
 * The Envoy starts up after main and can create frameworks.
 *
 */
public interface Envoy {
	public static final int DEFAULT_PORT = 29997;

	Map<String, String> getSystemProperties() throws Exception;

	int createFramework(String name, List<String> runpath,
			Map<String, Object> properties) throws Exception;

}
