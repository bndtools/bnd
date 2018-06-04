/**
 * BndUtils class.
 */

package aQute.bnd.gradle

import org.gradle.api.logging.Logger

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
}
