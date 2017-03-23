package aQute.bnd.maven.indexer.plugin;

import static aQute.bnd.maven.indexer.plugin.LocalURLs.ALLOWED;
import static aQute.bnd.maven.indexer.plugin.LocalURLs.REQUIRED;
import static java.util.Collections.singletonList;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;
import static org.eclipse.aether.metadata.Metadata.Nature.RELEASE;
import static org.eclipse.aether.metadata.Metadata.Nature.SNAPSHOT;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.MetadataReader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;
import org.osgi.service.repository.ContentNamespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.resource.CapabilityBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.libg.cryptography.SHA256;

/**
 * Exports project dependencies to OSGi R5 index format.
 */
@Mojo(name = "index", defaultPhase = PACKAGE, requiresDependencyResolution = TEST)
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

	@Parameter(property = "bnd.indexer.scopes", readonly = true)
	private List<String>				scopes;

	@Parameter(property = "bnd.indexer.include.gzip", defaultValue = "true")
	private boolean						includeGzip;

	@Parameter(property = "bnd.indexer.skip", defaultValue = "false")
	private boolean						skip;

	@Component
	private RepositorySystem			system;

	@Component
	private ProjectDependenciesResolver	resolver;

	@Component
	private MetadataReader				metadataReader;

	@Component
	private MavenProjectHelper			projectHelper;

	private boolean						fail;

	public void execute() throws MojoExecutionException, MojoFailureException {

        if ( skip ) {
			logger.debug("skip project as configured");
			return;
		}

		if (scopes == null || scopes.isEmpty()) {
			scopes = Arrays.asList("compile", "runtime");
		}

		logger.debug("Indexing dependencies with scopes: {}", scopes);
		logger.debug("Including Transitive dependencies: {}", includeTransitive);
		logger.debug("Local file URLs permitted: {}", localURLs);
		logger.debug("Adding mvn: URLs as alternative content: {}", addMvnURLs);

		DependencyResolutionRequest request = new DefaultDependencyResolutionRequest(project, session);

		request.setResolutionFilter(new DependencyFilter() {
			@Override
			public boolean accept(DependencyNode node, List<DependencyNode> parents) {
				if (node.getDependency() != null) {
					return scopes.contains(node.getDependency().getScope());
				}
				return false;
			}
		});

		DependencyResolutionResult result;
		try {
			result = resolver.resolve(request);
		} catch (DependencyResolutionException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}

		Map<File,ArtifactResult> dependencies = new HashMap<>();

		DependencyNode dependencyGraph = result.getDependencyGraph();

		if (dependencyGraph != null && !dependencyGraph.getChildren().isEmpty()) {
			List<RemoteRepository> remoteRepositories = new ArrayList<>(project.getRemoteProjectRepositories());

			ArtifactRepository deployRepo = project.getDistributionManagementArtifactRepository();

			if (deployRepo != null) {
				remoteRepositories.add(RepositoryUtils.toRepo(deployRepo));
			}

			discoverArtifacts(dependencies, dependencyGraph.getChildren(), project.getArtifact().getId(),
					remoteRepositories);
		}

		Map<String,ArtifactRepository> repositories = new HashMap<>();

		for (ArtifactRepository artifactRepository : project.getRemoteArtifactRepositories()) {
			logger.debug("Located an artifact repository {}", artifactRepository.getId());
			repositories.put(artifactRepository.getId(), artifactRepository);
		}

		ArtifactRepository deploymentRepo = project.getDistributionManagementArtifactRepository();

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

		outputFile.getParentFile().mkdirs();

		RepositoryURLResolver repositoryURLResolver = new RepositoryURLResolver(repositories);
		MavenURLResolver mavenURLResolver = new MavenURLResolver();

		ResourcesRepository resourcesRepository = new ResourcesRepository();
		XMLResourceGenerator xmlResourceGenerator = new XMLResourceGenerator();

		logger.debug("Indexing artifacts: {}", dependencies.keySet());
		try {
			for (Entry<File,ArtifactResult> entry : dependencies.entrySet()) {
				File file = entry.getKey();
				ResourceBuilder resourceBuilder = new ResourceBuilder();
				resourceBuilder.addFile(entry.getKey(), repositoryURLResolver.resolver(file, entry.getValue()));

				if (addMvnURLs) {
					CapabilityBuilder c = new CapabilityBuilder(ContentNamespace.CONTENT_NAMESPACE);
					c.addAttribute(ContentNamespace.CONTENT_NAMESPACE, SHA256.digest(file).asHex());
					c.addAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE,
							mavenURLResolver.resolver(file, entry.getValue()));
					c.addAttribute(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE, file.length());
					c.addAttribute(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE, MavenURLResolver.MIME);
					resourceBuilder.addCapability(c);
				}
				resourcesRepository.add(resourceBuilder.build());
			}
			if (includeJar && project.getPackaging().equals("jar")) {
				File current = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".jar");
				if (current.exists()) {
					ResourceBuilder resourceBuilder = new ResourceBuilder();
					resourceBuilder.addFile(current, current.toURI());
					resourcesRepository.add(resourceBuilder.build());
				}
			}
			xmlResourceGenerator.repository(resourcesRepository).save(outputFile);
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
		if (fail) {
			throw new MojoExecutionException("One or more URI lookups failed");
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

	private void attach(File file, String type, String extension) {
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

				sb.append(artifact.getGroupId()).append("/").append(artifact.getArtifactId()).append("/");

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

				return URI.create(sb.toString()).normalize();
			} catch (Exception e) {
				fail = true;
				logger.error("Failed to determine the artifact URI", e);
				throw e;
			}
		}
	}

	class RepositoryURLResolver {

		private final Map<String,ArtifactRepository>	repositories;

		public RepositoryURLResolver(Map<String,ArtifactRepository> repositories) {
			this.repositories = repositories;
		}

		public URI resolver(File file, ArtifactResult artifactResult) throws Exception {
			try {
				if (localURLs == REQUIRED) {
					return file.toURI();
				}

				Artifact artifact = artifactResult.getArtifact();

				ArtifactRepository repo = repositories.get(artifactResult.getRepository().getId());

				if (repo == null) {
					if (localURLs == ALLOWED) {
						logger.info(
								"The Artifact {} could not be found in any repository, returning the local location",
								artifact);
						return file.toURI();
					}
					throw new FileNotFoundException("The repository " + artifactResult.getRepository().getId()
							+ " is not known to this resolver");
				}

				String baseUrl = repo.getUrl();
				if (baseUrl.startsWith("file:")) {
					// File URLs on Windows are nasty, so send them via a file
					baseUrl = new File(baseUrl.substring(5)).toURI().normalize().toString();
				}

				// The base URL must always point to a directory
				if (!baseUrl.endsWith("/")) {
					baseUrl = baseUrl + "/";
				}

				String artifactPath = repo.getLayout().pathOf(RepositoryUtils.toArtifact(artifact));

				// The artifact path should never be absolute, it is always
				// relative to the repo URL
				while (artifactPath.startsWith("/")) {
					artifactPath = artifactPath.substring(1);
				}

				// As we have sorted the trailing and leading / characters
				// resolve should do the rest!
				return URI.create(baseUrl).resolve(artifactPath).normalize();
			} catch (Exception e) {
				fail = true;
				logger.error("Failed to determine the artifact URI", e);
				throw e;
			}
		}
	}

	private void discoverArtifacts(Map<File,ArtifactResult> files, List<DependencyNode> nodes, String parent,
			List<RemoteRepository> remoteRepositories)
			throws MojoExecutionException {

		for (DependencyNode node : nodes) {
			// Ensure that the file is downloaded so we can index it
			try {
				ArtifactResult resolvedArtifact = postProcessResult(system.resolveArtifact(session,
						new ArtifactRequest(node.getArtifact(), remoteRepositories, parent)));
				logger.debug("Located file: {} for artifact {}", resolvedArtifact.getArtifact().getFile(),
						resolvedArtifact);

				files.put(resolvedArtifact.getArtifact().getFile(), resolvedArtifact);
			} catch (ArtifactResolutionException e) {
				throw new MojoExecutionException("Failed to resolve the dependency " + node.getArtifact().toString(),
						e);
			}
			if (includeTransitive) {
				discoverArtifacts(files, node.getChildren(), node.getRequestContext(), remoteRepositories);
			} else {
				logger.debug("Ignoring transitive dependencies of {}", node.getDependency());
			}
		}
	}

	private ArtifactResult postProcessResult(ArtifactResult resolvedArtifact) throws MojoExecutionException {

		if (localURLs == REQUIRED) {
			// Skip the search as we will use the local file anyway
			return resolvedArtifact;
		}

		String repoId = resolvedArtifact.getRepository().getId();
		Artifact artifact = resolvedArtifact.getArtifact();
		if ("workspace".equals(repoId) || "local".equals(repoId)) {
			logger.debug("Post processing {} to determine a remote source", artifact);
			ArtifactResult postProcessed;
			if (artifact.isSnapshot()) {
				postProcessed = postProcessSnapshot(resolvedArtifact.getRequest(), artifact);
			} else {
				postProcessed = postProcessRelease(resolvedArtifact.getRequest(), artifact);
			}
			if (postProcessed != null) {
				return postProcessed;
			}
		}
		return resolvedArtifact;
	}

	private ArtifactResult postProcessSnapshot(ArtifactRequest request, Artifact artifact)
			throws MojoExecutionException {

		for (RemoteRepository repository : request.getRepositories()) {
			if (!repository.getPolicy(true).isEnabled()) {
				// Skip the repo if it isn't enabled for snapshots
				continue;
			}

			// Remove the workspace from the session so that we don't use it
			DefaultRepositorySystemSession newSession = new DefaultRepositorySystemSession(session);
			newSession.setWorkspaceReader(null);

			// Find the snapshot metadata for the module
			MetadataRequest mr = new MetadataRequest().setRepository(repository)
					.setMetadata(new DefaultMetadata(artifact.getGroupId(), artifact.getArtifactId(),
							artifact.getVersion(), "maven-metadata.xml", SNAPSHOT));

			for (MetadataResult metadataResult : system.resolveMetadata(newSession, singletonList(mr))) {
				if (metadataResult.isResolved()) {
					String version;
					try {
						Metadata read = metadataReader.read(metadataResult.getMetadata().getFile(), null);
						Versioning versioning = read.getVersioning();
						if (versioning == null || versioning.getSnapshotVersions() == null
								|| versioning.getSnapshotVersions().isEmpty()) {
							continue;
						} else {
							version = versioning.getSnapshotVersions().get(0).getVersion();
						}
					} catch (Exception e) {
						throw new MojoExecutionException("Unable to read project metadata for " + artifact, e);
					}
					Artifact fullVersionArtifact = new org.eclipse.aether.artifact.DefaultArtifact(
							artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(),
							artifact.getExtension(), version);
					try {
						ArtifactResult result = system.resolveArtifact(newSession,
								new ArtifactRequest().setArtifact(fullVersionArtifact).addRepository(repository));
						if (result.isResolved()) {
							File toUse = new File(session.getLocalRepository().getBasedir(),
									session.getLocalRepositoryManager().getPathForRemoteArtifact(fullVersionArtifact,
											repository, artifact.toString()));
							if (!toUse.exists()) {
								logger.warn(
										"The resolved artifact {} does not exist at {}", fullVersionArtifact, toUse);
								continue;
							} else {
								logger.debug("Located snapshot file {} for artifact {}", toUse, artifact);
							}
							result.getArtifact().setFile(toUse);
							return result;
						}
					} catch (ArtifactResolutionException e) {
						logger.debug("Unable to locate the artifact {}", fullVersionArtifact, e);
					}
				}
			}
		}

		logger.debug("Unable to resolve a remote repository containing {}", artifact);

		return null;
	}

	private ArtifactResult postProcessRelease(ArtifactRequest request, Artifact artifact)
			throws MojoExecutionException {

		for (RemoteRepository repository : request.getRepositories()) {
			if (!repository.getPolicy(false).isEnabled()) {
				// Skip the repo if it isn't enabled for releases
				continue;
			}

			// Remove the workspace from the session so that we don't use it
			DefaultRepositorySystemSession newSession = new DefaultRepositorySystemSession(session);
			newSession.setWorkspaceReader(null);

			// Find the snapshot metadata for the module
			MetadataRequest mr = new MetadataRequest().setRepository(repository).setMetadata(new DefaultMetadata(
					artifact.getGroupId(), artifact.getArtifactId(), null, "maven-metadata.xml", RELEASE));

			for (MetadataResult metadataResult : system.resolveMetadata(newSession, singletonList(mr))) {
				if (metadataResult.isResolved()) {
					try {
						Metadata read = metadataReader.read(metadataResult.getMetadata().getFile(), null);
						Versioning versioning = read.getVersioning();
						if (versioning == null || versioning.getVersions() == null
								|| versioning.getVersions().isEmpty()) {
							continue;
						} else if (versioning.getVersions().contains(artifact.getVersion())) {

							ArtifactResult result = system.resolveArtifact(newSession,
									new ArtifactRequest().setArtifact(artifact).addRepository(repository));
							if (result.isResolved()) {
								File toUse = new File(session.getLocalRepository().getBasedir(),
										session.getLocalRepositoryManager().getPathForLocalArtifact(artifact));
								if (!toUse.exists()) {
									logger.warn("The resolved artifact {} does not exist at {}", artifact, toUse);
									continue;
								} else {
									logger.debug("Located snapshot file {} for artifact {}", toUse, artifact);
								}
								result.getArtifact().setFile(toUse);
								return result;
							}
						}
					} catch (Exception e) {
						throw new MojoExecutionException("Unable to read project metadata for " + artifact, e);
					}
				}
			}
		}

		logger.debug("Unable to resolve a remote repository containing {}", artifact);

		return null;
	}

}
