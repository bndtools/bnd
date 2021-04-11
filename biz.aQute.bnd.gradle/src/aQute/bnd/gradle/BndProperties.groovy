/**
 * BndProperties for Gradle.
 *
 * <p>
 * Add property access for bnd properties to projects that apply
 * the {@code biz.aQute.bnd} plugin.
 */

package aQute.bnd.gradle

import aQute.bnd.build.Project

class BndProperties {
	final Project project
	BndProperties(Project bndProject) {
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

	Object propertyMissing(String name) {
		if (Objects.equals(name, 'ext') || extensions.extraProperties.has(name) || extensions.findByName(name)) {
			throw new MissingPropertyException(name, String)
		}
		var value = get(name)
		if (Objects.nonNull(value)) {
			return value
		}
		return get(name.replace('_', '.'))
	}

	Object trimmed(value) {
		return (value instanceof String) ? value.trim() : value
	}
}
