/*
 * BundleTaskConvention for Gradle.
 *
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
    return bndfile
  }
  void setBndfile(File bndfile) {
    this.bndfile = bndfile
  }

  Configuration getConfiguration() {
    return configuration
  }
  void setConfiguration(Configuration configuration) {
    this.configuration = configuration
  }

  SourceSet getSourceSet() {
    return sourceSet
  }
  void setSourceSet(SourceSet sourceSet) {
    this.sourceSet = sourceSet
  }
}
