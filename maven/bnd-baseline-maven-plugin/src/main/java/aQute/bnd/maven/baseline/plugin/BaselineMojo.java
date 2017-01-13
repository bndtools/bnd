package aQute.bnd.maven.baseline.plugin;

import aQute.bnd.differ.Baseline;
import aQute.bnd.differ.Baseline.BundleInfo;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.osgi.Jar;
import aQute.libg.reporter.ReporterAdapter;
import aQute.service.reporter.Reporter;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.plugin.AbstractMojo;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.maven.plugins.annotations.LifecyclePhase.VERIFY;

/**
 * Exports project dependencies to OSGi R5 index format.
 */
@Mojo(name = "baseline", defaultPhase = VERIFY)
public class BaselineMojo extends AbstractMojo {
	private static final Logger		logger	= LoggerFactory.getLogger(BaselineMojo.class);

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject			project;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
	private RepositorySystemSession	session;

	@Parameter(property = "bnd.baseline.fail.on.missing", defaultValue = "true")
	private boolean					failOnMissing;

	@Parameter(property = "bnd.baseline.include.distribution.management", defaultValue = "true")
	private boolean					includeDistributionManagement;

	@Parameter(property = "bnd.baseline.full.report", defaultValue = "false")
	private boolean					fullReport;

	@Parameter(property = "bnd.baseline.continue.on.error", defaultValue = "false")
	private boolean					continueOnError;

	@Parameter(property = "bnd.baseline.compare.to.snapshot", defaultValue = "true")
	private boolean					compareToShanpshot;

	@Parameter(readonly = true)
	private Base					base;

	@Parameter(property = "bnd.baseline.skip", defaultValue = "false")
    private boolean				skip;
    
	@Component
	private RepositorySystem		system;

	public void execute() throws MojoExecutionException, MojoFailureException {
        if ( skip ) {
			logger.debug("skip project as configured");
			return;
		}

		Artifact artifact = RepositoryUtils.toArtifact(project.getArtifact());

		List<RemoteRepository> aetherRepos = getRepositories(artifact);

		setupBase(artifact);

		try {
			if (base.getVersion() == null || base.getVersion().isEmpty()) {
				searchForBaseVersion(artifact, aetherRepos);
			}

			if (base.getVersion() != null && !base.getVersion().isEmpty()) {

				ArtifactResult artifactResult = locateBaseJar(aetherRepos);

				Reporter reporter;
				if (fullReport) {
					reporter = new ReporterAdapter(System.out);
					((ReporterAdapter) reporter).setTrace(true);
				} else {
					reporter = new ReporterAdapter();
				}

				Baseline baseline = new Baseline(reporter, new DiffPluginImpl());

				if (checkFailures(artifact, artifactResult, baseline)) {
					if (continueOnError) {
						logger.warn(
								"The baselining check failed when checking {} against {} but the bnd-baseline-maven-plugin is configured not to fail the build.",
								artifact, artifactResult.getArtifact());
					} else {
						throw new MojoExecutionException("The baselining plugin detected versioning errors");
					}
				} else {
					logger.info("Baselining check succeeded checking {} against {}", artifact,
							artifactResult.getArtifact());
				}
			} else {
				if (failOnMissing) {
					throw new MojoExecutionException("Unable to locate a previous version of the artifact");
				} else {
					logger.warn("No previous version of {} could be found to baseline against", artifact);
				}
			}
		} catch (RepositoryException re) {
			throw new MojoExecutionException("Unable to locate a previous version of the artifact", re);
		} catch (Exception e) {
			throw new MojoExecutionException("An error occurred while calculating the baseline", e);
		}
	}

