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

import static aQute.bnd.gradle.BndUtils.logReport
import static aQute.bnd.gradle.BndUtils.builtBy

import java.util.Properties
import java.util.jar.Manifest
import java.util.regex.Matcher
import java.util.zip.ZipException
import java.util.zip.ZipFile

import aQute.bnd.osgi.Builder
import aQute.bnd.osgi.Constants
import aQute.bnd.osgi.Jar
import aQute.bnd.osgi.Processor
import aQute.bnd.version.MavenVersion
import aQute.lib.utf8properties.UTF8Properties
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.SourceSet

class BundleTaskConvention {
  private final Task task
  private final Project project
  private File bndfile
  private final StringBuilder instructions
  private final ConfigurableFileCollection classpathCollection
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
    this.project = task.project
    instructions = new StringBuilder()
    classpathCollection = project.files()
    setSourceSet(project.sourceSets.main)
    classpathModified = false
    // need to programmatically add to inputs since @InputFiles in a convention is not processed
    task.inputs.files(classpathCollection).withPropertyName('classpath')
    task.inputs.file({ getBndfile() }).optional().withPropertyName('bndfile')
    task.inputs.property('bnd', { getBnd() })
  }

  /**
   * Get the bndfile property.
   * <p>
   * File path to a bnd file containing bnd instructions for this project.
   */
  @InputFile
  @Optional
  public File getBndfile() {
    return bndfile
  }
  /**
   * Set the bndfile property.
   *
   * <p>
   * The argument will be handled using
   * Project.file().
   */
  public void setBndfile(Object file) {
    bndfile = project.file(file)
  }

  /**
   * Get the bnd property.
   * <p>
   * If the bndfile property points an existing file, this property is ignored.
   * Otherwise, the bnd instructions in this property will be used.
   */
  @Input
  @Optional
  public String getBnd() {
    return instructions.toString()
  }

  /**
   * Set the bnd property from a multi-line string.
   */
  public void setBnd(CharSequence line) {
     instructions.length = 0
     bnd(line)
  }

  /**
   * Add instuctions to the bnd property from a list of multi-line strings.
   */
  public void bnd(CharSequence... lines) {
    lines.each { line ->
      instructions.append(line).append('\n')
    }
  }

  /**
   * Set the bnd property from a map.
   */
  public void setBnd(Map<String, ?> map) {
     instructions.length = 0
     bnd(map)
  }

  /**
   * Add instuctions to the bnd property from a map.
   */
  public void bnd(Map<String, ?> map) {
    map.each { key, value ->
      instructions.append(key).append('=').append(value).append('\n')
    }
  }

  /**
   * Add files to the classpath.
   *
   * <p>
   * The arguments will be handled using
   * Project.files().
   */
  public ConfigurableFileCollection classpath(Object... paths) {
    classpathModified = true
    return builtBy(classpathCollection.from(paths), paths)
  }

  /**
   * Get the classpath property.
   */
  @InputFiles
  public ConfigurableFileCollection getClasspath() {
    return classpathCollection
  }
  /**
   * Set the classpath property.
   */
  public void setClasspath(Object path) {
    classpathCollection.from = []
    classpathCollection.builtBy = []
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
    if (!classpathModified) {
      setClasspath(sourceSet.compileClasspath)
      classpathModified = false
    }
  }

  void buildBundle() {
    task.configure {
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
          }.replaceHere(project.projectDir).store(writer, null)

          // if the bnd file exists, add its contents to the tmp bnd file
          if (bndfile?.isFile()) {
            builder.loadProperties(bndfile).store(writer, null)
          } else if (!bnd.empty) {
            UTF8Properties props = new UTF8Properties()
            props.load(bnd, project.buildFile, builder)
            props.replaceHere(project.projectDir).store(writer, null)
          }
        }
        builder.setProperties(temporaryBndFile, project.projectDir) // this will cause project.dir property to be set
        builder.setProperty('project.output', project.buildDir.canonicalPath)

        // If no bundle to be built, we have nothing to do
        if (builder.is(Constants.NOBUNDLES)) {
          return
        }

        // Reject sub-bundle projects
        if (builder.getSubBuilders() != [builder]) {
          throw new GradleException('Sub-bundles are not supported by this task')
        }

        // Gradle 5.1 deprecates Jar task properties
        File archivePath = task.hasProperty('archiveFile') ? task.archiveFile.get().asFile : task.archivePath
        String archiveName = task.hasProperty('archiveFileName') ? task.archiveFileName.get() : task.archiveName
        String version = task.hasProperty('archiveVersion') ? task.archiveVersion.get() : task.version

        // Include entire contents of Jar task generated jar (except the manifest)
        project.copy {
          from archivePath
          into temporaryDir
        }
        File archiveCopyFile = new File(temporaryDir, archiveName)
        Jar bundleJar = new Jar(archiveName, archiveCopyFile)
        String reproducible = builder.getProperty(Constants.REPRODUCIBLE)
        bundleJar.setReproducible((reproducible != null) ? Processor.isTrue(reproducible) : !task.preserveFileTimestamps)
        bundleJar.updateModified(archiveCopyFile.lastModified(), 'time of Jar task generated jar')
        bundleJar.setManifest(new Manifest())
        builder.setJar(bundleJar)

        // set builder classpath
        ConfigurableFileCollection buildpath = project.files(classpath.files.findAll { File file ->
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
        builder.setProperty('project.buildpath', buildpath.asPath)
        builder.setClasspath(buildpath.files as File[])
        logger.debug 'builder classpath: {}', builder.getClasspath()*.getSource()

        // set builder sourcepath
        ConfigurableFileCollection sourcepath = project.files(sourceSet.allSource.srcDirs.findAll{it.exists()})
        builder.setProperty('project.sourcepath', sourcepath.asPath)
        builder.setSourcepath(sourcepath.files as File[])
        logger.debug 'builder sourcepath: {}', builder.getSourcePath()


        // set bundle symbolic name from tasks's baseName property if necessary
        String bundleSymbolicName = builder.getProperty(Constants.BUNDLE_SYMBOLICNAME)
        if (isEmpty(bundleSymbolicName)) {
          builder.setProperty(Constants.BUNDLE_SYMBOLICNAME, baseName)
        }

        // set bundle version from task's version if necessary
        String bundleVersion = builder.getProperty(Constants.BUNDLE_VERSION)
        if (isEmpty(bundleVersion)) {
          builder.setProperty(Constants.BUNDLE_VERSION, MavenVersion.parseMavenString(version?.toString()).getOSGiVersion().toString())
        }

        logger.debug 'builder properties: {}', builder.getProperties()

        // Build bundle
        Jar builtJar = builder.build()
        if (!builder.isOk()) {
          // if we already have an error; fail now
          logReport(builder, logger)
          failBuild("Bundle ${archiveName} has errors", archivePath)
        }

        // Write out the bundle
        builtJar.write(archivePath)
        long now = System.currentTimeMillis()
        archivePath.setLastModified(now)

        logReport(builder, logger)
        if (!builder.isOk()) {
          failBuild("Bundle ${archiveName} has errors", archivePath)
        }
      }
    }
  }

  private void failBuild(String msg, File archivePath) {
    project.delete(archivePath)
    throw new GradleException(msg)
  }

  private boolean isEmpty(String header) {
    return (header == null) || header.trim().isEmpty() || Constants.EMPTY_HEADER.equals(header)
  }
}
