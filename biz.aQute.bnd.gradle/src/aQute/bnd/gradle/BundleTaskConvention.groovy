/**
 * BundleTaskConvention for Gradle.
 *
 * <p>
 * Adds properties to bundle builder tasks.
 *
 * <p>
 * Properties:
 * <ul>
 * <li>bndfile - This is the name of the bnd file to use to make the bundle.
 * The bndfile does not need
 * to exist. It will override headers in the jar task's manifest.</li>
 * <li>bnd - This is a string holding a bnd file to use to make the bundle.
 * It will override headers in the jar task's manifest.
 * This properties is ignored if bndfile is specified and the specified file
 * exists.</li>
 * <li>sourceSet - This is the SourceSet to use for the
 * bnd builder. It defaults to 'project.sourceSets.main'.</li>
 * <li>classpath - This is the FileCollection to use for the buildpath
 * for the bnd builder. It defaults to 'sourceSet.compileClasspath'.</li>
 * </ul>
 */

package aQute.bnd.gradle

import static aQute.bnd.gradle.BndUtils.builtBy
import static aQute.bnd.gradle.BndUtils.jarLibraryElements
import static aQute.bnd.gradle.BndUtils.logReport
import static aQute.bnd.gradle.BndUtils.unwrap
import static org.gradle.api.tasks.PathSensitivity.RELATIVE

import java.util.Properties
import java.util.jar.Manifest
import java.util.zip.ZipException
import java.util.zip.ZipFile

import aQute.bnd.osgi.Builder
import aQute.bnd.osgi.Constants
import aQute.bnd.osgi.Jar
import aQute.bnd.osgi.Processor
import aQute.bnd.version.MavenVersion
import aQute.lib.io.IO
import aQute.lib.utf8properties.UTF8Properties

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.SourceSet

class BundleTaskConvention {
  /**
   * The bndfile property.
   *
   * <p>
   * A bnd file containing bnd instructions for this project.
   */
  @InputFile
  @PathSensitive(RELATIVE)
  @Optional
  final RegularFileProperty bndfile

  /**
   * The classpath property.
   *
   * <p>
   * The default value is sourceSets.main.compileClasspath.
   */
  @InputFiles
  final ConfigurableFileCollection classpath

  private final Task task
  private final Project project
  private final ProjectLayout layout
  private final ObjectFactory objects
  private final File buildFile
  private final ListProperty<CharSequence> instructions
  private final Provider<String> bndbnd
  private boolean classpathModified
  private SourceSet sourceSet

  /**
   * Create a BundleTaskConvention for the specified Jar task.
   *
   * <p>
   * This also sets the default values for the added properties
   * and adds the bnd file to the task inputs.
   */
  BundleTaskConvention(org.gradle.api.tasks.bundling.Jar task) {
    this.task = task
    this.project = task.getProject()
    layout = project.getLayout()
    objects = project.getObjects()
    buildFile = project.getBuildFile()
    bndfile = objects.fileProperty()
    instructions = objects.listProperty(CharSequence.class).empty()
    bndbnd = instructions.map({ it.join('\n') })
    classpath = objects.fileCollection()
    setSourceSet(project.sourceSets.main)
    classpathModified = false
    // need to programmatically add to inputs since @InputFiles in a convention is not processed
    task.inputs.files(getClasspath()).withPropertyName('classpath')
    task.inputs.file(getBndfile()).optional().withPathSensitivity(RELATIVE).withPropertyName('bndfile')
    task.inputs.property('bnd', getBnd())
  }

  /**
   * Set the bndfile property.
   *
   * <p>
   * The argument will be handled using
   * Project.file().
   */
  public void setBndfile(String file) {
    getBndfile().set(project.file(file))
  }

  /**
   * Set the bndfile property.
   *
   * <p>
   * The argument will be handled using
   * Project.file().
   */
  public void setBndfile(Object file) {
    getBndfile().set(project.file(file))
  }

