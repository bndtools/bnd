package aQute.bnd.maven.baseline.plugin;

import static org.apache.maven.plugins.annotations.LifecyclePhase.VERIFY;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

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

import aQute.bnd.differ.Baseline;
import aQute.bnd.differ.Baseline.BundleInfo;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.lib.strings.Strings;
import aQute.libg.reporter.ReporterAdapter;
import aQute.service.reporter.Reporter;

/**
 * Exports project dependencies to OSGi R5 index format.
 */
@Mojo(name = "baseline", defaultPhase = VERIFY, threadSafe = true)
public class BaselineMojo extends AbstractMojo {
	private static final Logger		logger			= LoggerFactory.getLogger(BaselineMojo.class);
	private static final String		PACKAGING_POM	= "pom";

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

	@Parameter
	private Base					base;

	@Parameter(required = false)
	private List<String>			diffignores;

	@Parameter(required = false, defaultValue = "*")
	private List<String>			diffpackages;

	@Parameter(property = "bnd.baseline.skip", defaultValue = "false")
	private boolean					skip;

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

		try (Processor processor = new Processor()) {
			searchForBaseVersion(aetherRepos);
			if (base.getVersion() != null && !base.getVersion()
				.isEmpty()) {

				ArtifactResult artifactResult = locateBaseJar(aetherRepos);

				Reporter reporter;
				if (fullReport) {
					reporter = new ReporterAdapter(System.out);
					((ReporterAdapter) reporter).setTrace(true);
				} else {
					reporter = new ReporterAdapter();
				}

				DiffPluginImpl differ = new DiffPluginImpl();
				differ.setIgnore(new Parameters(Strings.join(",", diffignores), processor));
				Baseline baseline = new Baseline(reporter, differ);

				if (checkFailures(artifact, artifactResult, baseline,
					new Instructions(new Parameters(Strings.join(",", diffpackages), processor)))) {
					if (continueOnError) {
						logger.warn(
							"The baselining check failed when checking {} against {} but the bnd-baseline-maven-plugin is configured not to fail the build.",
							artifact, artifactResult.getArtifact());
					} else {
						throw new MojoFailureException("The baselining plugin detected versioning errors");
					}
				} else {
					logger.info("Baselining check succeeded checking {} against {}", artifact,
						artifactResult.getArtifact());
				}
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

		base.setVersion(null);
		for (ListIterator<Version> li = found.listIterator(found.size()); li.hasPrevious();) {
			String highest = li.previous()
				.toString();
			if (!toFind.setVersion(highest)
				.isSnapshot()) {
				base.setVersion(highest);
				break;
			}
		}

		logger.info("The baseline version was found to be {}", base.getVersion());
	}

	private ArtifactResult locateBaseJar(List<RemoteRepository> aetherRepos) throws ArtifactResolutionException {
		Artifact toFind = new DefaultArtifact(base.getGroupId(), base.getArtifactId(), base.getClassifier(),
			base.getExtension(), base.getVersion());

		return system.resolveArtifact(session, new ArtifactRequest(toFind, aetherRepos, "baseline"));
	}

	private boolean checkFailures(Artifact artifact, ArtifactResult artifactResult, Baseline baseline,
		Instructions diffpackages) throws Exception, IOException {
		StringBuffer sb = new StringBuffer();
		try (Formatter f = new Formatter(sb, Locale.US);
			Jar newer = new Jar(artifact.getFile());
			Jar older = new Jar(artifactResult.getArtifact()
				.getFile())) {
			boolean failed = false;

			for (Info info : baseline.baseline(newer, older, diffpackages)) {
				if (info.mismatch) {
					failed = true;
					if (logger.isErrorEnabled()) {
						sb.setLength(0);
						f.format(
							"Baseline mismatch for package %s, %s change. Current is %s, repo is %s, suggest %s or %s",
							info.packageName, info.packageDiff.getDelta(), info.newerVersion, info.olderVersion,
							info.suggestedVersion, info.suggestedIfProviders == null ? "-" : info.suggestedIfProviders);
						if (fullReport) {
							f.format("%n%#S", info.packageDiff);
						}
						logger.error(f.toString());
					}
				}
			}

			BundleInfo binfo = baseline.getBundleInfo();
			if (binfo.mismatch) {
				failed = true;
				if (logger.isErrorEnabled()) {
					sb.setLength(0);
					f.format("The bundle version change (%s to %s) is too low, the new version must be at least %s",
						binfo.olderVersion, binfo.newerVersion, binfo.suggestedVersion);
					if (fullReport) {
						f.format("%n%#S", baseline.getDiff());
					}
					logger.error(f.toString());
				}
			}
			return failed;
		}
	}
}
