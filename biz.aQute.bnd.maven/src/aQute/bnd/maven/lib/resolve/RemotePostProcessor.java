package aQute.bnd.maven.lib.resolve;

import static java.util.Collections.singletonList;
import static org.eclipse.aether.metadata.Metadata.Nature.RELEASE;
import static org.eclipse.aether.metadata.Metadata.Nature.SNAPSHOT;

import java.io.File;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.MetadataReader;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemotePostProcessor implements PostProcessor {

	private static final Logger				logger	= LoggerFactory.getLogger(RemotePostProcessor.class);

	private final LocalURLs					localURLs;
	private final MetadataReader			metadataReader;
	private final RepositorySystemSession	session;
	private final RepositorySystem			system;

	public RemotePostProcessor(RepositorySystemSession session, RepositorySystem system, MetadataReader metadataReader,
		LocalURLs localURLs) {

		this.session = session;
		this.system = system;
		this.metadataReader = metadataReader;
		this.localURLs = localURLs;
	}

	@Override
	public ArtifactResult postProcessResult(ArtifactResult resolvedArtifact) throws MojoExecutionException {

		if (localURLs == LocalURLs.REQUIRED) {
			// Skip the search as we will use the local file anyway
			return resolvedArtifact;
		}

		String repoId = resolvedArtifact.getRepository()
			.getId();
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
			if (!repository.getPolicy(true)
				.isEnabled()) {
				// Skip the repo if it isn't enabled for snapshots
				continue;
			}

			// Remove the workspace from the session so that we don't use it
			DefaultRepositorySystemSession newSession = new DefaultRepositorySystemSession(session);
			newSession.setWorkspaceReader(null);

			// Find the snapshot metadata for the module
			MetadataRequest mr = new MetadataRequest().setRepository(repository)
				.setMetadata(new DefaultMetadata(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
					"maven-metadata.xml", SNAPSHOT));

			for (MetadataResult metadataResult : system.resolveMetadata(newSession, singletonList(mr))) {
				if (metadataResult.isResolved()) {
					String version;
					try {
						Metadata read = metadataReader.read(metadataResult.getMetadata()
							.getFile(), null);
						Versioning versioning = read.getVersioning();
						if (versioning == null || versioning.getSnapshotVersions() == null
							|| versioning.getSnapshotVersions()
								.isEmpty()) {
							continue;
						}
						version = versioning.getSnapshotVersions()
							.get(0)
							.getVersion();
					} catch (Exception e) {
						throw new MojoExecutionException("Unable to read project metadata for " + artifact, e);
					}
					Artifact fullVersionArtifact = new org.eclipse.aether.artifact.DefaultArtifact(
						artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(),
						artifact.getExtension(), version);
					try {
						ArtifactResult result = system.resolveArtifact(newSession,
							new ArtifactRequest().setArtifact(fullVersionArtifact)
								.addRepository(repository));
						if (result.isResolved()) {
							File toUse = new File(session.getLocalRepository()
								.getBasedir(),
								session.getLocalRepositoryManager()
									.getPathForRemoteArtifact(fullVersionArtifact, repository, artifact.toString()));
							if (!toUse.exists()) {
								logger.warn("The resolved artifact {} does not exist at {}", fullVersionArtifact,
									toUse);
								continue;
							}
							logger.debug("Located snapshot file {} for artifact {}", toUse, artifact);
							result.getArtifact()
								.setFile(toUse);
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
			if (!repository.getPolicy(false)
				.isEnabled()) {
				// Skip the repo if it isn't enabled for releases
				continue;
			}

			// Remove the workspace from the session so that we don't use it
			DefaultRepositorySystemSession newSession = new DefaultRepositorySystemSession(session);
			newSession.setWorkspaceReader(null);

			// Find the snapshot metadata for the module
			MetadataRequest mr = new MetadataRequest().setRepository(repository)
				.setMetadata(new DefaultMetadata(artifact.getGroupId(), artifact.getArtifactId(), null,
					"maven-metadata.xml", RELEASE));

			for (MetadataResult metadataResult : system.resolveMetadata(newSession, singletonList(mr))) {
				if (metadataResult.isResolved()) {
					try {
						Metadata read = metadataReader.read(metadataResult.getMetadata()
							.getFile(), null);
						Versioning versioning = read.getVersioning();
						if (versioning == null || versioning.getVersions() == null || versioning.getVersions()
							.isEmpty()) {
							continue;
						} else if (versioning.getVersions()
							.contains(artifact.getVersion())) {

							ArtifactResult result = system.resolveArtifact(newSession,
								new ArtifactRequest().setArtifact(artifact)
									.addRepository(repository));
							if (result.isResolved()) {
								File toUse = new File(session.getLocalRepository()
									.getBasedir(),
									session.getLocalRepositoryManager()
										.getPathForLocalArtifact(artifact));
								if (!toUse.exists()) {
									logger.warn("The resolved artifact {} does not exist at {}", artifact, toUse);
									continue;
								}
								logger.debug("Located snapshot file {} for artifact {}", toUse, artifact);
								result.getArtifact()
									.setFile(toUse);
								result.setRepository(repository);
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
