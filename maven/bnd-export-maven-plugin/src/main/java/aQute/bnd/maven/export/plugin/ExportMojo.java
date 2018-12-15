package aQute.bnd.maven.export.plugin;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;

import java.io.File;
import java.io.OutputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map.Entry;
import java.util.Properties;
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

import aQute.bnd.build.Workspace;
import aQute.bnd.exporter.executable.ExecutableJarExporter;
import aQute.bnd.exporter.runbundles.RunbundlesExporter;
import aQute.bnd.maven.lib.configuration.BeanProperties;
import aQute.bnd.maven.lib.configuration.Bndruns;
import aQute.bnd.maven.lib.configuration.Bundles;
import aQute.bnd.maven.lib.resolve.DependencyResolver;
import aQute.bnd.maven.lib.resolve.Scope;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.io.IO;
import biz.aQute.resolve.Bndrun;
import biz.aQute.resolve.ResolveProcess;

@Mojo(name = "export", defaultPhase = PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ExportMojo extends AbstractMojo {
	private static final Logger			logger	= LoggerFactory.getLogger(ExportMojo.class);

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject				project;

	@Parameter(defaultValue = "${settings}", readonly = true)
	private Settings					settings;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
	private RepositorySystemSession		repositorySession;

	@Parameter
	private Bndruns						bndruns	= new Bndruns();

	@Parameter(defaultValue = "${project.build.directory}", readonly = true)
	private File						targetDir;

	@Parameter
	private Bundles						bundles	= new Bundles();

	@Parameter(defaultValue = "true")
	private boolean						useMavenDependencies;

	@Parameter(defaultValue = "false")
	private boolean						resolve;

	@Parameter(defaultValue = "true")
	private boolean						reportOptional;

	@Parameter(defaultValue = "true")
	private boolean						failOnChanges;

	@Parameter(defaultValue = "false")
	private boolean						bundlesOnly;

	@Parameter
	private String						exporter;

	@Parameter(defaultValue = "true")
	private boolean						attach;

	@Parameter(defaultValue = "${session}", readonly = true)
	private MavenSession				session;

	@Parameter
	private Set<Scope>					scopes	= EnumSet.of(Scope.compile, Scope.runtime);

	@Parameter(property = "bnd.export.skip", defaultValue = "false")
	private boolean						skip;

	private int							errors	= 0;

	@Component
	private RepositorySystem			system;

	@Component
	private ProjectDependenciesResolver	resolver;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			logger.debug("skip project as configured");
			return;
		}

		try {
			DependencyResolver dependencyResolver = new DependencyResolver(project, repositorySession, resolver,
				system, scopes);

			FileSetRepository fileSetRepository = dependencyResolver.getFileSetRepository(project.getName(),
				bundles.getFiles(project.getBasedir()),
				useMavenDependencies);

			if (exporter == null) {
				exporter = bundlesOnly ? RunbundlesExporter.RUNBUNDLES : ExecutableJarExporter.EXECUTABLE_JAR;
			}

			Properties beanProperties = new BeanProperties();
			beanProperties.put("project", project);
			beanProperties.put("settings", settings);
			Properties mavenProperties = new Properties(beanProperties);
			mavenProperties.putAll(project.getProperties());
			Processor processor = new Processor(mavenProperties, false);

			for (File runFile : bndruns.getFiles(project.getBasedir(), "*.bndrun")) {
				export(runFile, fileSetRepository, processor);
			}
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}

		if (errors > 0)
			throw new MojoFailureException(errors + " errors found");
	}

	private void export(File runFile, FileSetRepository fileSetRepository, Processor processor) throws Exception {
		if (!runFile.exists()) {
			logger.error("Could not find bnd run file {}", runFile);
			errors++;
			return;
		}
		String bndrun = getNamePart(runFile);
		File temporaryDir = new File(targetDir, "tmp/export/" + bndrun);
		File cnf = new File(temporaryDir, Workspace.CNFDIR);
		IO.mkdirs(cnf);
		try (Bndrun run = Bndrun.createBndrun(null, runFile)) {
			run.setBase(temporaryDir);
			Workspace workspace = run.getWorkspace();
			workspace.setParent(processor);
			workspace.setBuildDir(cnf);
			workspace.setOffline(session.getSettings()
				.isOffline());
			workspace.addBasicPlugin(fileSetRepository);
			for (RepositoryPlugin repo : workspace.getRepositories()) {
				repo.list(null);
			}
			run.getInfo(workspace);
			report(run);
			if (!run.isOk()) {
				return;
			}
			if (resolve) {
				try {
					String runBundles = run.resolve(failOnChanges, false);
					if (!run.isOk()) {
						return;
					}
					run.setProperty(Constants.RUNBUNDLES, runBundles);
				} catch (ResolutionException re) {
					logger.error(ResolveProcess.format(re, reportOptional));
					throw re;
				} finally {
					report(run);
				}
			}
			try {
				Entry<String, Resource> export = run.export(exporter, Collections.emptyMap());
				if (export != null) {
					if (exporter.equals(RunbundlesExporter.RUNBUNDLES)) {
						try (JarResource r = (JarResource) export.getValue()) {
							File runbundlesDir = new File(targetDir, "export/" + bndrun);
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
			} finally {
				report(run);
			}
		}
	}

	private String getNamePart(File runFile) {
		String nameExt = runFile.getName();
		int pos = nameExt.lastIndexOf('.');
		return (pos > 0) ? nameExt.substring(0, pos) : nameExt;
	}

	private void report(Bndrun run) {
		for (String warning : run.getWarnings()) {
			logger.warn("Warning : {}", warning);
		}
		for (String error : run.getErrors()) {
			logger.error("Error   : {}", error);
			errors++;
		}
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
