package aQute.bnd.maven.export.plugin;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import aQute.bnd.osgi.Jar;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import biz.aQute.resolve.ProjectResolver;

@Mojo(name = "export", defaultPhase = PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ExportMojo extends AbstractMojo {

	@Parameter(readonly = true, required = false)
	private List<File>	bndruns;

	@Parameter(readonly = true, required = false)
	private File		bndrun;

	@Parameter(defaultValue = "${project.build.directory}", readonly = true)
	private File		targetDir;

	@Parameter(readonly = true, required = false)
	private boolean		resolve			= false;

	@Parameter(readonly = true, required = false)
	private boolean		failOnChanges	= true;

	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			if (bndrun != null) {
				export(bndrun);
			} else {
				for (File runFile : bndruns) {
					export(runFile);
				}
			}
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private void export(File runFile) throws MojoExecutionException, Exception, IOException {
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
			Collections.sort(runBundles, new Comparator<Container>() {

				@Override
				public int compare(Container o1, Container o2) {
					return o1.getBundleSymbolicName().compareTo(o2.getBundleSymbolicName());
				}
			});

			if (!CollectionUtils.isEqualCollection(runBundles, currentRunBundles)) {
				printRunBundles(runBundles, System.out);
				if (failOnChanges) {
					throw new MojoExecutionException("The runbundles have changed. Failing the build");
				} else {
					getLog().warn("The runbundles have changed:");
					run.setRunBundles(runBundles);
				}
			}
		}
	}

	private void printRunBundles(List<Container> runBundles, Appendable ps) throws IOException {
		if (runBundles.isEmpty())
			return;

		ps.append("\n-runbundles: ");
		String del = "";

		for (Container c : runBundles) {
			Version version = Version.parseVersion(c.getVersion()).getWithoutQualifier();
			VersionRange versionRange = new VersionRange(true, version,
					new Version(version.getMajor(), version.getMinor(), version.getMicro() + 1), false);

			ps.append(del)
					.append("\\\n\t")
					.append(c.getBundleSymbolicName())
					.append("; version='")
					.append(versionRange.toString())
					.append("'");

			del = ",";
		}
		ps.append("\n\n");
	}

	private String getNamePart(File runFile) {
		String nameExt = runFile.getName();
		int pos = nameExt.lastIndexOf(".");
		return nameExt.substring(0, pos);
	}

}
