package aQute.bnd.service;

import java.io.File;

public interface Refreshable {
	/**
	 * Instructs a Refreshable to refresh itself
	 *
	 * @return true if refreshed, false if not refreshed possibly due to error.
	 * @throws Exception
	 */
	boolean refresh() throws Exception;

	File getRoot() throws Exception;
}
