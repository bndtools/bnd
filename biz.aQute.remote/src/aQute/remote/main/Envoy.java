package aQute.remote.main;

import java.util.*;

public interface Envoy {
	public static final int	DEFAULT_PORT	= 29998;

	boolean isEnvoy();

	boolean createFramework(String name, Collection<String> runpath, Map<String,Object> properties) throws Exception;

}
