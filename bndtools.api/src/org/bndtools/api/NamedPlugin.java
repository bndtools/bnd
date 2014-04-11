package org.bndtools.api;

/**
 * <p>
 * The interface of a generic - named - plugin.
 * </p>
 * <p>
 * A named plugin declares its name and whether it's enabled by default. These
 * declarations are used in UI elements.
 * </p>
 * <p>
 * Note: only plugins in the bndtools source tree are allowed to declare
 * themselves enabled by default.
 * </p>
 */
public interface NamedPlugin {
	/**
	 * @return The human-readable name of the plugin, used in UI elements. Must
	 *         be unique.
	 */
	public String getName();

	/**
	 * @return true when the plugin is enabled by default. Note: only plugins in
	 *         the bndtools source tree are allowed to return true.
	 */
	public boolean isEnabledByDefault();

	/**
	 * @return true when the plugin is deprecated, which will result in it never
	 *         being enabled by default.
	 */
	public boolean isDeprecated();
}