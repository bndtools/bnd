package aQute.bnd.service.diff;

import aQute.lib.osgi.*;

/**
 * Compare two Jars and report the differences.
 */
public interface DiffPlugin {
	interface Info {
		boolean isProvider(String fqn);
	}
	Diff diff( Analyzer newer, Analyzer older, Info ... info) throws Exception;
	Diff diff( Jar newer, Jar older, Info ...infos) throws Exception;
}
