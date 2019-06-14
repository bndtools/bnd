package aQute.bnd.maven.indexer.plugin;

import static aQute.bnd.maven.lib.resolve.LocalURLs.ALLOWED;
import static aQute.bnd.maven.lib.resolve.LocalURLs.REQUIRED;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.repository.metadata.io.MetadataReader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.resolution.ArtifactResult;
import org.osgi.service.repository.ContentNamespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.maven.lib.resolve.DependencyResolver;
import aQute.bnd.maven.lib.resolve.LocalURLs;
import aQute.bnd.maven.lib.resolve.RemotePostProcessor;
import aQute.bnd.maven.lib.resolve.Scope;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.resource.CapabilityBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.lib.io.IO;
import aQute.libg.cryptography.SHA256;

/**
 * Exports project dependencies to OSGi R5 index format.
 */
@Mojo(name = "index", defaultPhase = PACKAGE, requiresDependencyResolution = TEST, threadSafe = true)
public class IndexerMojo extends AbstractMojo {
	private static final Logger			logger	= LoggerFactory.getLogger(IndexerMojo.class);

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject				project;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
	private RepositorySystemSession		session;

	@Parameter(property = "bnd.indexer.output.file", defaultValue = "${project.build.directory}/index.xml")
	private File						outputFile;

	@Parameter(property = "bnd.indexer.localURLs", defaultValue = "FORBIDDEN")
	private LocalURLs					localURLs;

	@Parameter(property = "bnd.indexer.includeTransitive", defaultValue = "true")
	private boolean						includeTransitive;

	@Parameter(property = "bnd.indexer.includeJar", defaultValue = "false")
	private boolean						includeJar;

	@Parameter(property = "bnd.indexer.add.mvn.urls", defaultValue = "false")
	private boolean						addMvnURLs;

	@Parameter(property = "bnd.indexer.scopes", defaultValue = "compile,runtime")
	private Set<Scope>					scopes;

	@Parameter(property = "bnd.indexer.include.gzip", defaultValue = "true")
	private boolean						includeGzip;

	@Parameter(property = "bnd.indexer.skip", defaultValue = "false")
	private boolean						skip;

	@Parameter(property = "bnd.indexer.attach", defaultValue = "true")
	private boolean						attach;

	/**
	 * This configuration parameter is used by the maven-deploy-plugin to define
	 * a release repo for deployment
	 */
	@Parameter(property = "altReleaseDeploymentRepository")
	private String						altReleaseDeploymentRepository;

	/**
	 * This configuration parameter is used by the maven-deploy-plugin to define
	 * a snapshot repo for deployment
	 */
	@Parameter(property = "altSnapshotDeploymentRepository")
	private String						altSnapshotDeploymentRepository;

	/**
	 * This configuration parameter is used to set the name of the repository in
	 * the generated index
	 */
	@Parameter(property = "bnd.indexer.name", defaultValue = "${project.artifactId}")
	private String						indexName;

	/**
	 * This configuration parameter is the old mechanism used by the
	 * maven-deploy-plugin to define a release repo for deployment
	 */
	@Parameter(property = "altDeploymentRepository")
	private String						altDeploymentRepository;

	@Component
	private RepositorySystem			system;

	@Component
	private ProjectDependenciesResolver	resolver;

	@Component
	private MetadataReader				metadataReader;

	@Component
	private MavenProjectHelper			projectHelper;

	private boolean						fail;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if (skip) {
			logger.debug("skip project as configured");
			return;
		}

		logger.debug("Indexing dependencies with scopes: {}", scopes);
		logger.debug("Including Transitive dependencies: {}", includeTransitive);
		logger.debug("Local file URLs permitted: {}", localURLs);
		logger.debug("Adding mvn: URLs as alternative content: {}", addMvnURLs);

		Map<String, ArtifactRepository> repositories = new LinkedHashMap<>();
		for (ArtifactRepository artifactRepository : project.getRemoteArtifactRepositories()) {
			logger.debug("Located an artifact repository {}", artifactRepository.getId());
			repositories.put(artifactRepository.getId(), artifactRepository);
		}

