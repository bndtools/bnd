/**
 * BndProperties for Gradle.
 *
 * <p>
 * Add property access for bnd properties to projects that apply
 * the {@code biz.aQute.bnd} plugin.
 */

package aQute.bnd.gradle

class BndProperties {
  private final bndProject
  BndProperties(bndProject) {
    this.bndProject = bndProject
  }
  String get(String name) {
    String value = bndProject.getProperty(name)
    if (value instanceof String) {
      value = value.trim()
    }
    return value
  }
  Object get(String name, Object defaultValue) {
    def value = get(name)
    if (value == null) {
      value = defaultValue
      if (value instanceof String) {
        value = value.trim()
      }
    }
    return value
  }
  String propertyMissing(String name) {
    String value = get(name)
    if (value == null) {
      value = get(name.replace('_', '.'))
    }
    return value
  }
}