	protected List<RemoteRepository> getRepositories(Artifact artifact) {
		List<RemoteRepository> aetherRepos = RepositoryUtils.toRepos(project.getRemoteArtifactRepositories());

		if (includeDistributionManagement) {
			RemoteRepository releaseDistroRepo;
			if (artifact.isSnapshot()) {
				MavenProject tmpClone = project.clone();
				org.apache.maven.artifact.Artifact tmpArtifact = project.getArtifact();
				tmpClone.setArtifact(tmpArtifact);
				releaseDistroRepo = RepositoryUtils.toRepo(tmpClone.getDistributionManagementArtifactRepository());
			} else {
				releaseDistroRepo = RepositoryUtils.toRepo(project.getDistributionManagementArtifactRepository());
			}

			aetherRepos.add(0, releaseDistroRepo);
		}

		return aetherRepos;
	}

	protected void setupBase(Artifact artifact) {
		if (base == null) {
			base = new Base();
		}
		if (base.getGroupId() == null || base.getGroupId().isEmpty()) {
			base.setGroupId(project.getGroupId());
		}
		if (base.getArtifactId() == null || base.getArtifactId().isEmpty()) {
			base.setArtifactId(project.getArtifactId());
		}
		if (base.getClassifier() == null || base.getClassifier().isEmpty()) {
			base.setClassifier(artifact.getClassifier());
		}
		if (base.getExtension() == null || base.getExtension().isEmpty()) {
			base.setExtension(artifact.getExtension());
		}

		logger.debug("Baselining against {}, fail on missing: {}", base, failOnMissing);
	}

	protected void searchForBaseVersion(Artifact artifact, List<RemoteRepository> aetherRepos)
			throws VersionRangeResolutionException {
		logger.info("Automatically determining the baseline version for {} using repositories {}", artifact,
				aetherRepos);

		Artifact toFind = new DefaultArtifact(base.getGroupId(), base.getArtifactId(), base.getClassifier(),
				base.getExtension(), base.getVersion());

		Artifact toCheck = toFind.setVersion("(," + artifact.getVersion() + ")");

		VersionRangeRequest request = new VersionRangeRequest(toCheck, aetherRepos, "baseline");

		VersionRangeResult versions = system.resolveVersionRange(session, request);

		logger.debug("Found versions {}", versions.getVersions());

		Version baseVersion = getBaseVersion(versions);

		base.setVersion(baseVersion != null ? baseVersion.toString() : null);

		logger.info("The baseline version was found to be {}", base.getVersion());
	}

	private Version getBaseVersion(VersionRangeResult versions) {
		if(compareToShanpshot){
			return versions.getHighestVersion();
		}
		List<Version> foundVersions = versions.getVersions();
		List<Version> versionsWithoutSnapshot = new ArrayList<>();
		for (Version foundVersion : foundVersions) {
			if(!foundVersion.toString().endsWith("SNAPSHOT")){
				versionsWithoutSnapshot.add(foundVersion);
			}
		}
		return versions.setVersions(versionsWithoutSnapshot).getHighestVersion();
	}

	protected ArtifactResult locateBaseJar(List<RemoteRepository> aetherRepos) throws ArtifactResolutionException {
		Artifact toFind = new DefaultArtifact(base.getGroupId(), base.getArtifactId(), base.getClassifier(),
				base.getExtension(), base.getVersion());

		return system.resolveArtifact(session, new ArtifactRequest(toFind, aetherRepos, "baseline"));
	}

	protected boolean checkFailures(Artifact artifact, ArtifactResult artifactResult, Baseline baseline)
			throws Exception, IOException {
		boolean failed = false;

		for (Info info : baseline.baseline(new Jar(artifact.getFile()), new Jar(artifactResult.getArtifact().getFile()),
				null)) {
			if (info.mismatch) {
				failed = true;
				logger.error("Baseline mismatch for package {}, {} change. Current is {}, repo is {}, suggest {} or {}",
						info.packageName, info.packageDiff.getDelta(), info.newerVersion, info.olderVersion,
						info.suggestedVersion, info.suggestedIfProviders == null ? "-" : info.suggestedIfProviders);
			}
		}

		BundleInfo binfo = baseline.getBundleInfo();
		if (binfo.mismatch) {
			failed = true;
			logger.error("The bundle version change ({} to {}) is too low, the new version must be at least {}",
					binfo.olderVersion, binfo.newerVersion, binfo.suggestedVersion);
		}
		return failed;
	}
}
