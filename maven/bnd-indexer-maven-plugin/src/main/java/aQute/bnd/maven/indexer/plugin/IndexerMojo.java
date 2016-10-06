package aQute.bnd.maven.indexer.plugin;

import static aQute.bnd.maven.indexer.plugin.LocalURLs.ALLOWED;
import static aQute.bnd.maven.indexer.plugin.LocalURLs.REQUIRED;
import static java.util.Collections.singletonList;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;
import static org.eclipse.aether.metadata.Metadata.Nature.RELEASE;
import static org.eclipse.aether.metadata.Metadata.Nature.SNAPSHOT;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

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
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.indexer.ResourceIndexer;
import org.osgi.service.indexer.impl.KnownBundleAnalyzer;
import org.osgi.service.indexer.impl.RepoIndex;
import org.osgi.service.indexer.impl.URLResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	@Parameter(property = "bnd.indexer.output.file", defaultValue = "${project.build.directory}/index.xml", readonly = true)
	private File						outputFile;

	@Parameter(property = "bnd.indexer.localURLs", defaultValue = "FORBIDDEN", readonly = true)
	private LocalURLs					localURLs;

	@Parameter(property = "bnd.indexer.includeTransitive", defaultValue = "true", readonly = true)
	private boolean						includeTransitive;

	@Parameter(property = "bnd.indexer.add.mvn.urls", defaultValue = "false", readonly = true)
	private boolean						addMvnURLs;

	@Parameter(property = "bnd.indexer.scopes", readonly = true, required = false)
	private List<String>				scopes;

	@Parameter(property = "bnd.indexer.include.gzip", defaultValue = "true", readonly = true)
	private boolean						includeGzip;

	@Parameter(defaultValue = "false", readonly = true)
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

		if (result.getDependencyGraph() != null && !result.getDependencyGraph().getChildren().isEmpty()) {
			discoverArtifacts(dependencies, result.getDependencyGraph().getChildren(), project.getArtifact().getId());
		}

		Map<String,ArtifactRepository> repositories = new HashMap<>();

		for (ArtifactRepository artifactRepository : project.getRemoteArtifactRepositories()) {
			repositories.put(artifactRepository.getId(), artifactRepository);
		}

		RepoIndex indexer = new RepoIndex();
		Filter filter;
		try {
			filter = FrameworkUtil.createFilter("(name=*.jar)");
		} catch (InvalidSyntaxException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
		indexer.addAnalyzer(new KnownBundleAnalyzer(), filter);

		indexer.addURLResolver(new RepositoryURLResolver(dependencies, repositories));

		if (addMvnURLs) {
			indexer.addURLResolver(new MavenURLResolver(dependencies));
		}

		Map<String,String> config = new HashMap<String,String>();
		config.put(ResourceIndexer.PRETTY, "true");

		OutputStream output;
		try {
			outputFile.getParentFile().mkdirs();
			output = new FileOutputStream(outputFile);
		} catch (FileNotFoundException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}

		logger.debug("Indexing artifacts: {}", dependencies.keySet());
		try {
			indexer.index(dependencies.keySet(), output, config);
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
		if (fail) {
			throw new MojoExecutionException("One or more URI lookups failed");
		}
		attach(outputFile, "osgi-index", "xml");

		if (includeGzip) {
			File gzipOutputFile = new File(outputFile.getPath() + ".gz");

			try (InputStream is = new BufferedInputStream(new FileInputStream(outputFile));
					OutputStream gos = new GZIPOutputStream(new FileOutputStream(gzipOutputFile))) {
				byte[] bytes = new byte[4096];
				int read;
				while ((read = is.read(bytes)) != -1) {
					gos.write(bytes, 0, read);
				}
			} catch (IOException ioe) {
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

	class MavenURLResolver implements URLResolver {

		private final Map<File,ArtifactResult> dependencies;

		public MavenURLResolver(Map<File,ArtifactResult> dependencies) {
			this.dependencies = dependencies;
		}

		public URI resolver(File file) throws Exception {
			try {
				ArtifactResult artifactResult = dependencies.get(file);

				if (artifactResult == null) {
					throw new FileNotFoundException(
							"The file " + file.getCanonicalPath() + " is not known to this resolver");
				}

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

	class RepositoryURLResolver implements URLResolver {

		private final Map<File,ArtifactResult>			dependencies;
		private final Map<String,ArtifactRepository>	repositories;

		public RepositoryURLResolver(Map<File,ArtifactResult> dependencies,
				Map<String,ArtifactRepository> repositories) {
			this.dependencies = dependencies;
			this.repositories = repositories;
		}

		public URI resolver(File file) throws Exception {
			try {
				ArtifactResult artifactResult = dependencies.get(file);

				if (artifactResult == null) {
					throw new FileNotFoundException(
							"The file " + file.getCanonicalPath() + " is not known to this resolver");
				}

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

	private void discoverArtifacts(Map<File,ArtifactResult> files, List<DependencyNode> nodes, String parent)
			throws MojoExecutionException {
		for (DependencyNode node : nodes) {
			// Ensure that the file is downloaded so we can index it
			try {
				ArtifactResult resolvedArtifact = postProcessResult(system.resolveArtifact(session,
						new ArtifactRequest(node.getArtifact(), project.getRemoteProjectRepositories(), parent)));
				logger.debug("Located file: {} for artifact {}", resolvedArtifact.getArtifact().getFile(),
						resolvedArtifact);

				files.put(resolvedArtifact.getArtifact().getFile(), resolvedArtifact);
			} catch (ArtifactResolutionException e) {
				throw new MojoExecutionException("Failed to resolve the dependency " + node.getArtifact().toString(),
						e);
			}
			if (includeTransitive) {
				discoverArtifacts(files, node.getChildren(), node.getRequestContext());
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

		for (ArtifactRepository repository : project.getRemoteArtifactRepositories()) {
			if (!repository.getSnapshots().isEnabled()) {
				// Skip the repo if it isn't enabled for snapshots
				continue;
			}

			RemoteRepository aetherRepo = RepositoryUtils.toRepo(repository);

			// Remove the workspace from the session so that we don't use it
			DefaultRepositorySystemSession newSession = new DefaultRepositorySystemSession(session);
			newSession.setWorkspaceReader(null);

			// Find the snapshot metadata for the module
			MetadataRequest mr = new MetadataRequest().setRepository(aetherRepo)
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
								new ArtifactRequest().setArtifact(fullVersionArtifact).addRepository(aetherRepo));
						if (result.isResolved()) {
							File toUse = new File(session.getLocalRepository().getBasedir(),
									session.getLocalRepositoryManager().getPathForRemoteArtifact(fullVersionArtifact,
											aetherRepo, artifact.toString()));
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

		for (ArtifactRepository repository : project.getRemoteArtifactRepositories()) {
			if (!repository.getReleases().isEnabled()) {
				// Skip the repo if it isn't enabled for releases
				continue;
			}

			RemoteRepository aetherRepo = RepositoryUtils.toRepo(repository);

			// Remove the workspace from the session so that we don't use it
			DefaultRepositorySystemSession newSession = new DefaultRepositorySystemSession(session);
			newSession.setWorkspaceReader(null);

			// Find the snapshot metadata for the module
			MetadataRequest mr = new MetadataRequest().setRepository(aetherRepo).setMetadata(new DefaultMetadata(
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
									new ArtifactRequest().setArtifact(artifact).addRepository(aetherRepo));
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
