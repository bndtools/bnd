package aQute.bnd.gradle;

import java.util.Objects;

import aQute.bnd.build.Project;
import groovy.lang.MissingPropertyException;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtraPropertiesExtension;

/**
 * BndPluginExtension for Gradle.
 * <p>
 * Add property access for bnd properties to projects that apply the
 * {@code biz.aQute.bnd} plugin.
 */
public abstract class BndPluginExtension implements ExtensionAware {
	/**
	 * Name of the extension.
	 */
	public static final String	NAME	= "bnd";
	private final Project		project;

	/**
	 * Create a BndPlugin extension.
	 *
	 * @param bndProject The Bnd Project for the extension.
	 */
	public BndPluginExtension(Project bndProject) {
		this.project = bndProject;
	}

	/**
	 * Return the Bnd Project for the extension.
	 *
	 * @return The Bnd Project for the extension.
	 */
	public Project getProject() {
		return project;
	}

	/**
	 * Return a boolean value for the specified property.
	 *
	 * @param name The property name.
	 * @return A boolean value for the specified property.
	 */
	public boolean is(String name) {
		return project.is(name);
	}

	/**
	 * Return the trimmed value of the specified property.
	 *
	 * @param name The property name.
	 * @return The trimmed value of the specified property.
	 */
	public String get(String name) {
		return trimmed(project.getProperty(name));
	}

	/**
	 * Return the trimmed value of the specified property.
	 *
	 * @param name The property name.
	 * @param defaultValue The default value if the specified property does not
	 *            exist.
	 * @return The trimmed value of the specified property.
	 */
	public Object get(String name, Object defaultValue) {
		String value = project.getProperty(name);
		if (Objects.nonNull(value)) {
			return value.trim();
		}
		return trimmed(defaultValue);
	}

	/**
	 * Return the trimmed value of the specified merged property.
	 *
	 * @param name The property name.
	 * @return The trimmed value of the specified merged property.
	 */
	public String merge(String name) {
		return trimmed(project.mergeProperties(name));
	}

	/**
	 * Return the macro processed value of the specified line.
	 *
	 * @param line The line to macro process.
	 * @return The macro processed value of the specified line.
	 */
	public String process(String line) {
		return project.getReplacer()
			.process(line);
	}

	/**
	 * Return the trimmed unprocessed value of the specified property.
	 *
	 * @param name The property name.
	 * @param defaultValue The default value if the specified property does not
	 *            exist.
	 * @return The trimmed unprocessed value of the specified property.
	 */
	public Object unprocessed(String name, Object defaultValue) {
		String value = project.getUnprocessedProperty(name, null);
		if (Objects.nonNull(value)) {
			return value.trim();
		}
		return trimmed(defaultValue);
	}

	@SuppressWarnings("unchecked")
	private static <O> O trimmed(O value) {
		return (value instanceof String) ? (O) ((String) value).trim() : value;
	}

	/**
	 * Handle a missing property.
	 *
	 * @param name The requested property name.
	 * @return A value for the requested property.
	 * @throws MissingPropertyException If this method cannot supply a value.
	 */
	public String propertyMissing(String name) {
		if (Objects.equals(name, ExtraPropertiesExtension.EXTENSION_NAME) || getExtensions().getExtraProperties()
			.has(name) || Objects.nonNull(getExtensions().findByName(name))) {
			throw new MissingPropertyException(name, String.class);
		}
		String value = get(name);
		if (Objects.nonNull(value)) {
			return value;
		}
		return get(name.replace('_', '.'));
	}
}
