package aQute.bnd.service.diff;

import aQute.lib.osgi.*;

/**
 * Compare two Jars and report the differences.
 */
public interface DiffPlugin {
	Diff diff( Jar newer, Jar older) throws Exception;
}
