package aQute.bnd.gradle

import aQute.bnd.build.Project

/**
 * BndPluginExtension for Gradle.
 *
 * <p>
 * Add property access for bnd properties to projects that apply
 * the {@code biz.aQute.bnd} plugin.
 */
class BndPluginExtension {
	final Project project
	BndPluginExtension(Project bndProject) {
		this.project = bndProject
	}

	boolean is(String name) {
		return project.is(name)
	}

	Object get(String name) {
		return trimmed(project.getProperty(name))
	}

	Object get(String name, Object defaultValue) {
		var value = get(name)
		if (Objects.nonNull(value)) {
			return value
		}
		return trimmed(defaultValue)
	}

	Object merge(String name) {
		return trimmed(project.mergeProperties(name))
	}

	Object process(String line) {
		return project.getReplacer().process(line)
	}

	Object unprocessed(String name, Object defaultValue) {
		var value = project.getUnprocessedProperty(name, null)
		if (Objects.isNull(value)) {
			value = defaultValue
		}
		if (value instanceof String) {
			value = value.trim()
		}
		return value
	}

	Object propertyMissing(String name) {
		if (Objects.equals(name, "ext") || extensions.extraProperties.has(name) || extensions.findByName(name)) {
			throw new MissingPropertyException(name, String)
		}
		var value = get(name)
		if (Objects.nonNull(value)) {
			return value
		}
		return get(name.replace((char) '_', (char) '.'))
	}

	Object trimmed(value) {
		return (value instanceof String) ? value.trim() : value
	}
}
