/**
 * BndPluginConvention for Gradle.
 *
 * <p>
 * Adds bnd and bndUnprocessed methods to projects that apply
 * the {@code biz.aQute.bnd} plugin.
 */

package aQute.bnd.gradle

import org.gradle.api.Project

class BndPluginConvention {
  private final Project project
  BndPluginConvention(BndPlugin plugin) {
   this.project = plugin.project
  }
  boolean bndis(String name) {
    return project.bnd.is(name)
  }
  String bnd(String name) {
    return project.bnd.get(name)
  }
  String bndMerge(String name) {
    return project.bnd.merge(name)
  }
  Object bnd(String name, Object defaultValue) {
    return project.bnd.get(name, defaultValue)
  }
  String bndProcess(String line) {
    return project.bnd.process(line)
  }
  Object bndUnprocessed(String name, Object defaultValue) {
    def value = project.bnd.project.getUnprocessedProperty(name, null)
    if (value == null) {
      value = defaultValue
    }
    if (value instanceof String) {
      value = value.trim()
    }
    return value
  }
}
