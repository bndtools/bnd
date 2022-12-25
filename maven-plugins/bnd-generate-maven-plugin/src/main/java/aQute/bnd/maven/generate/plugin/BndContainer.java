package aQute.bnd.maven.generate.plugin;

import static aQute.bnd.exceptions.FunctionWithException.asFunctionOrElse;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import aQute.bnd.annotation.ProviderType;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.maven.lib.configuration.BeanProperties;
import aQute.bnd.maven.lib.configuration.BndConfiguration;
import aQute.bnd.maven.lib.resolve.ImplicitFileSetRepository;
import aQute.bnd.maven.lib.resolve.LocalPostProcessor;
import aQute.bnd.maven.lib.resolve.PostProcessor;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;
import aQute.lib.strings.Strings;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.maven.api.Revision;
import aQute.maven.provider.POM;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
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

	private static final Logger				logger	= LoggerFactory.getLogger(BndContainer.class);

	private final List<File>				bundles	= new ArrayList<>();

	private final MavenProject				project;

	private final RepositorySystemSession	repositorySession;

	private final MavenSession				session;

	private final RepositorySystem			system;

	private final PostProcessor				postProcessor;

	private List<Dependency>				dependencies;

	private Properties						additionProperties;

	private ArtifactHandlerManager			artifactHandlerManager;

	public static class Builder {

		private final MavenProject				project;
		private final MavenSession				session;
		private final RepositorySystemSession	repositorySession;
		private final RepositorySystem			system;
		private PostProcessor					postProcessor		= new LocalPostProcessor();
		private List<Dependency>				dependencies		= new ArrayList<>();
		private Properties						additionaProperties	= new Properties();
		private ArtifactHandlerManager			artifactHandlerManager;

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
			return new BndContainer(project, session, repositorySession, system, dependencies, postProcessor,
				additionaProperties, artifactHandlerManager);
		}

		public Builder setArtifactHandlerManager(ArtifactHandlerManager artifactHandlerManager) {
			this.artifactHandlerManager = artifactHandlerManager;
			return this;
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
		Properties additionProperties, ArtifactHandlerManager artifactHandlerManager) {
		this.project = project;
		this.session = session;
		this.repositorySession = repositorySession;
		this.system = system;
		this.dependencies = dependencies;
		this.postProcessor = postProcessor;
		this.additionProperties = additionProperties;
		this.artifactHandlerManager = artifactHandlerManager;
	}

	public int generate(String task, File workingDir, GenerateOperation operation, Settings settings,
		MojoExecution mojoExecution, boolean includeTestDependencies) throws Exception {
		Properties beanProperties = new BeanProperties();
		beanProperties.put("project", project);
		beanProperties.put("settings", settings);
		Properties mavenProperties = new Properties(beanProperties);
		Properties projectProperties = project.getProperties();
		mavenProperties.putAll(projectProperties);
		List<Jar> closeables = new ArrayList<>();
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

			handleDependencies(bnd, closeables, includeTestDependencies);

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

			return operation.apply("generate", bnd);
		} finally {
			closeables.forEach(Jar::close);
		}
	}

	private void handleDependencies(Project bnd, List<Jar> closeables, boolean includeTestDependencies)
		throws Exception {
		// Compute bnd build and testpath, which might be used by a
		// generator
		Set<org.apache.maven.artifact.Artifact> artifacts = project.getArtifacts();
		List<Object> buildpath = new ArrayList<>(artifacts.size());
		List<Object> testpath = new ArrayList<>(artifacts.size());
		final ScopeArtifactFilter scopeFilter = new ScopeArtifactFilter(
			org.apache.maven.artifact.Artifact.SCOPE_COMPILE);
		for (org.apache.maven.artifact.Artifact artifact : artifacts) {
			File cpe = artifact.getFile()
				.getCanonicalFile();
			if (!cpe.exists()) {
				logger.debug("dependency {} does not exist", cpe);
				continue;
			}
			if (cpe.isDirectory()) {
				Jar cpeJar = new Jar(cpe);
				cpeJar.setSource(new File(cpe.getParentFile(), createArtifactName(artifact)));
				closeables.add(cpeJar);
				if (scopeFilter.include(artifact)) {
					buildpath.add(createBuildPathExpression(cpe));
				} else if (includeTestDependencies) {
					testpath.add(createBuildPathExpression(cpe));
				}
			} else {
				if (!cpe.getName()
					.endsWith(".jar")) {
					/*
					 * Check if it is a valid zip file. We don't create a Jar
					 * object here because we want to avoid the cost of creating
					 * the Jar object if we decide not to build.
					 */
					try (ZipFile zip = new ZipFile(cpe)) {
						zip.entries();
					} catch (ZipException e) {
						logger.debug("dependency {} is not a zip", cpe);
						continue;
					}
				}

				if (scopeFilter.include(artifact)) {
					buildpath.add(createBuildPathExpression(cpe));
				} else if (includeTestDependencies) {
					testpath.add(createBuildPathExpression(cpe));
				}
			}
		}

		bnd.setProperty("-buildpath.bndmavengenerate", Strings.join(",", buildpath));
		if (includeTestDependencies) {
			bnd.setProperty("-testpath.bndmavengenerate", Strings.join(",", testpath));
		}

		injectImplicitRepository(bnd.getWorkspace());
	}

	private String createBuildPathExpression(File file) {
		try (Jar jar = new Jar(file)) {
			Optional<Revision> revision = jar.getPomXmlResources()
				.findFirst()
				.map(asFunctionOrElse(pomResource -> new POM(null, pomResource.openInputStream(), true), null))
				.map(POM::getRevision);

			String name = jar.getBsn();
			if (name == null) {
				name = revision.map(r -> r.program.toString())
					.orElse(null);
				if (name == null) {
					return null;
				}
			}

			Version version = revision.map(r -> r.version.getOSGiVersion())
				.orElse(null);
			if (version == null) {
				try {
					version = new MavenVersion(jar.getModuleVersion()).getOSGiVersion();
				} catch (Exception e) {
					// no Version
				}
			}
			// return name + (version != null ? ";version=" +
			// version.toStringWithoutQualifier() : "");
			return name + (version != null ? ";version=" + version.getWithoutQualifier() : "");
		} catch (Exception e) {
			return file + ";version=file";
		}
	}

	private String getExtension(String type) {
		ArtifactHandler artifactHandler = artifactHandlerManager.getArtifactHandler(type);
		if (artifactHandler != null) {
			return artifactHandler.getExtension();
		}
		return type;
	}

	private String createArtifactName(org.apache.maven.artifact.Artifact artifact) {
		String classifier = artifact.getClassifier();
		if ((classifier == null) || classifier.isEmpty()) {
			return String.format("%s-%s.%s", artifact.getArtifactId(), artifact.getVersion(),
				getExtension(artifact.getType()));
		}
		return String.format("%s-%s-%s.%s", artifact.getArtifactId(), artifact.getVersion(), classifier,
			getExtension(artifact.getType()));
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
		for (org.apache.maven.artifact.Artifact dep : project.getArtifacts()) {

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

	private Artifact transform(org.apache.maven.artifact.Artifact dependency) {
		Artifact artifact = new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(),
			dependency.getType(), dependency.getVersion());
		return artifact;
	}
}
