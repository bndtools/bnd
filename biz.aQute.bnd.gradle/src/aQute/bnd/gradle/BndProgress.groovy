package aQute.bnd.gradle

import aQute.bnd.service.progress.ProgressPlugin
import aQute.bnd.service.progress.ProgressPlugin.Task

import java.text.DecimalFormat

import org.gradle.logging.ProgressLoggerFactory

/**
 * In order to enable this progress plugin add the following
 * line in settings.gradle after the workspace is created
 *
 * <pre>
 *  def workspace = new Workspace(rootDir, bnd_cnf)
 *  workspace.addBasicPlugin(new BndProgress(gradle))
 * </pre>
 */
class BndProgress implements ProgressPlugin {

  def gradle
  def percentFormat = new DecimalFormat("##%")
  def tasks = [:]

  BndProgress(def gradle) {
    this.gradle = gradle;
  }

  Task startTask(String name, int size) {
    // begin: use gradle internal api
    def serviceFactory = gradle.getServices()
    def progressLoggerFactory = serviceFactory.get(ProgressLoggerFactory.class)
    def progressLogger = progressLoggerFactory.newOperation(name)
    progressLogger.setDescription(name)
    progressLogger.setLoggingHeader(name)
    progressLogger.started()
    // end: use gradle internal api

    Task task = new Task() {
      public void worked(int units) {
        tasks[this].units += units
        def totalUnits = tasks.values().sum{ it.units }
        def totalSize = tasks.values().sum{ it.size }
        progressLogger.progress("Downloaded " + percentFormat.format(totalUnits / totalSize))
      }

      public void done(String message, Throwable e) {
        tasks.remove(this)
        progressLogger.completed()
      }

      @Override
      public boolean isCanceled() {
        return false
      }
    }

    tasks.put(task, [units:0, size:size])
    return task
  }
}
