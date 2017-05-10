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
import aQute.bnd.version.Version

import org.gradle.api.Buildable
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

public class Baseline extends DefaultTask {
  private ConfigurableFileCollection bundleCollection
  private ConfigurableFileCollection baselineCollection

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
    bundleCollection = project.files()
    baselineCollection = project.files()
    dependsOn {
      [bundleCollection, baselineCollection]
    }
  }

  /**
   * Set the bundle to be baselined.
   *
   * <p>
   * The argument will be handled using
   * Project.files().
   */
  public void setBundle(Object file) {
    bundleCollection = project.files(file)
    if (file instanceof Task || file instanceof Buildable) {
      bundleCollection.builtBy file
    }
  }

  /**
   * Return the bundle File to be baselined.
   *
   * <p>
   * An exception will be thrown if the set bundle does
   * not result in exactly one file.
   */
  @InputFile
  public File getBundle() {
    return bundleCollection.singleFile
  }

  /**
   * Set the baseline bundle from a File.
   * <p>
   * The argument will be handled using
   * Project.files().
   */
  public void setBaseline(Object file) {
    baselineCollection = project.files(file)
    if (file instanceof Task || file instanceof Buildable) {
      baselineCollection.builtBy file
    }
  }

  /**
   * Get the baseline bundle File.
   *
   * <p>
   * An exception will be thrown if the baseline argument does
   * not result in exactly one file.
   */
  @InputFile
  public File getBaseline() {
    return baselineCollection.singleFile
  }

  /**
   * Returns a file pointing to the baseline reporting directory.
   */
  public File getBaselineReportDir() {
    File dir = new File(baselineReportDirName)
    return dir.absolute ? dir : project.reporting.file(dir.path)
  }

  Task getBundleTask() {
    return bundleCollection?.builtBy.find()
  }
  ConfigurableFileCollection getBundleCollection() {
    return bundleCollection
  }
  ConfigurableFileCollection getBaselineCollection() {
    return baselineCollection
  }

  @OutputFile
  public File getDestination() {
    String bundlename = bundle.name
    bundlename = bundlename[0..Math.max(-1, bundlename.lastIndexOf('.') - 1)]
    return new File(baselineReportDir, "${name}/${bundlename}.txt")
  }

  /**
   * Baseline the bundle.
   *
   */
  @TaskAction
  void baselineBundle() {
    File report = destination
    project.mkdir(report.parent)
    boolean failure = false
    new Processor().withCloseable { Processor processor ->
      Jar newer = new Jar(bundle)
      processor.addClose(newer)
      Jar older = new Jar(baseline)
      processor.addClose(older)

      def baseliner = new aQute.bnd.differ.Baseline(processor, new DiffPluginImpl())
      def infos = baseliner.baseline(newer, older, null).sort {it.packageName}
      def bundleInfo = baseliner.getBundleInfo()
      new Formatter(report, 'UTF-8', Locale.US).withCloseable { Formatter f ->
        f.format '===============================================================%n'
        f.format '%s %s %s-%s',
          bundleInfo.mismatch ? '*' : ' ',
          bundleInfo.bsn,
          newer.getVersion(),
          older.getVersion()

        if (bundleInfo.mismatch) {
          failure = true
          if (bundleInfo.suggestedVersion != null) {
            f.format ' suggests %s', bundleInfo.suggestedVersion
          }
          f.format '%n%#2S', baseliner.getDiff()
        }

        f.format '%n===============================================================%n'

        String format = '%s %-50s %-10s %-10s %-10s %-10s %-10s %s%n'
        f.format format, ' ', 'Name', 'Type', 'Delta', 'New', 'Old', 'Suggest', 'If Prov.'

        infos.each { info ->
          def packageDiff = info.packageDiff
          f.format format,
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
            f.format '%#2S%n', packageDiff
          }
        }
      }
    }

    if (failure) {
      String msg = "Baseline problems detected. See the report in ${report}.\n${report.text}"
      if (ignoreFailures) {
        logger.error msg
      } else {
        throw new GradleException(msg)
      }
    }
  }
}
