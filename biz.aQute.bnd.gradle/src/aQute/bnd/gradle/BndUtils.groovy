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
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.logging.Logger
import org.gradle.util.GradleVersion

class BndUtils {
  private static final boolean IS_GRADLE_MIN_50 = GradleVersion.current().compareTo(GradleVersion.version("5.0"))>=0

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
  public static Object namedTask(Project project, String name) {
    if (IS_GRADLE_MIN_50) {
      return project.getTasks().named(name)
    } else {
      return project.getTasks().getByName(name)
    }
  }

  @CompileStatic
  public static <T extends Task> Object configureTask(Project project, String name, Action<? super T> configuration) {
    if (IS_GRADLE_MIN_50) {
      return project.getTasks().named(name, configuration)
    } else {
      return project.getTasks().getByName(name, configuration)
    }
  }

  @CompileStatic
  public static <T extends Task> Object createTask(Project project, String name, Action<? super T> configuration) {
    if (IS_GRADLE_MIN_50) {
      return project.getTasks().register(name, configuration)
    } else {
      return project.getTasks().create(name, configuration)
    }
  }

  @CompileStatic
  public static <T extends Task> Object createTask(Project project, String name, Class<T> type, Action<? super T> configuration) {
    if (IS_GRADLE_MIN_50) {
      return project.getTasks().register(name, type, configuration)
    } else {
      return project.getTasks().create(name, type, configuration)
    }
  }

  @CompileStatic
  public static <T extends Task> void configureEachTask(Project project, Class<T> type, Action<? super T> configuration) {
    if (IS_GRADLE_MIN_50) {
      project.getTasks().withType(type).configureEach(configuration)
    } else {
      project.getTasks().withType(type).all(configuration)
    }
  }

  @CompileStatic
  public static ConfigurableFileCollection builtBy(ConfigurableFileCollection collection, Object... paths) {
    if (IS_GRADLE_MIN_50) {
      return collection.builtBy(paths.findAll { path ->
        path instanceof Task || path instanceof TaskProvider || path instanceof Buildable
      })
    } else {
      return collection.builtBy(paths.findAll { path ->
        path instanceof Task || path instanceof Buildable
      })
    }
  }

  @CompileStatic
  public static Object toTask(Object t) {
    if (IS_GRADLE_MIN_50) {
      if (t instanceof TaskProvider) {
        return t.get()
      }
    }
    return t
  }
}
