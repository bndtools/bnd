package aQute.bnd.maven.plugin;

import java.util.Optional;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Generate OSGi metadata for the target classes and
 * package into a jar.
 * <p>
 * This goal has the default phase of "package".
 */
@Mojo(name = "jar", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class BndMavenPackagingPlugin extends BndMavenPlugin {

	/**
	 * The classifier to use for the generated artifact.
	 */
	@Parameter
	private String classifier;

	@Override
	public Optional<String> getClassifier() {
		return Optional.ofNullable(classifier)
			.map(String::trim)
			.filter(s -> !s.isEmpty());
	}

	@Override
	public Optional<String> getType() {
		return Optional.of("jar");
	}

}
