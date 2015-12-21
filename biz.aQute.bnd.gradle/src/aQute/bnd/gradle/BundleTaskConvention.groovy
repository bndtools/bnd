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
 * This defaults to 'bnd.bnd' in the projectDir. The bndfile does not need
 * to exist. It supersedes any information in the jar task's manifest.</li>
 * <li>configuration - This is the Configuration to use for the buildpath
 * for the bnd builder. It defaults to the 'compile' Configuration.</li>
 * <li>sourceSet - This is the SourceSet to use for the sourcepath for the
 * bnd builder. It defaults to the 'main' SourceSet.</li>
 * </ul>
 */

package aQute.bnd.gradle

import java.util.Properties
import java.util.jar.Manifest

import aQute.bnd.osgi.Builder
import aQute.bnd.osgi.Constants
import aQute.bnd.osgi.Jar
import aQute.bnd.version.MavenVersion
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.SourceSet

class BundleTaskConvention {
  private final org.gradle.api.tasks.bundling.Jar task
  private File bndfile
  private Configuration configuration
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
    task.inputs.file { // Add bndfile to task inputs
      getBndfile()
    }
  }

  /**
   * Get the bndfile property.
   */
  public File getBndfile() {
    if (bndfile == null) {
      setBndfile(task.project.file('bnd.bnd'))
    }
    return bndfile
  }
  /**
   * Set the bndfile property.
   */
  public void setBndfile(File bndfile) {
    this.bndfile = bndfile
  }

  /**
   * Get the configuration property.
   */
  public Configuration getConfiguration() {
    if (configuration == null) {
      setConfiguration(task.project.configurations.compile)
    }
    return configuration
  }
  /**
   * Set the configuration property.
   */
  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration
  }

  /**
   * Get the sourceSet property.
   */
  public SourceSet getSourceSet() {
    if (sourceSet == null) {
      setSourceSet(task.project.sourceSets.main)
    }
    return sourceSet
  }
  /**
   * Set the sourceSet property.
   */
  public void setSourceSet(SourceSet sourceSet) {
    this.sourceSet = sourceSet
  }

  void buildBundle() {
    task.configure {
      // create Builder and set trace level from gradle
      new Builder().withCloseable { builder ->
        builder.setTrace(logger.isDebugEnabled())
        // load bnd properties
        File temporaryBndFile = new File(temporaryDir, 'bnd.bnd')
        temporaryBndFile.withWriter('UTF-8') { writer ->
          // write any task manifest entries into the tmp bnd file
          manifest.effectiveManifest.attributes.inject(new Properties()) { properties, key, value ->
            if (key != 'Manifest-Version') {
              properties.setProperty(key, value.toString())
            }
            return properties
          }.store(writer, null)

          // if the bnd file exists, add its contents to the tmp bnd file
          if (bndfile.isFile()) {
            builder.loadProperties(bndfile).store(writer, null)
          }
        }
        builder.setProperties(temporaryBndFile, project.projectDir) // this will cause project.dir property to be set
        builder.setProperty('project.name', project.name)
        builder.setProperty('project.output', project.buildDir.canonicalPath)

        // If no bundle to be built, we have nothing to do
        if (Builder.isTrue(builder.getProperty(Constants.NOBUNDLES))) {
          return
        }

        // Reject sub-bundle projects
        if (builder.getSubBuilders() != [builder]) {
          throw new GradleException('Sub-bundles are not supported by this task')
        }

        // Include entire contents of Jar task generated jar (except the manifest)
        project.copy {
          from archivePath
          into temporaryDir
        }
        File archiveCopyFile = new File(temporaryDir, archiveName)
        Jar archiveCopyJar = new Jar(archiveName, archiveCopyFile)
        archiveCopyJar.setManifest(new Manifest())
        builder.setJar(archiveCopyJar)

        // set builder classpath
        def buildpath = project.files(configuration.resolvedConfiguration.resolvedArtifacts.findAll{it.type == 'jar'}*.file)
        builder.setProperty('project.buildpath', buildpath.asPath)
        builder.setClasspath(buildpath as File[])
        logger.debug 'builder classpath: {}', builder.getClasspath()*.getSource()

        // set builder sourcepath
        def sourcepath = project.files(sourceSet.allSource.srcDirs.findAll{it.exists()})
        builder.setProperty('project.sourcepath', sourcepath.asPath)
        builder.setSourcepath(sourcepath as File[])
        logger.debug 'builder sourcepath: {}', builder.getSourcePath()


        // set bundle symbolic name from tasks's baseName property if necessary
        String bundleSymbolicName = builder.getProperty(Constants.BUNDLE_SYMBOLICNAME)
        if (isEmpty(bundleSymbolicName)) {
          builder.setProperty(Constants.BUNDLE_SYMBOLICNAME, baseName)
        }

        // set bundle version from task's version if necessary
        String bundleVersion = builder.getProperty(Constants.BUNDLE_VERSION)
        if (isEmpty(bundleVersion)) {
          builder.setProperty(Constants.BUNDLE_VERSION, MavenVersion.parseString(version).getOSGiVersion().toString())
        }

        logger.debug 'builder properties: {}', builder.getProperties()

        // Build bundle
        Jar bundleJar = builder.build()
        if (!builder.isOk()) {
          // if we already have an error; fail now
          builder.getWarnings().each {
            logger.warn 'Warning: {}', it
          }
          builder.getErrors().each {
            logger.error 'Error  : {}', it
          }
          failBuild("Bundle ${archiveName} has errors")
        }

        // Write out the bundle
        bundleJar.updateModified(archiveCopyFile.lastModified(), 'time of Jar task generated jar')
        try {
          bundleJar.write(archivePath)
        } catch (Exception e) {
          failBuild("Bundle ${archiveName} failed to build: ${e.getMessage()}", e)
        } finally {
          bundleJar.close()
        }

        builder.getWarnings().each {
          logger.warn 'Warning: {}', it
        }
        builder.getErrors().each {
          logger.error 'Error  : {}', it
        }
        if (!builder.isOk()) {
          failBuild("Bundle ${archiveName} has errors")
        }
      }
    }
  }

  private void failBuild(String msg) {
    task.archivePath.delete()
    throw new GradleException(msg)
  }

  private void failBuild(String msg, Exception e) {
    task.archivePath.delete()
    throw new GradleException(msg, e)
  }

  private boolean isEmpty(String header) {
    return (header == null) || header.trim().isEmpty() || Constants.EMPTY_HEADER.equals(header)
  }
}