  /**
   * Get the bnd property.
   * <p>
   * If the bndfile property points an existing file, this property is ignored.
   * Otherwise, the bnd instructions in this property will be used.
   */
  @Input
  @Optional
  public Provider<String> getBnd() {
    return bndbnd
  }

  /**
   * Set the bnd property from a multi-line string.
   */
  public void setBnd(CharSequence line) {
     instructions.empty()
     bnd(line)
  }

  /**
   * Set the bnd property from a multi-line string using a
   * {@link Provider<? extends CharSequence>}.
   */
  public void setBnd(Provider<? extends CharSequence> lines) {
    instructions.empty()
    bnd(lines)
  }

  /**
   * Add instructions to the bnd property from a list of multi-line strings.
   */
  public void bnd(CharSequence... lines) {
    instructions.addAll(lines)
  }

  /**
   * Add a multi-line string of instructions to the bnd property
   * using a {@link Provider<? extends CharSequence>}.
   */
  public void bnd(Provider<? extends CharSequence> lines) {
    instructions.add(lines)
  }

  /**
   * Set the bnd property from a map.
   */
  public void setBnd(Map<String, ?> map) {
     instructions.empty()
     bnd(map)
  }

  /**
   * Add instructions to the bnd property from a map.
   */
  public void bnd(Map<String, ?> map) {
    map.each({ key, value ->
      instructions.add("${key}=${value}")
    })
  }

  /**
   * Add files to the classpath.
   *
   * <p>
   * The arguments will be handled using
   * ConfigurableFileCollection.from().
   */
  public ConfigurableFileCollection classpath(Object... paths) {
    classpathModified = true
    return builtBy(getClasspath().from(paths), paths)
  }

  /**
   * Set the classpath property.
   *
   * <p>
   * The argument will be handled using
   * ConfigurableFileCollection.from().
   */
  public void setClasspath(Object path) {
    getClasspath().setFrom(Collections.emptyList())
    getClasspath().setBuiltBy(Collections.emptyList())
    classpath(path)
  }

  /**
   * Get the sourceSet property.
   */
  public SourceSet getSourceSet() {
    return sourceSet
  }

  /**
   * Set the sourceSet property.
   */
  public void setSourceSet(SourceSet sourceSet) {
    this.sourceSet = sourceSet
    jarLibraryElements(project, sourceSet.compileClasspathConfigurationName)
    if (!classpathModified) {
      setClasspath(sourceSet.compileClasspath)
      classpathModified = false
    }
  }

