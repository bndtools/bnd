package aQute.bnd.maven.plugin;

import java.io.File;
import java.util.Optional;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Generate OSGi metadata for the test target classes and
 * package into a jar.
 * <p>
 * This goal has the default phase of "package".
 */
@Mojo(name = "test-jar", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class BndMavenPackagingTestsPlugin extends BndMavenTestsPlugin {

	/**
	 * The classifier to use for the generated artifact.
	 */
	@Parameter(defaultValue = "tests")
	String classifier;

	/**
	 * The directory where this plugin will store the generated artifact.
	 */
	@Parameter(defaultValue = "${project.build.directory}")
	File outputDir;

	@Override
	public Optional<String> getClassifier() {
		return Optional.ofNullable(classifier)
			.map(String::trim)
			.filter(s -> !s.isEmpty());
	}

	@Override
	public Optional<String> getType() {
		return Optional.of("test-jar");
	}

	@Override
	public File getOutputDir() {
		return outputDir;
	}
}
