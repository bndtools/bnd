package aQute.bnd.maven.baseline.plugin;

import static org.apache.maven.plugins.annotations.LifecyclePhase.VERIFY;

import java.io.File;
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Objects;

import aQute.bnd.differ.Baseline;
import aQute.bnd.differ.Baseline.BundleInfo;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.version.MavenVersion;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
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

	/**
	 * Also use the remote repository given via
	 * {@code project.distributionManagement.repository.url} for retrieving the
	 * base artifact.
	 *
	 * @see <a href="https://maven.apache.org/pom.html#Repository">POM
	 *      Reference: Distribution Management -> Repository</a>
	 */
	@Parameter(property = "bnd.baseline.include.distribution.management", defaultValue = "false")
	private boolean					includeDistributionManagement;

	@Parameter(property = "bnd.baseline.full.report", defaultValue = "false")
	private boolean					fullReport;

	@Parameter(property = "bnd.baseline.continue.on.error", defaultValue = "false")
	private boolean					continueOnError;

	/**
	 * The Maven coordinates of the base artifact in the format
	 * {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>}. If
	 * set, takes precedence over {@link #base}.
	 */
	@Parameter(property = "bnd.baseline.base.coordinates")
	private String					baseCoordinates;

	@Parameter(required = false)
	private Base					base;

	@Parameter(required = false, property = "bnd.baseline.diffignores")
	private List<String>			diffignores;

	@Parameter(required = false, defaultValue = "*", property = "bnd.baseline.diffpackages")
	private List<String>			diffpackages;

	@Parameter(property = "bnd.baseline.skip", defaultValue = "false")
	private boolean					skip;

	@Parameter(property = "bnd.baseline.releaseversions", defaultValue = "false")
	private boolean					releaseversions;

	@Component
	private RepositorySystem		system;

	@Parameter(property = "bnd.baseline.report.file", defaultValue = "${project.build.directory}/baseline/${project.build.finalName}.txt")
	private File					reportFile;

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
				if ( !artifactResult.isMissing() ) {
					baselineAction(artifact.getFile(), artifactResult.getArtifact()
						.getFile());
					return;
				}
			}
		} catch (RepositoryException re) {
			// fall through
		} catch (Exception e) {
			throw new MojoExecutionException("An error occurred while calculating the baseline", e);
		}
		if (failOnMissing) {
			throw new MojoFailureException("Unable to locate a previous version of the artifact");
		} else {
			logger.warn("No previous version of {} could be found to baseline against", artifact);
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
		if (baseCoordinates != null && !baseCoordinates.isBlank()) {
			base.setFromCoordinates(baseCoordinates);
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

	private void baselineAction(File bundle, File baseline) throws Exception {
		IO.mkdirs(reportFile.getParentFile());
		boolean failure = false;
		try (Processor processor = new Processor();
			Jar newer = new Jar(bundle);
			Jar older = new Jar(baseline)) {
			logger.info("Baseline bundle {} against baseline {}", bundle, baseline);
			DiffPluginImpl differ = new DiffPluginImpl();
			differ.setIgnore(new Parameters(Strings.join(diffignores), processor));
			Baseline baseliner = new Baseline(processor, differ);
			List<Info> infos = baseliner
				.baseline(newer, older, new Instructions(new Parameters(Strings.join(diffpackages), processor)))
				.stream()
				.sorted(Comparator.comparing(info -> info.packageName))
				.toList();
			BundleInfo bundleInfo = baseliner.getBundleInfo();
			try (Formatter f = new Formatter(reportFile, "UTF-8", Locale.US)) {
				String format = "%s %-50s %-10s %-10s %-10s %-10s %-10s %s\n";
				f.format("===============================================================\n");
				f.format(format, " ", "Name", "Type", "Delta", "New", "Old", "Suggest", "");
				Diff diff = baseliner.getDiff();
				f.format(format, bundleInfo.mismatch ? "*" : " ", bundleInfo.bsn, diff.getType(), diff.getDelta(),
					newer.getVersion(), older.getVersion(),
					bundleInfo.mismatch && Objects.nonNull(bundleInfo.suggestedVersion) ? bundleInfo.suggestedVersion
						: "-",
					"");
				if (fullReport || bundleInfo.mismatch) {
					f.format("%#2S\n", diff);
				}
				if (bundleInfo.mismatch) {
					failure = true;
				}

				if (!infos.isEmpty()) {
					f.format("===============================================================\n");
					f.format(format, " ", "Name", "Type", "Delta", "New", "Old", "Suggest", "If Prov.");
					for (Info info : infos) {
						diff = info.packageDiff;
						f.format(format, info.mismatch ? "*" : " ", diff.getName(), diff.getType(), diff.getDelta(),
							info.newerVersion,
							Objects.nonNull(info.olderVersion)
								&& info.olderVersion.equals(aQute.bnd.version.Version.LOWEST) ? "-" : info.olderVersion,
							Objects.nonNull(info.suggestedVersion)
								&& info.suggestedVersion.compareTo(info.newerVersion) <= 0 ? "ok"
									: info.suggestedVersion,
							Objects.nonNull(info.suggestedIfProviders) ? info.suggestedIfProviders : "-");
						if (fullReport || info.mismatch) {
							f.format("%#2S\n", diff);
						}
						if (info.mismatch) {
							failure = true;
						}
					}
				}
			}
		}

		if (failure) {
			String msg = String.format("Baseline problems detected. See the report in %s.\n%s", reportFile,
				IO.collect(reportFile));
			if (continueOnError) {
				logger.warn(msg);
			} else {
				throw new MojoFailureException(msg);
			}
		} else {
			logger.info("Baseline check succeeded. See the report in {}.", reportFile);
		}
	}
}
