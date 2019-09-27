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

  def get(String name) {
    return trimmed(project.getProperty(name))
  }

  def get(String name, Object defaultValue) {
    def value = get(name)
    if (value != null) {
      return value
    }
    return trimmed(defaultValue)
  }

  def merge(String name) {
    return trimmed(project.mergeProperties(name))
  }

  def process(String line) {
    return project.getReplacer().process(line)
  }

  def propertyMissing(String name) {
    if ((name == 'ext') || extensions.extraProperties.has(name) || extensions.findByName(name)) {
      throw new MissingPropertyException(name, String)
    }
    def value = get(name)
    if (value != null) {
      return value
    }
    return get(name.replace('_', '.'))
  }

  def trimmed(value) {
    return (value instanceof String) ? value.trim() : value
  }
}
