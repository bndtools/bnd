package aQute.bnd.service;

/**
 * Signals the end of the registry initialization
 */
public interface RegistryDonePlugin {
	/**
	 * Signals the end of the registry initialization
	 *
	 * @throws Exception
	 */
	void done() throws Exception;
}