		addDeploymentRepo(repositories, project.getDistributionManagementArtifactRepository());

		addDeploymentRepo(repositories, parseAltDistRepo(altReleaseDeploymentRepository, true, false));
		addDeploymentRepo(repositories, parseAltDistRepo(altSnapshotDeploymentRepository, false, true));
		addDeploymentRepo(repositories, parseAltDistRepo(altDeploymentRepository, true, true));

		DependencyResolver dependencyResolver = new DependencyResolver(project, session, resolver, system, scopes,
			includeTransitive, new RemotePostProcessor(session, system, metadataReader, localURLs));

		Map<File, ArtifactResult> dependencies = dependencyResolver.resolveAgainstRepos(repositories.values());

		RepositoryURLResolver repositoryURLResolver = new RepositoryURLResolver(repositories);
		MavenURLResolver mavenURLResolver = new MavenURLResolver();

		ResourcesRepository resourcesRepository = new ResourcesRepository();
		XMLResourceGenerator xmlResourceGenerator = new XMLResourceGenerator();

		logger.debug("Indexing artifacts: {}", dependencies.keySet());
		try {
			IO.mkdirs(outputFile.getParentFile());
			for (Entry<File, ArtifactResult> entry : dependencies.entrySet()) {
				File file = entry.getKey();
				ResourceBuilder resourceBuilder = new ResourceBuilder();
				resourceBuilder.addFile(entry.getKey(), repositoryURLResolver.resolver(file, entry.getValue()));

				if (addMvnURLs) {
					CapabilityBuilder c = new CapabilityBuilder(ContentNamespace.CONTENT_NAMESPACE);
					c.addAttribute(ContentNamespace.CONTENT_NAMESPACE, SHA256.digest(file)
						.asHex());
					c.addAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE,
						mavenURLResolver.resolver(file, entry.getValue()));
					c.addAttribute(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE, file.length());
					c.addAttribute(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE, MavenURLResolver.MIME);
					resourceBuilder.addCapability(c);
				}
				resourcesRepository.add(resourceBuilder.build());
			}
			if (includeJar && project.getPackaging()
				.equals("jar")) {
				File current = new File(project.getBuild()
					.getDirectory(),
					project.getBuild()
						.getFinalName() + ".jar");
				if (current.exists()) {
					ResourceBuilder resourceBuilder = new ResourceBuilder();
					resourceBuilder.addFile(current, current.toURI());
					resourcesRepository.add(resourceBuilder.build());
				}
			}
			xmlResourceGenerator.name(indexName)
				.repository(resourcesRepository)
				.save(outputFile);
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
		if (fail) {
			throw new MojoFailureException("One or more URI lookups failed");
		}
		attach(outputFile, "osgi-index", "xml");

