package aQute.bnd.gradle;

import java.util.Objects;

import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtraPropertiesExtension;

import aQute.bnd.build.Project;
import groovy.lang.MissingPropertyException;

/**
 * BndPluginExtension for Gradle.
 * <p>
 * Add property access for bnd properties to projects that apply the
 * {@code biz.aQute.bnd} plugin.
 */
public abstract class BndPluginExtension implements ExtensionAware {
	public static final String	NAME	= "bnd";
	private final Project		project;

	public BndPluginExtension(Project bndProject) {
		this.project = bndProject;
	}

	public Project getProject() {
		return project;
	}

	public boolean is(String name) {
		return project.is(name);
	}

	public String get(String name) {
		return trimmed(project.getProperty(name));
	}

	public Object get(String name, Object defaultValue) {
		String value = project.getProperty(name);
		if (Objects.nonNull(value)) {
			return value.trim();
		}
		return trimmed(defaultValue);
	}

	public String merge(String name) {
		return trimmed(project.mergeProperties(name));
	}

	public String process(String line) {
		return project.getReplacer()
			.process(line);
	}

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
