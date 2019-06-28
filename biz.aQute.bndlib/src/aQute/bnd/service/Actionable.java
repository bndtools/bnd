package aQute.bnd.service;

import java.util.Map;

/**
 * An interface to allow bnd to provide commands on elements. This interface can
 * provide information about the implementer but it can also provide information
 * about its elements. These elements are identified by a <i>target</i>. A
 * target is one or more objects that uniquely identify a child in the
 * container. The exact protocol for the target is left to the implementers,
 * this interface is just a conduit between the bnd world (no Eclipse etc) and
 * the GUI world, using only bnd and java interfaces.
 */
public interface Actionable {
	/**
	 * Return a map with command names (potentially localized) and a Runnable.
	 * The caller can execute the caller at will.
	 *
	 * @param target the target object, null if commands for the encompassing
	 *            entity is sought (e.g. the repo itself).
	 * @return A Map with the actions or null if no actions are available.
	 * @throws Exception
	 */
	Map<String, Runnable> actions(Object... target) throws Exception;

	/**
	 * Return a tooltip for the given target or the encompassing entity if null
	 * is passed.
	 *
	 * @param target the target, any number of parameters to identify
	 * @return the tooltip or null
	 * @throws Exception
	 */
	String tooltip(Object... target) throws Exception;

	/**
	 * Provide a title for an element.
	 *
	 * @param target the target, any number of parameters to identify
	 * @return the text for this element
	 * @throws Exception
	 */

	String title(Object... target) throws Exception;
}
