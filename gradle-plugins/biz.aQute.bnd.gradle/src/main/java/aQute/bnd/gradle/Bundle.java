package aQute.bnd.gradle;

import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

/**
 * Bundle task type for Gradle.
 * <p>
 * This task type extends the Jar task type and can be used for tasks that make
 * bundles. The Bundle task type adds the properties from the
 * BundleTaskExtension.
 * <p>
 * Here is an example of using the Bundle task type:
 *
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
@CacheableTask
public class Bundle extends Jar {
	/**
	 * Create a Bundle task.
	 * <p>
	 * Also adds the BundleTaskExtension to this task.
	 */
	@SuppressWarnings("deprecation")
	public Bundle() {
		super();
		setGroup(LifecycleBasePlugin.BUILD_GROUP);
		BundleTaskExtension extension = getExtensions().create(BundleTaskExtension.NAME, BundleTaskExtension.class,
			this);
		getConvention().getPlugins()
			.put(BundleTaskExtension.NAME, new BundleTaskConvention(extension));
		doLast("buildBundle", extension.buildAction());
	}
}