  void buildBundle() {
    task.configure {
      def projectDir = unwrap(layout.getProjectDirectory())
      def buildDir = unwrap(layout.getBuildDirectory())
      // create Builder
      Properties gradleProperties = new PropertiesWrapper()
      gradleProperties.put('task', task)
      gradleProperties.put('project', project)
      new Builder(new Processor(gradleProperties, false)).withCloseable { Builder builder ->
        // load bnd properties
        File temporaryBndFile = File.createTempFile('bnd', '.bnd', temporaryDir)
        temporaryBndFile.withWriter('UTF-8') { writer ->
          // write any task manifest entries into the tmp bnd file
          manifest.effectiveManifest.attributes.inject(new UTF8Properties()) { properties, key, value ->
            if (key != 'Manifest-Version') {
              properties.setProperty(key, value.toString())
            }
            return properties
          }.replaceHere(projectDir).store(writer, null)

          // if the bnd file exists, add its contents to the tmp bnd file
          File bndfile = unwrap(getBndfile(), true)
          if (bndfile?.isFile()) {
            builder.loadProperties(bndfile).store(writer, null)
          } else {
            String bnd = unwrap(getBnd())
            if (!bnd.empty) {
              UTF8Properties props = new UTF8Properties()
              props.load(bnd, buildFile, builder)
              props.replaceHere(projectDir).store(writer, null)
            }
          }
        }
        builder.setProperties(temporaryBndFile, projectDir) // this will cause project.dir property to be set
        builder.setProperty('project.output', buildDir.getCanonicalPath())

        // If no bundle to be built, we have nothing to do
        if (builder.is(Constants.NOBUNDLES)) {
          return
        }

        // Reject sub-bundle projects
        if (builder.getSubBuilders() != [builder]) {
          throw new GradleException('Sub-bundles are not supported by this task')
        }

        File archiveFile = unwrap(task.getArchiveFile())
        String archiveFileName = unwrap(task.getArchiveFileName())
        String archiveBaseName = unwrap(task.getArchiveBaseName())
        String archiveClassifier = unwrap(task.getArchiveClassifier())
        String archiveVersion = unwrap(task.getArchiveVersion(), true)

        // Include entire contents of Jar task generated jar (except the manifest)
        File archiveCopyFile = new File(temporaryDir, archiveFileName)
        IO.copy(archiveFile, archiveCopyFile)
        Jar bundleJar = new Jar(archiveFileName, archiveCopyFile)
        String reproducible = builder.getProperty(Constants.REPRODUCIBLE)
        bundleJar.setReproducible((reproducible != null) ? Processor.isTrue(reproducible) : !task.isPreserveFileTimestamps())
        bundleJar.updateModified(archiveFile.lastModified(), 'time of Jar task generated jar')
        bundleJar.setManifest(new Manifest())
        builder.setJar(bundleJar)

        // set builder classpath
        ConfigurableFileCollection buildpath = objects.fileCollection().from(getClasspath().files.findAll { File file ->
          if (!file.exists()) {
            return false
          }
          if (file.directory) {
            return true
          }
          try {
            new ZipFile(file).withCloseable { ZipFile zip ->
              zip.entries() // make sure it is a valid zip file and not a pom
            }
          } catch (ZipException e) {
            return false
          }
          return true
        })
        builder.setProperty('project.buildpath', buildpath.getAsPath())
        builder.setClasspath(buildpath.files as File[])
        logger.debug 'builder classpath: {}', builder.getClasspath()*.getSource()

        // set builder sourcepath
        ConfigurableFileCollection sourcepath = objects.fileCollection().from(getSourceSet().allSource.srcDirs.findAll{it.exists()})
        builder.setProperty('project.sourcepath', sourcepath.getAsPath())
        builder.setSourcepath(sourcepath.files as File[])
        logger.debug 'builder sourcepath: {}', builder.getSourcePath()


        // set bundle symbolic name from tasks's archiveBaseName property if necessary
        String bundleSymbolicName = builder.getProperty(Constants.BUNDLE_SYMBOLICNAME)
        if (isEmpty(bundleSymbolicName)) {
          bundleSymbolicName = archiveClassifier.empty ? archiveBaseName : "${archiveBaseName}-${archiveClassifier}"
          builder.setProperty(Constants.BUNDLE_SYMBOLICNAME, bundleSymbolicName)
        }

        // set bundle version from task's archiveVersion if necessary
        String bundleVersion = builder.getProperty(Constants.BUNDLE_VERSION)
        if (isEmpty(bundleVersion)) {
          builder.setProperty(Constants.BUNDLE_VERSION, MavenVersion.parseMavenString(archiveVersion).getOSGiVersion().toString())
        }

        logger.debug 'builder properties: {}', builder.getProperties()

        // Build bundle
        Jar builtJar = builder.build()
        if (!builder.isOk()) {
          // if we already have an error; fail now
          logReport(builder, logger)
          failTask("Bundle ${archiveFileName} has errors", archiveFile)
        }

        // Write out the bundle
        builtJar.write(archiveFile)
        long now = System.currentTimeMillis()
        archiveFile.setLastModified(now)

        logReport(builder, logger)
        if (!builder.isOk()) {
          failTask("Bundle ${archiveFileName} has errors", archiveFile)
        }
      }
    }
  }

  private void failTask(String msg, File archiveFile) {
    IO.delete(archiveFile)
    throw new GradleException(msg)
  }

  private boolean isEmpty(String header) {
    return (header == null) || header.trim().isEmpty() || Constants.EMPTY_HEADER.equals(header)
  }
}
