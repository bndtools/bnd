package aQute.bnd.maven.export.plugin;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.HeaderClause;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.build.model.conversions.CollectionFormatter;
import aQute.bnd.build.model.conversions.Converter;
import aQute.bnd.build.model.conversions.HeaderClauseFormatter;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.properties.Document;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.io.IO;
import biz.aQute.resolve.ProjectResolver;

@Mojo(name = "export", defaultPhase = PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ExportMojo extends AbstractMojo {
	private static final Logger	logger			= LoggerFactory.getLogger(ExportMojo.class);

	@Parameter(readonly = true, required = true)
	private List<File>	bndruns;

	@Parameter(defaultValue = "${project.build.directory}", readonly = true)
	private File		targetDir;

	@Parameter(defaultValue = "false")
	private boolean				resolve;

	@Parameter(defaultValue = "true")
	private boolean				failOnChanges;

	@Parameter(defaultValue = "${session}", readonly = true)
	private MavenSession session;

	private Converter<String,Collection< ? extends HeaderClause>>	headerClauseListFormatter	= new CollectionFormatter<HeaderClause>(
			",\\\n\t", new HeaderClauseFormatter(), null);

	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			for (File runFile : bndruns) {
				export(runFile);
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
			Workspace workspace = run.getWorkspace();
			workspace.setOffline(session.getSettings().isOffline());
			for (RepositoryPlugin repo : workspace.getRepositories()) {
				repo.list(null);
			}
			run.check();
			if (!run.isOk()) {
				throw new MojoExecutionException("Initializing the workspace failed " + run.getErrors());
			}
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
			Map<Resource,List<Wire>> resolution = projectResolver.resolve();
			Set<Resource> resources = resolution.keySet();
			List<VersionedClause> runBundles = new ArrayList<>();
			for (Resource resource : resources) {
				VersionedClause runBundle = ResourceUtils.toVersionClause(resource, "[===,==+)");
				if (!runBundles.contains(runBundle)) {
					runBundles.add(runBundle);
				}
			}
			Collections.sort(runBundles, new Comparator<VersionedClause>() {
				@Override
				public int compare(VersionedClause a, VersionedClause b) {
					int diff = a.getName().compareTo(b.getName());
					return (diff != 0) ? diff : a.getVersionRange().compareTo(b.getVersionRange());
				}
			});

			File runFile = run.getPropertiesFile();
			BndEditModel bem = new BndEditModel(run.getWorkspace());
			Document doc = new Document(IO.collect(runFile));
			bem.loadFrom(doc);

			List<VersionedClause> bemRunBundles = bem.getRunBundles() != null ? bem.getRunBundles()
					: new ArrayList<VersionedClause>();

			String originalRunbundlesString = headerClauseListFormatter.convert(bemRunBundles);
			logger.debug("Original -runbundles was:\n\t {}", originalRunbundlesString);
			String runbundlesString = headerClauseListFormatter.convert(runBundles);
			logger.debug("Resolved -runbundles is:\n\t {}", runbundlesString);

			List<VersionedClause> deltaAdd = new ArrayList<>(runBundles);
			deltaAdd.removeAll(bemRunBundles);
			List<VersionedClause> deltaRemove = new ArrayList<>(bemRunBundles);
			deltaRemove.removeAll(runBundles);
			boolean added = bemRunBundles.addAll(deltaAdd);
			boolean removed = bemRunBundles.removeAll(deltaRemove);
			if (added || removed) {
				if (failOnChanges && !bemRunBundles.isEmpty()) {
					String message = String.format(
							"The runbundles have changed. Failing the build!\nWas: {}\nIs: {}",
							originalRunbundlesString, runbundlesString);
					throw new MojoExecutionException(message);
				}
				bem.setRunBundles(bemRunBundles);
				logger.info("Writing changes to {}", runFile.getAbsolutePath());
				logger.info("{}:{}", Constants.RUNBUNDLES, bem.getDocumentChanges().get(Constants.RUNBUNDLES));
				bem.saveChangesTo(doc);
				IO.store(doc.get(), runFile);
			}
		}
	}

	private String getNamePart(File runFile) {
		String nameExt = runFile.getName();
		int pos = nameExt.lastIndexOf(".");
		return nameExt.substring(0, pos);
	}

}
