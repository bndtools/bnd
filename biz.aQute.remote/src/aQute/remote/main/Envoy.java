package aQute.remote.main;

import java.util.Collection;
import java.util.Map;

public interface Envoy {
	public static final int DEFAULT_PORT = 29998;
	boolean isEnvoy();

	int createFramework(String name, Collection<String> runpath,
			Map<String, Object> properties) throws Exception;

}
