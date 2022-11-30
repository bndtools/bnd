package aQute.bnd.maven.baseline.plugin;

import static org.apache.maven.plugins.annotations.LifecyclePhase.VERIFY;

import java.util.List;
import java.util.ListIterator;

import aQute.bnd.version.MavenVersion;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.version.Version;

/**
 * Exports project dependencies to OSGi R5 index format.
 */
@Mojo(name = "baseline", defaultPhase = VERIFY, threadSafe = true)
public class BaselineMojo extends AbstractBaselineMojo {

	private static final String		PACKAGING_POM	= "pom";

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject			project;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
	private RepositorySystemSession	session;

	@Parameter(property = "bnd.baseline.fail.on.missing", defaultValue = "true")
	private boolean					failOnMissing;

	@Parameter(property = "bnd.baseline.include.distribution.management", defaultValue = "true")
	private boolean					includeDistributionManagement;

	@Parameter
	private Base					base;

	@Parameter(property = "bnd.baseline.skip", defaultValue = "false")
	private boolean					skip;

	@Parameter(property = "bnd.baseline.releaseversions", defaultValue = "false")
	private boolean					releaseversions;

	@Component
	private RepositorySystem		system;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			logger.debug("skip project as configured");
			return;
		}

		// Exit without generating anything if this is a pom-packaging project.
		// Probably it's just a parent project.
		if (PACKAGING_POM.equals(project.getPackaging())) {
			logger.info("skip project with packaging=pom");
			return;
		}

		Artifact artifact = RepositoryUtils.toArtifact(project.getArtifact());

		List<RemoteRepository> aetherRepos = getRepositories(artifact);

		setupBase(artifact);

		try {
			searchForBaseVersion(aetherRepos);
			if (base.getVersion() != null && !base.getVersion()
				.isEmpty()) {

				ArtifactResult artifactResult = locateBaseJar(aetherRepos);

				baselineAction(artifact.getFile(), artifactResult.getArtifact()
					.getFile());
			} else {
				if (failOnMissing) {
					throw new MojoFailureException("Unable to locate a previous version of the artifact");
				} else {
					logger.warn("No previous version of {} could be found to baseline against", artifact);
				}
			}
		} catch (RepositoryException re) {
			throw new MojoFailureException("Unable to locate a previous version of the artifact", re);
		} catch (Exception e) {
			throw new MojoExecutionException("An error occurred while calculating the baseline", e);
		}
	}

	private List<RemoteRepository> getRepositories(Artifact artifact) {
		List<RemoteRepository> aetherRepos = RepositoryUtils.toRepos(project.getRemoteArtifactRepositories());

		if (includeDistributionManagement) {
			RemoteRepository releaseDistroRepo;
			if (artifact.isSnapshot()) {
				MavenProject tmpClone = project.clone();
				tmpClone.getArtifact()
					.setVersion("1.0.0");
				releaseDistroRepo = RepositoryUtils.toRepo(tmpClone.getDistributionManagementArtifactRepository());
			} else {
				releaseDistroRepo = RepositoryUtils.toRepo(project.getDistributionManagementArtifactRepository());
			}

			// Issue #2040:
			// Don't fail on projects without distributionManagement
			if (releaseDistroRepo != null) {
				aetherRepos.add(0, releaseDistroRepo);
			}
		}

		return aetherRepos;
	}

	private void setupBase(Artifact artifact) {
		if (base == null) {
			base = new Base();
		}
		if (base.getGroupId() == null || base.getGroupId()
			.isEmpty()) {
			base.setGroupId(project.getGroupId());
		}
		if (base.getArtifactId() == null || base.getArtifactId()
			.isEmpty()) {
			base.setArtifactId(project.getArtifactId());
		}
		if (base.getClassifier() == null || base.getClassifier()
			.isEmpty()) {
			base.setClassifier(artifact.getClassifier());
		}
		if (base.getExtension() == null || base.getExtension()
			.isEmpty()) {
			base.setExtension(artifact.getExtension());
		}
		if (base.getVersion() == null || base.getVersion()
			.isEmpty()) {
			base.setVersion("(," + artifact.getVersion() + ")");
		}

		logger.debug("Baselining against {}, fail on missing: {}", base, failOnMissing);
	}

	private void searchForBaseVersion(List<RemoteRepository> aetherRepos) throws VersionRangeResolutionException {
		logger.info("Determining the baseline version for {} using repositories {}", base, aetherRepos);

		Artifact toFind = new DefaultArtifact(base.getGroupId(), base.getArtifactId(), base.getClassifier(),
			base.getExtension(), base.getVersion());

		VersionRangeRequest request = new VersionRangeRequest(toFind, aetherRepos, "baseline");

		VersionRangeResult versions = system.resolveVersionRange(session, request);

		List<Version> found = versions.getVersions();
		logger.debug("Found versions {}", found);

		boolean onlyreleaseversions = releaseversions && (base.getVersion()
			.startsWith("[")
			|| base.getVersion()
				.startsWith("("));

		base.setVersion(null);
		for (ListIterator<Version> li = found.listIterator(found.size()); li.hasPrevious();) {
			String highest = li.previous()
				.toString();
			if (toFind.setVersion(highest)
				.isSnapshot()) {
				continue;
			}
			if (onlyreleaseversions) {
				MavenVersion mavenVersion = MavenVersion.parseMavenString(highest);
				if (mavenVersion.compareTo(mavenVersion.toReleaseVersion()) < 0) {
					logger.debug("Version {} not considered since it is not a release version", highest);
					continue; // not a release version
				}
			}
			base.setVersion(highest);
			break;
		}

		logger.info("The baseline version was found to be {}", base.getVersion());
	}

	private ArtifactResult locateBaseJar(List<RemoteRepository> aetherRepos) throws ArtifactResolutionException {
		Artifact toFind = new DefaultArtifact(base.getGroupId(), base.getArtifactId(), base.getClassifier(),
			base.getExtension(), base.getVersion());

		return system.resolveArtifact(session, new ArtifactRequest(toFind, aetherRepos, "baseline"));
	}


}
