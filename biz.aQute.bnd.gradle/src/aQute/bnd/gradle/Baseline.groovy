/**
 * Baseline task type for Gradle.
 *
 * <p>
 * This task type can be used to baseline a bundle.
 *
 * <p>
 * Here is an example of using the Baseline task type:
 * <pre>
 * import aQute.bnd.gradle.Baseline
 * apply plugin: 'java'
 * configurations {
 *   baseline
 * }
 * dependencies {
 *     baseline('group': group, 'name': archivesBaseName, 'version': "(,${version})") {
 *       transitive false
 *     }
 *   }
 * }
 * task baseline(type: Baseline) {
 *   bundle jar
 *   baseline configurations.baseline
 * }
 * </pre>
 */

package aQute.bnd.gradle

import aQute.bnd.differ.DiffPluginImpl
import aQute.bnd.osgi.Jar
import aQute.bnd.osgi.Processor
import aQute.bnd.version.Version

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.AbstractArchiveTask

public class Baseline extends DefaultTask {
  private File bundle
  private File baseline

  /**
   * Whether baseline failures should be ignored.
   *
   * <p>
   * If <code>true</code>, then baseline failures will not fail the task.
   * Otherwise, a baseline failure will fail the task. The default is
   * <code>false</code>.
   */
  @Input
  boolean ignoreFailures

  /**
   * The name of the baseline reports directory. 
   *
   * <p>
   * Can be a name or a path relative to <code>ReportingExtension.getBaseDir()</code>.
   * The default name is 'baseline'.
   */
  @Input
  String baselineReportDirName

  /**
   * Create a Baseline task.
   *
   */
  public Baseline() {
    super()
    ignoreFailures = false
    baselineReportDirName = 'baseline'
  }

  /**
   * Set the bundle to be baselined from a File.
   */
  public void setBundle(File file) {
    bundle = file
  }

  /**
   * Set the bundle to be baselined from an archive task.
   *
   * <p>
   * The bundle File is obtained from the archivePath property
   * of the task. This task will also dependOn the specified task.
   */
  public void setBundle(AbstractArchiveTask archive) {
    dependsOn archive
    bundle = archive.archivePath
  }

  /**
   * Return the bundle File to be baselined.
   */
  @InputFile
  public File getBundle() {
    return bundle
  }

  /**
   * Set the baseline bundle from a File.
   */
  public void setBaseline(File file) {
    baseline = file
  }

  /**
   * Set the baseline bundle from a configuration.
   *
   * <p>
   * The specified configuration must contain only a single
   * file otherwise an exception will be thrown.
   */
  public void setBaseline(Configuration configuration) {
    baseline = configuration.singleFile
  }

  /**
   * Get the baseline bundle File.
   */
  @InputFile
  public File getBaseline() {
    return baseline
  }

  /**
   * Returns a file pointing to the baseline reporting directory.
   */
  public File getBaselineReportDir() {
    File dir = new File(baselineReportDirName)
    if (dir.absolute) {
      return dir
    }
    return new File(project.reporting.baseDir, dir.path)
  }

  /**
   * Baseline the bundle.
   *
   */
  @TaskAction
  void baselineBundle() {
    File report = new File(baselineReportDir, "${name}.txt")
    project.mkdir(report.parent)
    boolean failure = false
    new Processor().withCloseable { processor ->
      Jar newer = new Jar(bundle)
      processor.addClose(newer)
      Jar older = new Jar(baseline)
      processor.addClose(older)

      def baseliner = new aQute.bnd.differ.Baseline(processor, new DiffPluginImpl())
      def infos = baseliner.baseline(newer, older, null).toSorted {it.packageName}
      def bundleInfo = baseliner.getBundleInfo()
      report.withPrintWriter('UTF-8') { writer ->
        writer.println '==============================================================='
        writer.printf '%s %s %s-%s',
          bundleInfo.mismatch ? '*' : ' ',
          bundleInfo.bsn,
          newer.getVersion(),
          older.getVersion()

        if (bundleInfo.mismatch) {
          failure = true
          if (bundleInfo.suggestedVersion != null) {
            writer.print " suggests ${bundleInfo.suggestedVersion}"
          }
        }

        writer.println()
        writer.println '==============================================================='

        String format = '%s %-50s %-10s %-10s %-10s %-10s %-10s%n'
        writer.printf format, ' ', 'Package', 'Delta', 'New', 'Old', 'Suggest', 'If Prov.'

        infos.each { info ->
          if (info.mismatch) {
            failure = true
          }
          writer.printf format,
            info.mismatch ? '*' : ' ',
            info.packageName,
            info.packageDiff.getDelta(),
            info.newerVersion,
            info.olderVersion != null && info.olderVersion.equals(Version.LOWEST) ? '-': info.olderVersion,
            info.suggestedVersion != null && info.suggestedVersion.compareTo(info.newerVersion) <= 0 ? 'ok' : info.suggestedVersion,
            info.suggestedIfProviders == null ? '-' : info.suggestedIfProviders
        }
      }
    }

    if (failure) {
      String msg = "Baseline problems detected. See the report in ${report}."
      if (ignoreFailures) {
        logger.error msg
      } else {
        throw new GradleException(msg)
      }
    }
  }
}
