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
 *     baseline('group': group, 'name': jar.baseName, 'version': "(,${jar.version})") {
 *       transitive false
 *     }
 *   }
 * }
 * task baseline(type: Baseline) {
 *   bundle jar
 *   baseline configurations.baseline
 * }
 * </pre>
 *
 * <p>
 * Properties:
 * <ul>
 * <li>ignoreFailures - If true the build will not fail due to baseline
 * problems; instead an error message will be logged. Otherwise, the
 * build will fail. The default is false.</li>
 * <li>baselineReportDirName - This is the name of the baseline reports
 * directory. Can be a name or a path relative to ReportingExtension.getBaseDir().
 * The default name is 'baseline'.</li>
 * <li>bundle - This is the bundle to be baselined. It can either be a
 * File or a task that produces a bundle. This property must be set.</li>
 * <li>baseline - This is the baseline bundle. It can either be a File
 * or a Configuration. If a Configuration is specified, it must contain
 * a single file; otherwise an exception will fail the build. This property
 * must be set.</li>
 * </ul>
 */

package aQute.bnd.gradle

import aQute.bnd.differ.DiffPluginImpl
import aQute.bnd.osgi.Jar
import aQute.bnd.osgi.Processor
import aQute.bnd.service.diff.Delta
import aQute.bnd.service.diff.Type
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
  private AbstractArchiveTask bundleTask
  private File baseline
  private Configuration baselineConfiguration

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
    dependsOn {
      bundleTask
    }
  }

  /**
   * Set the bundle to be baselined from a File.
   */
  public void setBundle(File file) {
    bundle = file
    bundleTask = null
  }

  /**
   * Set the bundle to be baselined from an archive task.
   *
   * <p>
   * The bundle File is obtained from the archivePath property
   * of the task. This task will also dependOn the specified task.
   */
  public void setBundle(AbstractArchiveTask archive) {
    bundleTask = archive
    bundle = null
  }

  /**
   * Return the bundle File to be baselined.
   */
  @InputFile
  public File getBundle() {
    return (bundleTask != null) ? bundleTask.archivePath : bundle
  }

  /**
   * Return the archive task whose bundle is to be baselined.
   *
   * <p>
   * This will be null if the bundle to be baselined was not
   * set with a task.
   */
  public AbstractArchiveTask getBundleTask() {
    return bundleTask
  }

  /**
   * Set the baseline bundle from a File.
   */
  public void setBaseline(File file) {
    baseline = file
    baselineConfiguration = null
  }

  /**
   * Set the baseline bundle from a configuration.
   */
  public void setBaseline(Configuration configuration) {
    baselineConfiguration = configuration
    baseline = null
  }

  /**
   * Get the baseline bundle File.
   *
   * <p>
   * If the baseline was specified as a configuration,
   * an exception will be thrown if the configuration does
   * not contain a single file.
   */
  @InputFile
  public File getBaseline() {
    return (baselineConfiguration != null) ? baselineConfiguration.singleFile : baseline
  }

  /**
   * Return the baseline configuration.
   *
   * <p>
   * This will be null if the bundle is not to be baselined
   * with a configuration.
   */
  public Configuration getBaselineConfiguration() {
    return baselineConfiguration
  }

  /**
   * Returns a file pointing to the baseline reporting directory.
   */
  public File getBaselineReportDir() {
    File dir = new File(baselineReportDirName)
    return dir.absolute ? dir : new File(project.reporting.baseDir, dir.path)
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
      def infos = baseliner.baseline(newer, older, null).sort {it.packageName}
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

        String format = '%s %-50s %-10s %-10s %-10s %-10s %-10s %s%n'
        writer.printf format, ' ', 'Name', 'Type', 'Delta', 'New', 'Old', 'Suggest', 'If Prov.'

        infos.each { info ->
          def packageDiff = info.packageDiff
          writer.printf format,
            info.mismatch ? '*' : ' ',
            packageDiff.getName(),
            packageDiff.getType(),
            packageDiff.getDelta(),
            info.newerVersion,
            info.olderVersion != null && info.olderVersion.equals(Version.LOWEST) ? '-': info.olderVersion,
            info.suggestedVersion != null && info.suggestedVersion.compareTo(info.newerVersion) <= 0 ? 'ok' : info.suggestedVersion,
            info.suggestedIfProviders ?: '-'
          if (info.mismatch) {
            failure = true
            packageDiff.getChildren().findAll { typeDiff ->
              typeDiff.getDelta() != Delta.UNCHANGED
            }.each { typeDiff ->
              writer.printf '*  %-49s %-10s %s%n', typeDiff.getName(), typeDiff.getType(), typeDiff.getDelta()
              typeDiff.getChildren().findAll { memberDiff ->
                memberDiff.getDelta() != Delta.UNCHANGED
              }.each { memberDiff ->
                writer.printf '*   %-48s %-10s %s%n', memberDiff.getName(), memberDiff.getType(), memberDiff.getDelta()
                memberDiff.getChildren().findAll { childDiff ->
                  childDiff.getDelta() != Delta.UNCHANGED
                }.each { childDiff ->
                  writer.printf '*    %-47s %-10s %s%n', childDiff.getName(), childDiff.getType(), childDiff.getDelta()
                }
              }
            }
          }
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
