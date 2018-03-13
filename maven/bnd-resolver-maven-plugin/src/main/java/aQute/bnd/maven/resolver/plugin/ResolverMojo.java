package aQute.bnd.maven.resolver.plugin;

import java.io.File;
import java.util.List;

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
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.osgi.service.resolver.ResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Workspace;
import aQute.bnd.maven.lib.resolve.DependencyResolver;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.io.IO;
import biz.aQute.resolve.Bndrun;
import biz.aQute.resolve.ResolveProcess;

/**
 * Resolves the <code>-runbundles</code> for the given bndrun file.
 */
@Mojo(name = "resolve", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ResolverMojo extends AbstractMojo {
	private static final Logger			logger	= LoggerFactory.getLogger(ResolverMojo.class);

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject				project;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
	private RepositorySystemSession		repositorySession;

	@Parameter(readonly = true, required = true)
	private List<File>					bndruns;

	@Parameter(readonly = true, required = false)
	private List<File>					bundles;

	@Parameter(defaultValue = "true")
	private boolean						useMavenDependencies;

	@Parameter(defaultValue = "true")
	private boolean						failOnChanges;

	@Parameter(defaultValue = "${project.build.directory}", readonly = true)
	private File						targetDir;

	@Parameter(defaultValue = "${session}", readonly = true)
	private MavenSession				session;

	private int							errors	= 0;

	@Component
	private RepositorySystem			system;

	@Component
	private ProjectDependenciesResolver	resolver;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			DependencyResolver dependencyResolver = new DependencyResolver(project, repositorySession, resolver,
				system);

			FileSetRepository fileSetRepository = dependencyResolver.getFileSetRepository(project.getName(), bundles,
				useMavenDependencies);

			for (File runFile : bndruns) {
				resolve(runFile, fileSetRepository);
			}
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}

		if (errors > 0)
			throw new MojoExecutionException(errors + " errors found");
	}

	private void resolve(File runFile, FileSetRepository fileSetRepository) throws Exception {
		if (!runFile.exists()) {
			logger.error("Could not find bnd run file {}", runFile);
			errors++;
			return;
		}
		String bndrun = getNamePart(runFile);
		File temporaryDir = new File(targetDir, "tmp/resolve/" + bndrun);
		File cnf = new File(temporaryDir, Workspace.CNFDIR);
		IO.mkdirs(cnf);
		try (Bndrun run = Bndrun.createBndrun(null, runFile)) {
			run.setBase(temporaryDir);
			Workspace workspace = run.getWorkspace();
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
			try {
				run.resolve(failOnChanges, true);
			} catch (ResolutionException re) {
				logger.error("Unresolved requirements: {}", ResolveProcess.format(re.getUnresolvedRequirements()));
				throw re;
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

}
