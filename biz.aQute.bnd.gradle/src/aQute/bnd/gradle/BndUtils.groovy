/**
 * BndUtils class.
 */

package aQute.bnd.gradle

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Buildable
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.logging.Logger
import org.gradle.util.GradleVersion

class BndUtils {
  private BndUtils() { }

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
}
