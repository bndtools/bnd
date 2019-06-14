package aQute.bnd.maven.run.plugin;

import static aQute.bnd.maven.lib.resolve.BndrunContainer.report;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import aQute.bnd.build.Container;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.Workspace;
import aQute.bnd.maven.lib.configuration.Bndruns;
import aQute.bnd.maven.lib.configuration.Bundles;
import aQute.bnd.maven.lib.resolve.BndrunContainer;
import aQute.bnd.maven.lib.resolve.Operation;
import aQute.bnd.maven.lib.resolve.Scope;
import aQute.bnd.osgi.Constants;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.bnd.service.RepositoryPlugin;

import biz.aQute.resolve.Bndrun;
import biz.aQute.resolve.ResolveProcess;

import com.google.common.base.Objects;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.osgi.service.resolver.ResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mojo(name = "run", defaultPhase = LifecyclePhase.PACKAGE, requiresDirectInvocation = true, requiresProject = true, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class RunMojo extends AbstractMojo {
	private static final Logger									logger	= LoggerFactory.getLogger(RunMojo.class);

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject										project;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
	private RepositorySystemSession								repositorySession;

	@Parameter
	private File 												bndrun;

	@Parameter(required = false)
	private Bundles												bundles	= new Bundles();

	@Parameter(defaultValue = "true")
	private boolean												useMavenDependencies;

	@Parameter(defaultValue = "${session}", readonly = true)
	private MavenSession										session;

	@Parameter(defaultValue = "false")
	private boolean												resolve;

	@Parameter(defaultValue = "true")
	private boolean												reportOptional;

	@Parameter(defaultValue = "true")
	private boolean												failOnChanges;

	@Parameter(property = "bnd.run.scopes", defaultValue = "compile,runtime")
	private Set<Scope>											scopes	= new HashSet<>(
		Arrays.asList(Scope.compile, Scope.runtime));

	@Parameter(property = "bnd.run.include.dependency.management", defaultValue = "false")
	private boolean												includeDependencyManagement;

	@Parameter(defaultValue = "${project.build.directory}", readonly = true)
	private File												targetDir;

	@Component
	private RepositorySystem									system;

	@Component
	private ProjectDependenciesResolver							resolver;

	@Component
	@SuppressWarnings("deprecation")
	private org.apache.maven.artifact.factory.ArtifactFactory	artifactFactory;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (bndrun == null) {
			logger.info("Nothing to run.");
			return;
		}

		int errors = 0;

		try {
			BndrunContainer container = new BndrunContainer.Builder(project, session, repositorySession, resolver,
				artifactFactory, system).setBundles(bundles.getFiles(project.getBasedir()))
					.setIncludeDependencyManagement(includeDependencyManagement)
					.setScopes(scopes)
					.setUseMavenDependencies(useMavenDependencies)
					.build();

			Operation operation = getOperation(container);

			Bndruns bndruns = new Bndruns();
			bndruns.addFile(bndrun);
			List<File> files = bndruns.getFiles(project.getBasedir(), "*.bndrun");

			errors += container.execute(files.get(0), "run", targetDir, operation);
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}

		if (errors > 0)
			throw new MojoFailureException(errors + " errors found");
	}

	private Operation getOperation(final BndrunContainer container) {
		return (file, runName, run) -> {
			final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
			final Semaphore semaphore = new Semaphore(1);

			try (final ProjectLauncher pl = run.getProjectLauncher();
				WatchService watcher = FileSystems.getDefault().newWatchService()) {
				final Set<Path> watchedPaths = ConcurrentHashMap.newKeySet();
				Path filePath = file.toPath();
				watchedPaths.add(filePath);
				filePath.getParent().register(watcher, ENTRY_MODIFY);
				for (String runbundle : pl.getRunpath()) {
					filePath = Paths.get(runbundle);
					filePath.getParent().register(watcher, ENTRY_MODIFY);
					watchedPaths.add(filePath);
				}
				for (Container runbundle : run.getRunbundles()) {
					filePath = runbundle.getFile().toPath();
					filePath.getParent().register(watcher, ENTRY_MODIFY);
					watchedPaths.add(filePath);
				}

				ForkJoinPool.commonPool().submit(() -> {
					WatchKey key;
					for (;;) {
						try {
							key = watcher.take();
						} catch (InterruptedException x) {
							continue;
						}

						for (WatchEvent<?> event : key.pollEvents()) {
							@SuppressWarnings("unchecked")
							WatchEvent<Path> eventPath = (WatchEvent<Path>)event;
							Path dir = (Path)key.watchable();
							Path path = dir.resolve(eventPath.context());
							if (watchedPaths.contains(path) && semaphore.tryAcquire()) {
								logger.info("Detected change to {}. {}.", path, resolve ? "Resolving" : "Reloading");

								scheduledExecutor.schedule(() -> {
									try {
										if (resolve) {
											resolve(container, run, watchedPaths, watcher);
										}
										pl.update();
									} catch (Exception e) {
										logger.error(e.getMessage(), e);
									}
									finally {
										semaphore.release();
									}
								}, 600, TimeUnit.MILLISECONDS);
							}
						}

						boolean valid = key.reset();
						if (!valid) {
							logger.debug("Watch key invalid {}, quitting", key);
							break;
						}
					}
				});

				pl.setTrace(run.isTrace() || Bndrun.isTrue(run.getProperty(Constants.RUNTRACE)));

				logger.info("Launching run...");
				pl.launch();
			} finally {
				scheduledExecutor.shutdownNow();
				int errors = report(run);
				if (errors > 0) {
					return errors;
				}
			}
			return 0;
		};
	}

	void resolve(final BndrunContainer container, final Bndrun run, final Set<Path> watchedPaths, final WatchService watcher) throws Exception {
		try {
			List<Container> runbundlesBefore = new ArrayList<>(run.getRunbundles());

			resetRepo(run, container);

			String runBundles = run.resolve(failOnChanges, false);

			if (run.isOk()) {
				// this can happen when using inferred -runrequires
				if (runBundles != null) {
					run.setProperty(Constants.RUNBUNDLES, runBundles);
				}

				List<Container> runbundlesAfter = new ArrayList<>(run.getRunbundles());

				if (runbundlesBefore.equals(runbundlesAfter)) {
					logger.info("Resolved no new runbundles.");

					return;
				}

				logger.info("Resolved new runbundles.");

				runbundlesBefore.removeAll(runbundlesAfter);

				for (Container oldrunbundle : runbundlesBefore) {
					Path oldfilePath = oldrunbundle.getFile().toPath();
					watchedPaths.remove(oldfilePath);
					logger.info("Removed runbundle {}.", oldrunbundle);
				}
				for (Container runbundle : runbundlesAfter) {
					Path filePath = runbundle.getFile().toPath();
					if (!watchedPaths.contains(filePath)) {
						filePath.getParent().register(watcher, ENTRY_MODIFY);
						watchedPaths.add(filePath);
						logger.info("Added runbundle {}.", runbundle);
					}
				}
			}
		} catch (ResolutionException re) {
			logger.error(ResolveProcess.format(re, reportOptional));
		} finally {
			report(run);
		}
	}

	private void resetRepo(Bndrun run, BndrunContainer container) throws Exception {
		Workspace workspace = run.getWorkspace();
		FileSetRepository originalRepo = container.getFileSetRepository();

		workspace.getPlugins().removeIf(o -> Objects.equal(o, originalRepo));
		workspace.removeBasicPlugin(originalRepo);
		workspace.refresh();

		run.getPlugins().removeIf(o -> Objects.equal(o, originalRepo));
		run.removeBasicPlugin(originalRepo);
		run.forceRefresh();

		container.refresh();

		FileSetRepository newRepo = container.getFileSetRepository();
		workspace.addBasicPlugin(newRepo);
		run.addBasicPlugin(newRepo);

		run.setParent(container.getProcessor(workspace));
		for (RepositoryPlugin repo : workspace.getRepositories()) {
			repo.list(null);
		}

		container.setRunrequiresFromProjectArtifact(run);
		container.setEEfromBuild(run);
	}

}
