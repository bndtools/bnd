/**
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

import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar

public class Bundle extends Jar {
  /**
   * Create a Bundle task.
   *
   * <p>
   * Also adds the BundleTaskConvention to this task.
   */
  public Bundle() {
    super()
    convention.plugins.bundle = new BundleTaskConvention(this)
  }

  /**
   * Build the bundle.
   *
   * <p>
   * This method calls super.copy() and then uses the Bnd Builder
   * to transform the Jar task built jar into a bundle.
   */
  @TaskAction
  protected void copy() {
    super.copy()
    buildBundle()
  }
}