		if (includeGzip) {
			File gzipOutputFile = new File(outputFile.getPath() + ".gz");

			try {
				xmlResourceGenerator.save(gzipOutputFile);
			} catch (Exception e) {
				throw new MojoExecutionException("Unable to create the gzipped output file");
			}
			attach(gzipOutputFile, "osgi-index", "xml.gz");
		}

	}

	private ArtifactRepository parseAltDistRepo(String repo, boolean releases, boolean snapshots) {

		if (repo == null) {
			return null;
		}

		String[] tokens = repo.split("::", 3);

		if (!"default".equals(tokens[1])) {
			logger.warn("Ignoring repository {} as it uses a non-default layout {}", tokens[0], tokens[1]);
			return null;
		}

		return new MavenArtifactRepository(tokens[0], tokens[2], new DefaultRepositoryLayout(),
			new ArtifactRepositoryPolicy(snapshots, "always", "warn"),
			new ArtifactRepositoryPolicy(releases, "always", "warn"));
	}

	protected void addDeploymentRepo(Map<String, ArtifactRepository> repositories, ArtifactRepository deploymentRepo) {
		if (deploymentRepo != null) {
			logger.debug("Located a deployment repository {}", deploymentRepo.getId());
			if (repositories.get(deploymentRepo.getId()) == null) {
				repositories.put(deploymentRepo.getId(), deploymentRepo);
			} else {
				logger.info(
					"The configured deployment repository {} has the same id as one of the remote artifact repositories. It is assumed that these repositories are the same.",
					deploymentRepo.getId());
			}
		}
	}

	private void attach(File file, String type, String extension) {
		if (!attach) {
			logger.debug("The indexer is not attaching file {} with type {} and extension {} as attachment is disabled",
				file, type, extension);
			return;
		}

		DefaultArtifactHandler handler = new DefaultArtifactHandler(type);
		handler.setExtension(extension);
		DefaultArtifact artifact = new DefaultArtifact(project.getGroupId(), project.getArtifactId(),
			project.getVersion(), null, type, null, handler);
		artifact.setFile(file);
		project.addAttachedArtifact(artifact);
	}

	class MavenURLResolver {

		public static final String MIME = "application/zip";

		public URI resolver(File file, ArtifactResult artifactResult) throws Exception {
			try {
				Artifact artifact = artifactResult.getArtifact();

				StringBuilder sb = new StringBuilder("mvn://");

				sb.append(artifact.getGroupId())
					.append("/")
					.append(artifact.getArtifactId())
					.append("/");

				if (artifact.getVersion() != null) {
					sb.append(artifact.getVersion());
				}

				sb.append("/");

				String type = artifact.getProperty(ArtifactProperties.TYPE, artifact.getExtension());
				if (type != null) {
					sb.append(type);
				}

				sb.append("/");

				if (artifact.getClassifier() != null) {
					sb.append(artifact.getClassifier());
				}

				return URI.create(sb.toString())
					.normalize();
			} catch (Exception e) {
				fail = true;
				logger.error("Failed to determine the artifact URI", e);
				throw e;
			}
		}
	}

	class RepositoryURLResolver {

		private final Map<String, ArtifactRepository> repositories;

		public RepositoryURLResolver(Map<String, ArtifactRepository> repositories) {
			this.repositories = repositories;
		}

		public URI resolver(File file, ArtifactResult artifactResult) throws Exception {
			try {
				if (localURLs == REQUIRED) {
					return file.toURI();
				}

				Artifact artifact = artifactResult.getArtifact();

				ArtifactRepository repo = repositories.get(artifactResult.getRepository()
					.getId());

				if (repo == null) {
					if (localURLs == ALLOWED) {
						logger.info(
							"The Artifact {} could not be found in any repository, returning the local location",
							artifact);
						return file.toURI();
					}
					throw new FileNotFoundException(
						"Unable to index artifact " + artifact + ". The repository " + artifactResult.getRepository()
							.getId() + " is not known to this resolver");
				}

				String baseUrl = repo.getUrl();
				if (baseUrl.startsWith("file:")) {
					// File URLs on Windows are nasty, so send them via a file
					baseUrl = new File(baseUrl.substring(5)).toURI()
						.normalize()
						.toString();
				}

				// The base URL must always point to a directory
				if (!baseUrl.endsWith("/")) {
					baseUrl = baseUrl + "/";
				}

				String artifactPath = repo.getLayout()
					.pathOf(RepositoryUtils.toArtifact(artifact));

				// The artifact path should never be absolute, it is always
				// relative to the repo URL
				while (artifactPath.startsWith("/")) {
					artifactPath = artifactPath.substring(1);
				}

				// As we have sorted the trailing and leading / characters
				// resolve should do the rest!
				return URI.create(baseUrl)
					.resolve(artifactPath)
					.normalize();
			} catch (Exception e) {
				fail = true;
				logger.error("Failed to determine the artifact URI for artifact {}", artifactResult.getArtifact(), e);
				throw e;
			}
		}
	}

}
