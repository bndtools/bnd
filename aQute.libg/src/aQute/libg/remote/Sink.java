package aQute.libg.remote;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A Sink maintains a different file system somewhere. This API synchronizes
 * known files between a Source and the Sink.
 */
public interface Sink {
	int version = 1;

	boolean sync(String areaId, Collection<Delta> deltas) throws Exception;

	/**
	 * Return the protocol version that must be used. The parameter passed
	 * specifies the highest supported by the caller.
	 */
	Welcome getWelcome(int highestAccepted);

	/**
	 * Return a list of areas
	 */
	Collection<? extends Area> getAreas() throws Exception;

	/**
	 * Get a specific area
	 */
	Area getArea(String areaId) throws Exception;

	/**
	 * Remove an area
	 */
	boolean removeArea(String areaId) throws Exception;

	/**
	 * Create a new area
	 *
	 * @param areaId area id, or null for a new area
	 */
	Area createArea(String areaId) throws Exception;

	/**
	 * Launch
	 */

	boolean launch(String areaId, Map<String, String> env, List<String> args) throws Exception;

	int exit(String area) throws Exception;

	/**
	 * View a file or dir
	 */

	byte[] view(String areaId, String path) throws Exception;

	void exit() throws Exception;

	void input(String area, String text) throws Exception;

	void cancel(String areaId) throws Exception;

	boolean clearCache();
}
