package aQute.bnd.maven.resolver.plugin;

import static aQute.bnd.maven.lib.resolve.BndrunContainer.report;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import aQute.bnd.build.Container;
import aQute.bnd.header.Parameters;
import aQute.bnd.maven.lib.configuration.Bndruns;
import aQute.bnd.maven.lib.configuration.Bundles;
import aQute.bnd.maven.lib.resolve.BndrunContainer;
import aQute.bnd.maven.lib.resolve.Operation;
import aQute.bnd.maven.lib.resolve.Scope;
import aQute.bnd.osgi.BundleId;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.unmodifiable.Sets;
import aQute.bnd.version.VersionRange;
import biz.aQute.resolve.ResolutionCallback;
import biz.aQute.resolve.ResolveProcess;
import biz.aQute.resolve.RunResolution;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.resolver.ResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies the <code>-runbundles</code> for the given bndrun file(s).
 */
@Mojo(name = "verify", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class VerifierMojo extends AbstractMojo {
	private static final Logger									logger	= LoggerFactory.getLogger(VerifierMojo.class);

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject										project;

	@Parameter(defaultValue = "${settings}", readonly = true)
	private Settings											settings;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
	private RepositorySystemSession								repositorySession;

	@Parameter
	private Bndruns												bndruns	= new Bndruns();

	@Parameter
	private Bundles												bundles	= new Bundles();

	@Parameter(defaultValue = "true")
	private boolean												useMavenDependencies;

	@Parameter(defaultValue = "${project.build.directory}", readonly = true)
	private File												targetDir;

	@Parameter(defaultValue = "${session}", readonly = true)
	private MavenSession										session;

	@Parameter(property = "bnd.resolve.include.dependency.management", defaultValue = "false")
	private boolean												includeDependencyManagement;

	@Parameter(defaultValue = "true")
	private boolean												reportOptional;

	@Parameter(property = "bnd.resolve.scopes", defaultValue = "compile,runtime")
	private Set<Scope>											scopes	= Sets.of(Scope.compile, Scope.runtime);

	@Parameter(property = "bnd.resolve.skip", defaultValue = "false")
	private boolean												skip;

	/**
	 * The bndrun files will be read from this directory.
	 */
	@Parameter(defaultValue = "${project.basedir}")
	private File 												bndrunDir;

	@Component
	private RepositorySystem									system;

	@Component
	private ProjectDependenciesResolver							resolver;

	@Component
	@SuppressWarnings("deprecation")
	private org.apache.maven.artifact.factory.ArtifactFactory	artifactFactory;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			logger.debug("skip project as configured");
			return;
		}

		int errors = 0;

		try {
			List<File> bndrunFiles = bndruns.getFiles(bndrunDir, "*.bndrun");

			if (bndrunFiles.isEmpty()) {
				logger.warn(
					"No bndrun files were specified with <bndrun> or found as *.bndrun in the project. This is unexpected.");
				return;
			}

			BndrunContainer container = new BndrunContainer.Builder(project, session, repositorySession, resolver,
				artifactFactory, system).setBundles(bundles.getFiles(project.getBasedir()))
					.setIncludeDependencyManagement(includeDependencyManagement)
					.setScopes(scopes)
					.setUseMavenDependencies(useMavenDependencies)
					.build();

			Operation operation = getOperation();

			for (File runFile : bndrunFiles) {
				logger.info("Verifying {}:", runFile);
				errors += container.execute(runFile, "resolve", targetDir, operation);
			}
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}

		if (errors > 0)
			throw new MojoFailureException(errors + " errors found");
	}

	private Operation getOperation() {
		return (file, runName, run) -> {
			try {
				String originalRunRequires = run.mergeProperties(Constants.RUNREQUIRES);

				Collection<BundleId> expectedRunbundles = run.getRunbundles()
					.stream()
					.map(Container::getBundleId)
					.collect(toList());

				List<Requirement> runBundleReqs = expectedRunbundles.stream()
					.map(c -> CapReqBuilder
						.createBundleRequirement(c.getBsn(),
							new VersionRange(c.getVersion(), c.getVersion()).toString())
						.buildSyntheticRequirement())
					.collect(toList());

				run.setRunRequires(runBundleReqs.stream()
					.map(Requirement::toString)
					.collect(Collectors.joining(", ")));

				RunResolution result = run.resolve(new BundleFilter(runBundleReqs));

				if (result.isOK()) {
					List<BundleId> resolved = result.getContainers()
						.stream()
						.map(Container::getBundleId)
						.collect(toList());

					List<BundleId> missing = expectedRunbundles.stream()
						.filter(c -> !resolved.contains(c))
						.collect(toList());
					List<BundleId> extra = resolved.stream()
						.filter(c -> !expectedRunbundles.contains(c))
						.collect(toList());

					if (missing.isEmpty() && extra.isEmpty()) {

						Parameters inputRequirements = new Parameters(originalRunRequires, run);
						List<Resource> resolvedResources = result.getOrderedResources();

						List<Requirement> unmatchedInitialRequirements = CapReqBuilder
							.getRequirementsFrom(inputRequirements)
							.stream()
							.filter(req -> resolvedResources.stream()
								.noneMatch(res -> ResourceUtils.matches(req, res)))
							.collect(toList());

						if (unmatchedInitialRequirements.isEmpty()) {
							logger.info("The bndrun file {} validated successfully", file);
						} else {
							logger.error("The bndrun file {} failed validation with {} unmatched initial requirements",
								file, unmatchedInitialRequirements);
							run.error(
								"Bndrun resolution verification failed for file %s.\nThe following initial requirements were not satisfied: %s",
								file, unmatchedInitialRequirements);
						}
					} else {
						logger.error(
							"The bndrun file {} failed validation with {} missing bundles and {} extra bundles", file,
							missing, extra);
						run.error(
							"Bndrun resolution verification failed for file %s.\nThe missing results were: %s\nThe extra bundles were: %s",
							file, missing, extra);
					}
				} else {
					if (result.exception instanceof ResolutionException) {
						String msg = ResolveProcess.format((ResolutionException) result.exception, reportOptional);
						logger.error(msg);
						run.error(msg);
					} else {
						logger.error("An unknown error ocurred verifying bndrun file {}", file, result.exception);
						run.error("An unknown error ocurred verifying bndrun file %s", result.exception, file);
					}
				}
			} finally {
				int errors = report(run);
				if (errors > 0) {
					return errors;
				}
			}
			return 0;
		};
	}

	private static class BundleFilter implements ResolutionCallback {
		private final List<Requirement> bundleRequirements;

		public BundleFilter(List<Requirement> bundleRequirements) {
			this.bundleRequirements = bundleRequirements;
		}

		@Override
		public void processCandidates(Requirement requirement, Set<Capability> wired, List<Capability> candidates) {
			Iterator<Capability> it = candidates.iterator();
			while (it.hasNext()) {
				Capability id = ResourceUtils.getIdentityCapability(it.next()
					.getResource());
				if (bundleRequirements.stream()
					.noneMatch(r -> ResourceUtils.matches(requirement, id))) {
					it.remove();
				}
			}
		}
	}
}
