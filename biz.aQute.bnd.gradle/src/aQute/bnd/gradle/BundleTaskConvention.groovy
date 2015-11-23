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
      def Builder builder = new Builder()
      builder.setTrace(logger.isDebugEnabled())
      try {
        // load any task manifest entries into the builder
        builder.addProperties(manifest.effectiveManifest.attributes)
        // force includes to be processed
        builder.setProperties(project.projectDir, builder.getProperties())

        // if the bnd file exists, set it as the builder properties
        if (bndfile.isFile()) {
          builder.setProperties(bndfile, project.projectDir)
        } else {
          builder.setBase(project.projectDir)
        }

        // If no bundle to be built, we have nothing to do
        if (Builder.isTrue(builder.getProperty(Constants.NOBUNDLES))) {
          return
        }

        // Reject sub-bundle projects
        if ((builder.getSubBuilders().size() != 1) || ((builder.getPropertiesFile() != null) && !bndfile.equals(builder.getPropertiesFile()))) {
          throw new GradleException('Sub-bundles are not supported by this task')
        }

        // copy Jar task generated jar to temporaryDir
        project.copy {
          from archivePath
          into temporaryDir
        }
        def File temporaryFile = new File(temporaryDir, archiveName)

        // set builder classpath
        builder.setClasspath(configuration.resolvedConfiguration.resolvedArtifacts.findAll{it.type == 'jar'}*.file as File[])
        logger.debug 'builder classpath: {}', builder.getClasspath()*.getSource()

        // set builder sourcepath if -sources=true
        if (builder.hasSources()) {
          builder.setSourcepath(sourceSet.java.srcDirs as File[])
          logger.debug 'builder sourcepath: {}', builder.getSourcePath()
        }

        // Include entire contents of Jar task generated jar except the manifest
        def Jar temporaryJar = new Jar(archiveName, temporaryFile)
        temporaryJar.setManifest(new Manifest())
        builder.setJar(temporaryJar)

        // set bundle symbolic name from tasks's baseName property if necessary
        def String bundleSymbolicName = builder.getProperty(Constants.BUNDLE_SYMBOLICNAME)
        if (isEmpty(bundleSymbolicName)) {
          builder.setProperty(Constants.BUNDLE_SYMBOLICNAME, baseName)
        }

        // set bundle version from task's version if necessary
        def String bundleVersion = builder.getProperty(Constants.BUNDLE_VERSION)
        if (isEmpty(bundleVersion)) {
          builder.setProperty(Constants.BUNDLE_VERSION, MavenVersion.parseString(version).getOSGiVersion().toString())
        }

        logger.debug 'builder properties: {}', builder.getProperties()

        // Build bundle
        def Jar bundleJar = builder.build()
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
        bundleJar.updateModified(temporaryFile.lastModified(), 'time of Jar task generated jar')
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
      } finally {
        builder.close()
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
