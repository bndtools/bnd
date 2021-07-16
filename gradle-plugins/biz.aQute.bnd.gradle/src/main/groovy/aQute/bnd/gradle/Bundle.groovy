package aQute.bnd.gradle

import groovy.transform.CompileStatic
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar

/**
 * Bundle task type for Gradle.
 *
 * <p>
 * This task type extends the Jar task type and can be used 
 * for tasks that make bundles. The Bundle task type adds the
 * properties from the BundleTaskExtension.
 *
 * <p>
 * Here is an example of using the Bundle task type:
 * <pre>
 * import aQute.bnd.gradle.Bundle
 * tasks.register("bundle", Bundle) {
 *   description "Build my bundle"
 *   group "build"
 *   from sourceSets.bundle.output
 *   bundle {
 *     bndfile = project.file("bundle.bnd")
 *     sourceSet = sourceSets.bundle
 *     classpath = sourceSets.bundle.compileClasspath
 *   }
 * }
 * </pre>
 */
public class Bundle extends Jar {
	private final BundleTaskExtension extension
	/**
	 * Create a Bundle task.
	 *
	 * <p>
	 * Also adds the BundleTaskExtension to this task.
	 */
	public Bundle() {
		super()
		extension = getExtensions().create(BundleTaskExtension.NAME, BundleTaskExtension.class, this)
		getConvention().getPlugins().put(BundleTaskExtension.NAME, new BundleTaskConvention(extension, this))
	}

	/**
	 * Build the bundle.
	 *
	 * <p>
	 * This method calls super.copy() and then uses the Bnd Builder
	 * to transform the Jar task built jar into a bundle.
	 */
	@TaskAction
	@Override
	protected void copy() {
		supercopy()
		extension.buildBundle()
	}

	/**
	 * Private method to call super.copy().
	 *
	 * <p>
	 * We need to compile the call to super.copy() with @CompileStatic to
	 * avoid a Groovy 2.4 error where the super.copy() call instead results in
	 * calling this.copy() causing a StackOverflowError.
	 */
	@CompileStatic
	private void supercopy() {
		super.copy()
	}
}
