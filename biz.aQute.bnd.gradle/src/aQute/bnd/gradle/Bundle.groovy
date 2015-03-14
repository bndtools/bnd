/*
 * Bundle task type for Gradle.
 *
 * <p>
 * This task type extends the Jar task type and can be used 
 * for tasks that make bundles. The Bundle task type adds the
 * properties from the BundleTaskConvention.
 *
 * <p>
 * Here is an example of using the Bundle task type:
 * <pre>
 * import aQute.bnd.gradle.Bundle
 * task bundle(type: Bundle) {
 *   description 'Build my bundle'
 *   group 'build'
 *   from sourceSets.bundle.output
 *   bndfile = project.file('bundle.bnd')
 *   configuration = configurations.bundleCompile
 *   sourceSet = sourceSets.bundle
 * }
 * </pre>
 */

package aQute.bnd.gradle

import aQute.bnd.osgi.Builder
import aQute.bnd.osgi.Constants
import aQute.bnd.osgi.Jar
import aQute.bnd.version.Version
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.SourceSet

public class Bundle extends org.gradle.api.tasks.bundling.Jar {

  public Bundle() {
    super()
    convention.plugins.bundle = new BundleTaskConvention(this)
  }

  @TaskAction
  protected void copy() {
    super.copy()
    build(this)
  }

  public static void build(org.gradle.api.tasks.bundling.Jar task) {
    task.configure {
      // create Builder and set trace level from gradle
      def Builder builder = new Builder()
      builder.setTrace(logger.isEnabled(LogLevel.DEBUG))
      try {
        // load any task manifest entries into the builder
        builder.addProperties(manifest.effectiveManifest.attributes)

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
        def File jar = new File(temporaryDir, archivePath.name)

        // set builder classpath
        def Set<File> artifacts = new LinkedHashSet<File>(configuration.resolvedConfiguration.resolvedArtifacts*.file)
        artifacts.add(jar)
        builder.setClasspath(artifacts.toArray(new File[artifacts.size()]))
        logger.debug "builder classpath: ${builder.getClasspath()*.getSource()}"

        // set builder sourcepath
        def Set<File> srcDirs = new LinkedHashSet<File>(sourceSet.java.srcDirs)
        builder.setSourcepath(srcDirs.toArray(new File[srcDirs.size()]))
        logger.debug "builder sourcepath: ${builder.getSourcePath()}"

        // Include entire contents of Jar task generated jar except the manifest
        def String includes = builder.getProperty(Constants.INCLUDERESOURCE)
        if (isEmpty(includes)) {
          includes = "@${jar}!/!META-INF/MANIFEST.MF"
        } else {
          includes = "@${jar}!/!META-INF/MANIFEST.MF,${includes}"
        }
        builder.setProperty(Constants.INCLUDERESOURCE, includes)

        // set bundle symbolic name from tasks's baseName property if necessary
        def String bundleSymbolicName = builder.getProperty(Constants.BUNDLE_SYMBOLICNAME)
        if (isEmpty(bundleSymbolicName)) {
          builder.setProperty(Constants.BUNDLE_SYMBOLICNAME, baseName)
        }

        // set bundle version from task's version if necessary
        def String bundleVersion = builder.getProperty(Constants.BUNDLE_VERSION)
        if (isEmpty(bundleVersion)) {
          builder.setProperty(Constants.BUNDLE_VERSION, Version.parseVersion(version).toString())
        }

        logger.debug "builder properties: ${builder.getProperties()}"

        // Build bundle (in memory)
        def Jar bundleJar = builder.build()
        bundleJar.updateModified(jar.lastModified(), 'time of Jar task generated jar')

        // check for warnings and errors
        builder.getWarnings().each {
          logger.warn "Warning: ${it}"
        }
        builder.getErrors().each {
          logger.error "Error  : ${it}"
        }
        if (!builder.getErrors().isEmpty()) {
          throw new GradleException('Bundle build has errors')
        }

        try {
          bundleJar.write(archivePath)
        } finally {
          bundleJar.close()
        }
      } finally {
        builder.close()
      }
    }
  }

  private static boolean isEmpty(String header) {
    return (header == null) || header.trim().isEmpty() || Constants.EMPTY_HEADER.equals(header)
  }
}
