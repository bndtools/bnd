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
 * configurations {
 *   baseline
 * }
 * dependencies {
 *     baseline('group': group, 'name': jar.archiveBaseName) {
 *       version {
 *         strictly "(0,${jar.archiveVersion.get()}["
 *       }
 *       transitive false
 *     }
 * }
 * tasks.register('baseline', Baseline) {
 *   bundle = jar
 *   baseline = configurations.baseline
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
 * <li>diffignore - These are the bundle headers or resource paths to ignore when
 * baselining. The default is nothing ignored.</li>
 * <li>diffpackages - These are the names of the exported packages to baseline.
 * The default is all exported packages.</li>
 * </ul>
 */

package aQute.bnd.gradle

import static aQute.bnd.gradle.BndUtils.builtBy
import static aQute.bnd.gradle.BndUtils.unwrap
import static org.gradle.api.tasks.PathSensitivity.RELATIVE

import aQute.bnd.differ.DiffPluginImpl
import aQute.bnd.header.Parameters
import aQute.bnd.osgi.Instructions
import aQute.bnd.osgi.Jar
import aQute.bnd.osgi.Processor
import aQute.bnd.version.Version
import aQute.lib.io.IO

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.model.ReplacedBy
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

public class Baseline extends DefaultTask {
  /**
   * Whether baseline failures should be ignored.
   *
   * <p>
   * If <code>true</code>, then baseline failures will not fail the task.
   * Otherwise, a baseline failure will fail the task. The default is
   * <code>false</code>.
   */
  @Input
  boolean ignoreFailures = false

  /**
   * The names of the exported packages to baseline.
   * <p>
   * The default is all exported packages.
   */
  @Input
  final ListProperty<String> diffpackages

  /**
   * The bundle headers or resource paths to ignore when
   * baselining.
   * <p>
   * The default is nothing ignored.
   */
  @Input
  final ListProperty<String> diffignore

  /**
   * The name of the baseline reports directory.
   *
   * <p>
   * Can be a name or a path relative to <code>ReportingExtension.getBaseDirectory()</code>.
   * The default name is 'baseline'.
   */
  @Internal('Represented by reportFile')
  final Property<String> baselineReportDirName

  /**
   * The baseline reports directory.
   *
   * <p>
   * The default is <code>ReportingExtension.getBaseDirectory().dir(baselineReportDirName)</code>.
   */
  @Internal('Represented by reportFile')
  final DirectoryProperty baselineReportDirectory

  /**
   * The baseline reports file.
   *
   * <p>
   * The default is <code>baselineReportDirectory.dir("${task.name}/${bundlename}.txt")</code>.
   */
  @OutputFile
  final RegularFileProperty reportFile

  private final ConfigurableFileCollection bundleCollection
  private final ConfigurableFileCollection baselineCollection

  private final ProjectLayout layout
  private final ProviderFactory providers

  /**
   * Create a Baseline task.
   *
   */
  public Baseline() {
    super()
    this.layout = project.layout
    this.providers = project.providers
    ObjectFactory objects = project.objects
    baselineReportDirName = objects.property(String.class).convention('baseline')
    baselineReportDirectory = objects.directoryProperty().convention(project.reporting.baseDirectory.dir(baselineReportDirName))
    String taskName = name
    reportFile = objects.fileProperty().convention(baselineReportDirectory.file(providers.provider({ ->
      String bundlename = unwrap(getBundle()).getName()
      bundlename = bundlename[0..Math.max(-1, bundlename.lastIndexOf('.') - 1)]
      return "${taskName}/${bundlename}.txt"
    })))
    diffignore = objects.listProperty(String.class).empty()
    diffpackages = objects.listProperty(String.class).empty()
    bundleCollection = objects.fileCollection()
    baselineCollection = objects.fileCollection()
    inputs.files(bundleCollection).withPathSensitivity(RELATIVE).withPropertyName('bundleCollection')
    inputs.files(baselineCollection).withPropertyName('baselineCollection')
  }

  /**
   * Return the bundle File to be baselined.
   *
   * <p>
   * An exception will be thrown if the set bundle does
   * not result in exactly one file.
   */
  @InputFile
  public Provider<RegularFile> getBundle() {
    ConfigurableFileCollection collection = bundleCollection
    return layout.file(providers.provider({ ->
      return collection.getSingleFile()
    }))
  }

  /**
   * Set the bundle to be baselined.
   *
   * <p>
   * The argument will be handled using
   * ConfigurableFileCollection.from().
   */
  public void setBundle(Object file) {
    bundleCollection.setFrom(file)
    builtBy(bundleCollection, file)
  }

  /**
   * Get the baseline bundle File.
   *
   * <p>
   * An exception will be thrown if the baseline argument does
   * not result in exactly one file.
   */
  @InputFile
  public Provider<RegularFile> getBaseline() {
    ConfigurableFileCollection collection = baselineCollection
    return layout.file(providers.provider({ ->
      return collection.getSingleFile()
    }))
  }

  /**
   * Set the baseline bundle from a File.
   * <p>
   * The argument will be handled using
   * ConfigurableFileCollection.from().
   */
  public void setBaseline(Object file) {
    baselineCollection.setFrom(file)
    builtBy(baselineCollection, file)
  }

  /**
   * Add diffignore values.
   */
  public void diffignore(String... diffignore) {
    getDiffignore().addAll(diffignore)
  }

  /**
   * Add diffpackages values.
   */
  public void diffpackages(String... diffpackages) {
    getDiffpackages().addAll(diffpackages)
  }

  @Internal('Used by baseline configuration')
  Task getBundleTask() {
    return bundleCollection.getBuiltBy().flatten().findResult { t ->
      if (t instanceof TaskProvider) {
        t = t.get()
      }
      t instanceof Task && t.convention.findPlugin(BundleTaskConvention.class) ? t : null
    }
  }

  @Deprecated
  @ReplacedBy('reportFile')
  public File getDestination() {
    return unwrap(getReportFile())
  }

  /**
   * Baseline the bundle.
   *
   */
  @TaskAction
  void baselineAction() {
    File bundle = unwrap(getBundle())
    File baseline = unwrap(getBaseline())
    File report = unwrap(getReportFile())
    IO.mkdirs(report.getParentFile())
    boolean failure = false
    new Processor().withCloseable { Processor processor ->
      Jar newer = new Jar(bundle)
      processor.addClose(newer)
      Jar older = new Jar(baseline)
      processor.addClose(older)
      logger.debug 'Baseline bundle {} against baseline {}', bundle, baseline

      def differ = new DiffPluginImpl()
      differ.setIgnore(new Parameters(unwrap(getDiffignore()).join(','), processor))
      def baseliner = new aQute.bnd.differ.Baseline(processor, differ)
      def infos = baseliner.baseline(newer, older, new Instructions(new Parameters(unwrap(getDiffpackages()).join(','), processor))).sort {it.packageName}
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
