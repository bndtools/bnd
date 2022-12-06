package aQute.bnd.maven.generate.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import aQute.bnd.annotation.ProviderType;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.maven.lib.configuration.BeanProperties;
import aQute.bnd.maven.lib.configuration.BndConfiguration;
import aQute.bnd.maven.lib.resolve.ImplicitFileSetRepository;
import aQute.bnd.maven.lib.resolve.LocalPostProcessor;
import aQute.bnd.maven.lib.resolve.PostProcessor;
import aQute.bnd.osgi.Processor;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.utf8properties.UTF8Properties;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ProviderType
public class BndContainer {

	private static final Logger										logger	= LoggerFactory
		.getLogger(BndContainer.class);

	private final List<File>										bundles	= new ArrayList<>();

	private final MavenProject										project;

	private final RepositorySystemSession							repositorySession;

	private final MavenSession										session;

	private final RepositorySystem									system;

	private final PostProcessor										postProcessor;

	private List<Dependency>										dependencies;

	private Properties						additionProperties;

	public static class Builder {

		private final MavenProject										project;
		private final MavenSession										session;
		private final RepositorySystemSession							repositorySession;
		private final RepositorySystem									system;
		private PostProcessor											postProcessor				= new LocalPostProcessor();
		private List<Dependency>										dependencies	= new ArrayList<Dependency>();
		private Properties						additionaProperties	= new Properties();

		@SuppressWarnings("deprecation")
		public Builder(MavenProject project, MavenSession session, RepositorySystemSession repositorySession,
			RepositorySystem system) {

			this.project = Objects.requireNonNull(project);
			this.session = Objects.requireNonNull(session);
			this.repositorySession = Objects.requireNonNull(repositorySession);
			this.system = Objects.requireNonNull(system);
		}

		public Builder setPostProcessor(PostProcessor postProcessor) {
			this.postProcessor = postProcessor;
			return this;
		}

		public Builder setDependencies(List<Dependency> dependencies) {
			this.dependencies.addAll(dependencies);
			return this;
		}

		public Builder setAdditionalProperiets(Properties properties) {
			this.additionaProperties.putAll(properties);
			return this;
		}

		public BndContainer build() {
			return new BndContainer(project, session, repositorySession, system,
				dependencies, postProcessor, additionaProperties);
		}

	}

	public static int report(Processor project) {
		int errors = 0;
		for (String warning : project.getWarnings()) {
			logger.warn("Warning : {}", warning);
		}
		for (String error : project.getErrors()) {
			logger.error("Error   : {}", error);
			errors++;
		}
		return errors;
	}

	@SuppressWarnings("deprecation")
	BndContainer(MavenProject project, MavenSession session, RepositorySystemSession repositorySession,
		RepositorySystem system, List<Dependency> dependencies, PostProcessor postProcessor,
		Properties additionProperties) {
		this.project = project;
		this.session = session;
		this.repositorySession = repositorySession;
		this.system = system;
		this.dependencies = dependencies;
		this.postProcessor = postProcessor;
		this.additionProperties = additionProperties;
	}

	public int generate(String task, File workingDir, GenerateOperation operation, Settings settings,
		MojoExecution mojoExecution) throws Exception {
		Properties beanProperties = new BeanProperties();
		beanProperties.put("project", project);
		beanProperties.put("settings", settings);
		Properties mavenProperties = new Properties(beanProperties);
		Properties projectProperties = project.getProperties();
		mavenProperties.putAll(projectProperties);
		// try (Project bnd = init(task, workingDir, mavenProperties)) {
		try (Project bnd = init(task, workingDir, session.getCurrentProject()
			.getBasedir(), mavenProperties)) {
			if (bnd == null) {
				return 1;
			}

			bnd.setTrace(logger.isDebugEnabled());

			bnd.setBase(project.getBasedir());
			// load the bnd file or config element
			File propertiesFile = new BndConfiguration(project, mojoExecution).loadProperties(bnd);

			// handle our addition properties and trigger the replacement of
			// ${.}
			UTF8Properties properties = new UTF8Properties();
			properties.putAll(additionProperties);
			bnd.setProperties(properties.replaceHere(project.getBasedir()));

			bnd.setProperty("project.output", workingDir.getCanonicalPath());
			bnd.setProperty(aQute.bnd.osgi.Constants.DEFAULT_PROP_TARGET_DIR, workingDir.getCanonicalPath());

			if (logger.isDebugEnabled()) {
				logger.debug("Generate Project Properties");
				bnd.getProperties()
					.forEach((k, v) -> logger.debug(k + " - " + v));
			}

			int errors = report(bnd);
			if (!bnd.isOk()) {
				return errors;
			}
			injectImplicitRepository(bnd.getWorkspace());
			return operation.apply("generate", bnd);
		}
	}

	public Project init(String task, File wsDir, File workingDir, Properties mavenProperties) throws Exception {
		File temporaryDir = wsDir.toPath()
			.resolve("tmp")
			.resolve(task)
			.toFile();
		File cnfDir = new File(temporaryDir, "cnf");
		cnfDir.mkdirs();
		File buildBnd = new File(cnfDir, "build.bnd");
		buildBnd.createNewFile();
		mavenProperties.store(new FileOutputStream(buildBnd), task);
		Workspace workspace = new Workspace(cnfDir.getParentFile());
		Project project = new Project(workspace, workingDir);
		workspace.setOffline(session.getSettings()
			.isOffline());
		project.forceRefresh(); // setBase must be called after forceRefresh
		project.getInfo(workspace);

		return project;
	}

	public boolean injectImplicitRepository(Workspace workspace) throws Exception {
		if (workspace.getPlugin(ImplicitFileSetRepository.class) == null) {
			workspace.addBasicPlugin(getFileSetRepository());
			for (RepositoryPlugin repo : workspace.getRepositories()) {
				repo.list(null);
			}
			return true;
		}
		return false;
	}


	/**
	 * Creates a new repository in every invocation.
	 *
	 * @return a new {@link ImplicitFileSetRepository}
	 * @throws Exception
	 */
	public FileSetRepository getFileSetRepository() throws Exception {
		return getFileSetRepository(project);
	}

	private List<RemoteRepository> getProjectRemoteRepositories() {
		List<RemoteRepository> remoteRepositories = new ArrayList<>(project.getRemoteProjectRepositories());
		ArtifactRepository deployRepo = project.getDistributionManagementArtifactRepository();
		if (deployRepo != null) {
			remoteRepositories.add(RepositoryUtils.toRepo(deployRepo));
		}
		return remoteRepositories;
	}

	/**
	 * Creates a new repository in every invocation.
	 *
	 * @param project the Maven project
	 * @return a new {@link ImplicitFileSetRepository}
	 * @throws Exception
	 */
	public FileSetRepository getFileSetRepository(MavenProject project) throws Exception {

		List<RemoteRepository> repositories = getProjectRemoteRepositories();

		bundles.clear();

		for (Dependency dep : dependencies) {

			ArtifactResult artifactResult = postProcessor.postProcessResult(
				system.resolveArtifact(repositorySession, new ArtifactRequest(transform(dep), repositories, null)));

			bundles.add(artifactResult.getArtifact()
				.getFile());

		}
		return new ImplicitFileSetRepository("Generator-Dependencies", bundles);
	}

	private Artifact transform(Dependency dependency) {
		Artifact artifact = new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(),
			dependency.getType(), dependency.getVersion());
		return artifact;
	}
}
