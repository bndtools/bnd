package aQute.bnd.gradle;

import aQute.bnd.build.Project;
import groovy.lang.MissingPropertyException;

/**
 * BndPluginConvention for Gradle.
 *
 * @deprecated Replaced by BndPluginExtension.
 */
@Deprecated
public class BndPluginConvention {
	private final BndPluginExtension bnd;

	/**
	 * Create a BndPlugin convention.
	 *
	 * @param extension The BndPlugin extension.
	 */
	public BndPluginConvention(BndPluginExtension extension) {
		this.bnd = extension;
	}

	/**
	 * Return the Bnd Project for the convention.
	 *
	 * @return The Bnd Project for the convention.
	 */
	public Project getProject() {
		return bnd.getProject();
	}

	/**
	 * Return a boolean value for the specified property.
	 *
	 * @param name The property name.
	 * @return A boolean value for the specified property.
	 */
	public boolean bndis(String name) {
		return bnd.is(name);
	}

	/**
	 * Return the trimmed value of the specified property.
	 *
	 * @param name The property name.
	 * @return The trimmed value of the specified property.
	 */
	public String bnd(String name) {
		return bnd.get(name);
	}

	/**
	 * Return the trimmed value of the specified merged property.
	 *
	 * @param name The property name.
	 * @return The trimmed value of the specified merged property.
	 */
	public String bndMerge(String name) {
		return bnd.merge(name);
	}

	/**
	 * Return the trimmed value of the specified property.
	 *
	 * @param name The property name.
	 * @param defaultValue The default value if the specified property does not
	 *            exist.
	 * @return The trimmed value of the specified property.
	 */
	public Object bnd(String name, Object defaultValue) {
		return bnd.get(name, defaultValue);
	}

	/**
	 * Return the macro processed value of the specified line.
	 *
	 * @param line The line to macro process.
	 * @return The macro processed value of the specified line.
	 */
	public String bndProcess(String line) {
		return bnd.process(line);
	}

	/**
	 * Return the trimmed unprocessed value of the specified property.
	 *
	 * @param name The property name.
	 * @param defaultValue The default value if the specified property does not
	 *            exist.
	 * @return The trimmed unprocessed value of the specified property.
	 */
	public Object bndUnprocessed(String name, Object defaultValue) {
		return bnd.unprocessed(name, defaultValue);
	}

	/**
	 * Handle a missing property.
	 *
	 * @param name The requested property name.
	 * @return A value for the requested property.
	 * @throws MissingPropertyException If this method cannot supply a value.
	 */
	public String propertyMissing(String name) {
		return bnd.propertyMissing(name);
	}
}
