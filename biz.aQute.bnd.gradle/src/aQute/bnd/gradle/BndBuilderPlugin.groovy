/**
 * BndBuilderPlugin for Gradle.
 *
 * <p>
 * The plugin name is {@code biz.aQute.bnd.builder}.
 *
 * <p>
 * This plugin applies the java plugin to a project and modifies the jar
 * task by adding the properties from the {@link BundleTaskConvention},
 * setting the bndfile to 'bnd.bnd, and building the jar file as a bundle.
 * <p>
 * This plugin also defines a 'baseline' configuration and a baseline task
 * of type {@link Baseline}. The baseline task will be set up with the
 * default of baselining the output of the jar task using the baseline
 * configuration. If the baseline configuration is not otherwise
 * setup and the baseline task is configured to baseline a task, the
 * baseline configuration will be set as follows:
 *
 * <pre>
 * dependencies {
 *     baseline('group': project.group, 
 *              'name': baseline.bundleTask.baseName, 
 *              'version': "(,${baseline.bundleTask.version})") {
 *       transitive false
 *     }
 *   }
 * }
 * </pre>
 */

package aQute.bnd.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ResolveException


public class BndBuilderPlugin implements Plugin<Project> {
  public static final String PLUGINID = 'biz.aQute.bnd.builder'

  /**
   * Apply the {@code biz.aQute.bnd.builder} plugin to the specified project.
   */
  @Override
  public void apply(Project p) {
    p.configure(p) { project ->
      if (plugins.hasPlugin(BndPlugin.PLUGINID)) {
          throw new GradleException("Project already has '${BndPlugin.PLUGINID}' plugin applied.")
      }
      plugins.apply 'java'

      jar {
        description 'Assembles a bundle containing the main classes.'
        convention.plugins.bundle = new BundleTaskConvention(jar)
        bndfile = 'bnd.bnd'
        doLast {
          buildBundle()
        }
      }

      configurations {
        baseline
      }

      task ('baseline', type: Baseline) {
        description 'Baseline the project bundle.'
        group 'release'
        bundle jar
        baseline configurations.baseline
      }

      afterEvaluate {
        Task bundleTask = baseline.bundleTask
        Configuration baselineConfiguration = baseline.baselineConfiguration
        if (bundleTask && (baselineConfiguration == configurations.baseline) && baselineConfiguration.dependencies.empty) {
          Dependency baselineDep = dependencies.create('group': group, 'name': bundleTask.baseName, 'version': "(,${bundleTask.version})")
          boolean resolveError
          try {
            resolveError = configurations.detachedConfiguration(baselineDep).setTransitive(false).resolvedConfiguration.hasError()
          } catch(ResolveException e) {
            resolveError = true
          }
          if (resolveError) {
            dependencies {
              baseline files(baseline.bundle)
            }
          } else {
            dependencies {
              baseline(baselineDep) {
                transitive false
              }
            }
          }
        }
      }
    }
  }
}
