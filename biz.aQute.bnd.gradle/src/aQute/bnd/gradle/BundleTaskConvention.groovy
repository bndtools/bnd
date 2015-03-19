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

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.SourceSet

class BundleTaskConvention {
  private File bndfile
  private Configuration configuration
  private SourceSet sourceSet

  BundleTaskConvention(Task task) {
  /**
   * Create a BundleTaskConvention for the specified Jar task.
   *
   * <p>
   * This also sets the default values for the added properties
   * and adds the bnd file to the task inputs.
   */
    def Project project = task.project
    // Set default property values
    setBndfile(project.file('bnd.bnd'))
    setConfiguration(project.configurations.compile)
    setSourceSet(project.sourceSets.main)
    // Add bndfile to task inputs
    task.inputs.file {
      getBndfile()
    }
  }

  File getBndfile() {
  /**
   * Get the bndfile property.
   */
    return bndfile
  }
  void setBndfile(File bndfile) {
  /**
   * Set the bndfile property.
   */
    this.bndfile = bndfile
  }

  Configuration getConfiguration() {
  /**
   * Get the configuration property.
   */
    return configuration
  }
  void setConfiguration(Configuration configuration) {
  /**
   * Set the configuration property.
   */
    this.configuration = configuration
  }

  SourceSet getSourceSet() {
  /**
   * Get the sourceSet property.
   */
    return sourceSet
  }
  void setSourceSet(SourceSet sourceSet) {
  /**
   * Set the sourceSet property.
   */
    this.sourceSet = sourceSet
  }
}
