package aQute.bnd.maven.baseline.plugin;

import static org.apache.maven.plugins.annotations.LifecyclePhase.VERIFY;

import java.io.IOException;
import java.util.List;

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

import aQute.bnd.differ.Baseline;
import aQute.bnd.differ.Baseline.BundleInfo;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.osgi.Jar;
import aQute.libg.reporter.ReporterAdapter;
import aQute.service.reporter.Reporter;

/**
 * Exports project dependencies to OSGi R5 index format.
 */
@Mojo(name = "baseline", defaultPhase = VERIFY)
public class BaselineMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject			project;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
	private RepositorySystemSession	session;

	@Parameter(property = "bnd.baseline.fail.on.missing", defaultValue = "true", readonly = true)
	private boolean					failOnMissing;

	@Parameter(property = "bnd.baseline.include.distribution.management", defaultValue = "true", readonly = true)
	private boolean					includeDistributionManagement;

	@Parameter(property = "bnd.baseline.full.report", defaultValue = "false", readonly = true)
	private boolean					fullReport;

	@Parameter(property = "bnd.baseline.continue.on.error", defaultValue = "false", readonly = true)
	private boolean					continueOnError;

	@Parameter(readonly = true, required = false)
	private Base					base;

    @Parameter(defaultValue = "false", readonly = true)
    private boolean				skip;
    
	@Component
	private RepositorySystem		system;

	public void execute() throws MojoExecutionException, MojoFailureException {
        if ( skip ) {
			getLog().debug("skip project as configured");
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
						getLog().warn("The baselining check failed when checking " + artifact + " against "
								+ artifactResult.getArtifact()
								+ " but the bnd-baseline-maven-plugin is configured not to fail the build.");
					} else {
						throw new MojoExecutionException("The baselining plugin detected versioning errors");
					}
				} else {
					getLog().info("Baselining check succeeded checking " + artifact + " against "
							+ artifactResult.getArtifact());
				}
			} else {
				if (failOnMissing) {
					throw new MojoExecutionException("Unable to locate a previous version of the artifact");
				} else {
					getLog().warn("No previous version of " + artifact + " could be found to baseline against");
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
				tmpArtifact.setVersion("1.0.0");
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

		getLog().debug("Baselining against " + base + ", fail on missing: " + failOnMissing);
	}

	protected void searchForBaseVersion(Artifact artifact, List<RemoteRepository> aetherRepos)
			throws VersionRangeResolutionException {
		getLog().info("Automatically determining the baseline version for " + artifact + " using repositories "
				+ aetherRepos);

		Artifact toFind = new DefaultArtifact(base.getGroupId(), base.getArtifactId(), base.getClassifier(),
				base.getExtension(), base.getVersion());

		Artifact toCheck = toFind.setVersion("(," + artifact.getVersion() + ")");

		VersionRangeRequest request = new VersionRangeRequest(toCheck, aetherRepos, "baseline");

		VersionRangeResult versions = system.resolveVersionRange(session, request);

		getLog().debug("Found versions " + String.valueOf(versions.getVersions()));

		base.setVersion(versions.getHighestVersion() != null ? versions.getHighestVersion().toString() : null);

		getLog().info("The baseline version was found to be " + base.getVersion());
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
				getLog().error(String.format(
						"Baseline mismatch for package %s, %s change. Current is %s, repo is %s, suggest %s or %s\n",
						info.packageName, info.packageDiff.getDelta(), info.newerVersion, info.olderVersion,
						info.suggestedVersion, info.suggestedIfProviders == null ? "-" : info.suggestedIfProviders));
			}
		}

		BundleInfo binfo = baseline.getBundleInfo();
		if (binfo.mismatch) {
			failed = true;
			getLog().error(String.format(
					"The bundle version change (%s to %s) is too low, the new version must be at least %s",
					binfo.olderVersion, binfo.newerVersion, binfo.suggestedVersion));
		}
		return failed;
	}
}
