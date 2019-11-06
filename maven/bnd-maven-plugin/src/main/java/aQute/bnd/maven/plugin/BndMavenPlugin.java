package aQute.bnd.maven.plugin;

import java.io.File;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "bnd-process", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class BndMavenPlugin extends AbstractBndMavenPlugin {

	@Parameter(defaultValue = "${project.build.sourceDirectory}", readonly = true)
	private File									sourceDir;

	@Parameter(defaultValue = "${project.build.resources}", readonly = true)
	private List<org.apache.maven.model.Resource>	resources;

	@Parameter(defaultValue = "${project.build.outputDirectory}")
	private File									classesDir;

	@Parameter(defaultValue = "${project.build.outputDirectory}")
	private File									outputDir;

	@Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/MANIFEST.MF")
	File											manifestPath;

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
		return manifestPath;
	}

	@Override
	public boolean isSkip() {
		return skip;
	}

}
