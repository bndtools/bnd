package aQute.bnd.maven.plugin;

import java.io.File;
import java.util.Optional;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "jar", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class BndMavenPackagingPlugin extends BndMavenPlugin {

	@Parameter
	private String classifier;

	@Parameter(defaultValue = "${project.build.directory}")
	private File	outputDir;

	@Parameter(defaultValue = "${project.build.directory}")
	private File					warOutputDir;

	@Override
	public File getOutputDir() {
		return outputDir;
	}

	@Override
	protected File getWarOutputDir() {
		return warOutputDir;
	}

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
