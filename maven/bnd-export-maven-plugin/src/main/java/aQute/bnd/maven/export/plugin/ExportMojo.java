package aQute.bnd.maven.export.plugin;

import static aQute.bnd.maven.lib.resolve.BndrunContainer.report;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;

import java.io.File;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.osgi.service.resolver.ResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.exporter.executable.ExecutableJarExporter;
import aQute.bnd.exporter.runbundles.RunbundlesExporter;
import aQute.bnd.maven.lib.configuration.Bndruns;
import aQute.bnd.maven.lib.configuration.Bundles;
import aQute.bnd.maven.lib.resolve.BndrunContainer;
import aQute.bnd.maven.lib.resolve.Operation;
import aQute.bnd.maven.lib.resolve.Scope;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Resource;
import aQute.lib.io.IO;
import biz.aQute.resolve.ResolveProcess;

@Mojo(name = "export", defaultPhase = PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class ExportMojo extends AbstractMojo {
	private static final Logger									logger	= LoggerFactory.getLogger(ExportMojo.class);

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject										project;

	@Parameter(defaultValue = "${settings}", readonly = true)
	private Settings											settings;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
	private RepositorySystemSession								repositorySession;

	@Parameter
	private Bndruns												bndruns	= new Bndruns();

	@Parameter(defaultValue = "${project.build.directory}", readonly = true)
	private File												targetDir;

	@Parameter
	private Bundles												bundles	= new Bundles();

	@Parameter(defaultValue = "true")
	private boolean												useMavenDependencies;

	@Parameter(defaultValue = "false")
	private boolean												resolve;

	@Parameter(defaultValue = "true")
	private boolean												reportOptional;

	@Parameter(defaultValue = "true")
	private boolean												failOnChanges;

	@Parameter(defaultValue = "false")
	private boolean												bundlesOnly;

	@Parameter
	private String												exporter;

	@Parameter(defaultValue = "true")
	private boolean												attach;

	@Parameter(defaultValue = "${session}", readonly = true)
	private MavenSession										session;

	@Parameter(property = "bnd.export.scopes", defaultValue = "compile,runtime")
	private Set<Scope>											scopes	= new HashSet<>(
		Arrays.asList(Scope.compile, Scope.runtime));

	@Parameter(property = "bnd.export.skip", defaultValue = "false")
	private boolean												skip;

	@Parameter(property = "bnd.export.include.dependency.management", defaultValue = "false")
	private boolean												includeDependencyManagement;

	@Component
	private RepositorySystem									system;

	@Component
	private ProjectDependenciesResolver							resolver;

	@Component
	@SuppressWarnings("deprecation")
	protected org.apache.maven.artifact.factory.ArtifactFactory	artifactFactory;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			logger.debug("skip project as configured");
			return;
		}

		int errors = 0;

		try {
			BndrunContainer container = new BndrunContainer.Builder(project, session, repositorySession, resolver,
				artifactFactory, system).setBundles(bundles.getFiles(project.getBasedir()))
					.setIncludeDependencyManagement(includeDependencyManagement)
					.setScopes(scopes)
					.setUseMavenDependencies(useMavenDependencies)
					.build();

			Operation operation = getOperation();

			for (File runFile : bndruns.getFiles(project.getBasedir(), "*.bndrun")) {
				errors += container.execute(runFile, "export", targetDir, operation);
			}
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}

		if (errors > 0)
			throw new MojoFailureException(errors + " errors found");
	}

	private Operation getOperation() {
		return (file, bndrun, run) -> {
			if (resolve) {
				try {
					String runBundles = run.resolve(failOnChanges, false);
					if (run.isOk()) {
						logger.info("{}: {}", Constants.RUNBUNDLES, runBundles);
						run.setProperty(Constants.RUNBUNDLES, runBundles);
					}
				} catch (ResolutionException re) {
					logger.error(ResolveProcess.format(re, reportOptional));
					throw re;
				} finally {
					int errors = report(run);
					if (errors > 0) {
						return errors;
					}
				}
			}
			if (exporter == null) {
				exporter = bundlesOnly ? RunbundlesExporter.RUNBUNDLES : ExecutableJarExporter.EXECUTABLE_JAR;
			}
			Entry<String, Resource> export = run.export(exporter, Collections.emptyMap());
			if (export != null) {
				if (exporter.equals(RunbundlesExporter.RUNBUNDLES)) {
					try (JarResource r = (JarResource) export.getValue()) {
						File runbundlesDir = targetDir.toPath()
							.resolve("export")
							.resolve(bndrun)
							.toFile();
						r.getJar()
							.writeFolder(runbundlesDir);
					}
				} else {
					try (Resource r = export.getValue()) {
						File exported = IO.getBasedFile(targetDir, export.getKey());
						try (OutputStream out = IO.outputStream(exported)) {
							r.write(out);
						}
						exported.setLastModified(r.lastModified());
						attach(exported, bndrun);
					}
				}
			}
			return 0;
		};
	}

	private void attach(File file, String classifier) {
		if (!attach) {
			logger
				.debug("The export plugin has been configured not to attach the generated application to the project.");
			return;
		} else if (!file.getName()
			.endsWith(Constants.DEFAULT_JAR_EXTENSION)) {
			logger.debug("The export plugin will not attach a non-jar output to the project.");
			return;
		}

		DefaultArtifactHandler handler = new DefaultArtifactHandler("jar");
		handler.setExtension("jar");
		DefaultArtifact artifact = new DefaultArtifact(project.getGroupId(), project.getArtifactId(),
			project.getVersion(), null, "jar", classifier, handler);
		artifact.setFile(file);
		project.addAttachedArtifact(artifact);
	}

}
