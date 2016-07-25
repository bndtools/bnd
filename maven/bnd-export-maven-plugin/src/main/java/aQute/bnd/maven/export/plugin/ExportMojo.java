package aQute.bnd.maven.export.plugin;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import aQute.bnd.build.Container;
import aQute.bnd.build.Run;
import aQute.bnd.build.StandaloneRun;
import aQute.bnd.osgi.Jar;
import biz.aQute.resolve.ProjectResolver;

@Mojo(name = "export", defaultPhase = PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ExportMojo extends AbstractMojo {

	@Parameter(readonly = true, required = true)
	private List<File>	bndruns;

	@Parameter(defaultValue = "${project.build.directory}", readonly = true)
	private File targetDir;

	@Parameter(readonly = true, required = false)
	private boolean		resolve			= false;

	private boolean		failOnChanges	= true;

	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			for (File runFile : bndruns) {
				if (!runFile.exists()) {
					throw new MojoExecutionException("Could not find bnd run file " + runFile);
				}
				try (StandaloneRun run = new StandaloneRun(runFile)) {
					if (resolve) {
						resolve(run);
					}
					export(run);
				}
			}
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private void export(Run run) throws Exception {
		try (Jar jar = run.getProjectLauncher().executable()) {
			targetDir.mkdirs();
			File jarFile = new File(targetDir, getNamePart(run.getPropertiesFile()) + ".jar");
			jar.write(jarFile);
		}
	}

	private void resolve(StandaloneRun run) throws Exception, IOException {
		try (ProjectResolver projectResolver = new ProjectResolver(run)) {
			List<Container> runBundles = projectResolver.getRunBundles();
			Collection<Container> currentRunBundles = run.getRunbundles();
			if (!CollectionUtils.isEqualCollection(currentRunBundles, runBundles)) {
				if (failOnChanges) {
					throw new MojoExecutionException("The runbundles have changed. Failing the build");
				} else {
					getLog().warn("The runbundles have changed. Please check the results of this build.");
					run.setRunBundles(runBundles);
				}
			}
		}
	}

	private String getNamePart(File runFile) {
		String nameExt = runFile.getName();
		int pos = nameExt.lastIndexOf(".");
		return nameExt.substring(0, pos);
	}

}
