/**
 * BndProperties for Gradle.
 *
 * <p>
 * Add property access for bnd properties to projects that apply
 * the {@code biz.aQute.bnd} plugin.
 */

package aQute.bnd.gradle

import aQute.bnd.build.Project
import org.gradle.api.plugins.ExtraPropertiesExtension

class BndProperties {
  final Project project
  BndProperties(Project bndProject) {
    this.project = bndProject
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

  def propertyMissing(String name) {
    if (name == 'ext') {
      throw new MissingPropertyException(name, String)
    }
    ExtraPropertiesExtension ext = extensions.extraProperties
    if (ext.has(name)) {
      return ext.get(name)
    }
    def value = extensions.findByName(name)
    if (value != null) {
      return value
    }
    value = get(name)
    if (value != null) {
      return value
    }
    return get(name.replace('_', '.'))
  }

  def trimmed(value) {
    return (value instanceof String) ? value.trim() : value
  }
}
