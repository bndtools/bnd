package aQute.bnd.maven.plugin;

import java.util.Optional;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "test-jar", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class BndMavenPackagingTestsPlugin extends BndMavenTestsPlugin {

	@Parameter(defaultValue = "tests")
	private String classifier;

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

}
