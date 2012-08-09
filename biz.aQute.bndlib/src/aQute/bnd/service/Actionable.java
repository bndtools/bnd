package aQute.bnd.service;

import java.util.*;

/**
 * An interface to allow bnd to provide commands on selected entries. Primary
 * use case is the repository that provides commands on its entries. The
 * interface also provides the possibility to get some details of an entry.
 */
public interface Actionable {
	/**
	 * Return a map with command names (potentially localized) and a Runnable.
	 * The caller can execute the caller at will.
	 * 
	 * @param target
	 *            the target object, null if commands for the encompassing
	 *            entity is sought (e.g. the repo itself).
	 * @return A Map with the actions or null if no actions are available.
	 * @throws Exception
	 */
	Map<String,Runnable> actions(Object ... target) throws Exception;

	/**
	 * Return a tooltip for the given target or the encompassing entity if null
	 * is passed.
	 * 
	 * @param target
	 *            the target, any number of parameters to identify
	 * @return the tooltip or null
	 * @throws Exception 
	 */
	String tooltip(Object ... target) throws Exception;
}
