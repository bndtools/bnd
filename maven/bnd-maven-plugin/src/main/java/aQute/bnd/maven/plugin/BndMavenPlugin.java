package aQute.bnd.maven.plugin;

import java.io.File;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Processes the target classes to generate OSGi metadata.
 * <p>
 * This goal has the default phase of "process-classes".
 */
@Mojo(name = "bnd-process", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class BndMavenPlugin extends AbstractBndMavenPlugin {

	@Parameter(defaultValue = "${project.build.sourceDirectory}", readonly = true)
	private File									sourceDir;

	@Parameter(defaultValue = "${project.build.resources}", readonly = true)
	private List<org.apache.maven.model.Resource>	resources;

	/**
	 * The directory where the {@code maven-compiler-plugin} places its output.
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}")
	private File									classesDir;

	/**
	 * The directory where this plugin will store its output.
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}")
	private File									outputDir;

	/**
	 * Specify the path to store the generated manifest file.
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/MANIFEST.MF")
	File											manifestPath;

	/**
	 * if enabled and a release is given the output folder is adjusted to put
	 * the data into the appropriate versioned folder
	 */
	@Parameter
	boolean											multiReleaseOutput;

	/**
	 * Skip this goal.
	 */
	@Parameter(property = "bnd.skip", defaultValue = "false")
	boolean											skip;

	@Override
	public File getSourceDir() {
		return sourceDir;
	}

	@Override
	public List<Resource> getResources() {
		return resources;
	}

	@Override
	public File getClassesDir() {
		return classesDir;
	}

	@Override
	public File getOutputDir() {
		return outputDir;
	}

	@Override
	public File getManifestPath() {
		if (multiReleaseOutput && release > 0) {
			File parent = manifestPath.getParentFile();
			String name = manifestPath.getName();
			String versionedPath = String.format("versions/%d/%s", release, name);
			return new File(parent, versionedPath);
		}
		return manifestPath;
	}

	@Override
	public boolean isSkip() {
		return skip;
	}

}
