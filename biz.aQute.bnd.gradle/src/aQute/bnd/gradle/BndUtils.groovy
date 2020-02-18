/**
 * BndUtils class.
 */

package aQute.bnd.gradle

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Buildable
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.logging.Logger
import org.gradle.util.GradleVersion

class BndUtils {
  private BndUtils() { }

  private static final boolean IS_GRADLE_COMPATIBLE_5_6 = GradleVersion.current().compareTo(GradleVersion.version('5.6')) >= 0

  public static void logReport(def report, Logger logger) {
    if (logger.isWarnEnabled()) {
      report.getWarnings().each { String msg ->
        def location = report.getLocation(msg)
        if (location && location.file) {
          logger.warn '{}:{}: warning: {}', location.file, location.line, msg
        } else {
          logger.warn 'warning: {}', msg
        }
      }
    }
    if (logger.isErrorEnabled()) {
      report.getErrors().each { String msg ->
        def location = report.getLocation(msg)
        if (location && location.file) {
          logger.error '{}:{}: error: {}', location.file, location.line, msg
        } else {
          logger.error 'error  : {}', msg
        }
      }
    }
  }

  @CompileStatic
  public static ConfigurableFileCollection builtBy(ConfigurableFileCollection collection, Object... paths) {
    return collection.builtBy(paths.findAll { path ->
      path instanceof Task || path instanceof TaskProvider || path instanceof Buildable
    })
  }

  @CompileStatic
  public static Object unwrap(Object value) {
    if (value instanceof Provider) {
      value = value.getOrNull()
    } 
    if (value instanceof FileSystemLocation) {
      value = value.getAsFile()
    } 
    return value
  }

  public static void jarLibraryElements(Project project, String configurationName) {
    if (IS_GRADLE_COMPATIBLE_5_6) {
      def attributes = project.configurations[configurationName].attributes
      if (attributes.getAttribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE)?.name != LibraryElements.JAR) {
        try {
          attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.objects.named(LibraryElements.class, LibraryElements.JAR))
          project.logger.info 'Set {}:{} configuration attribute {} to {}', project.path, configurationName, LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, attributes.getAttribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE)
        } catch (IllegalArgumentException e) {
          project.logger.info 'Unable to set {}:{} configuration attribute {} to {}', project.path, configurationName, LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, LibraryElements.JAR, e
        }
      }
    }
  }
}
