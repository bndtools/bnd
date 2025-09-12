package aQute.bnd.main;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.main.bnd.PerProject;
import aQute.bnd.main.bnd.buildoptions;
import aQute.bnd.main.bnd.devOptions;
import aQute.libg.forker.Forker;

/**
 * Container for build related commands.
 */
public class BuildCommands {

	private bnd bnd;

	public BuildCommands(bnd bnd) {
		this.bnd = bnd;
	}

	public void _build(buildoptions opts) throws Exception {
		if (opts.watch()) {
			// continous build (for Live Coding)
			Executor executor = Executors.newCachedThreadPool();
			ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

			List<Project> projects = bnd.getFilteredProjects(opts);
			buildAndWatch(opts.test(), opts.verbose(), opts.force(), executor, scheduler, projects);
			return;
		}

		// default build
		boolean force = opts.force();
		if (opts.parallel()) {
			boolean test = opts.test();
			long syncms = opts.synctime() <= 0 ? 20000 : opts.synctime();
			bnd.out.format("Build parallel%n");
			List<Project> projects = bnd.getFilteredProjects(opts);
			buildParallelInternal(projects, force, test, syncms);
		} else {
			bnd.perProject(opts, p -> {
				p.getGenerate()
					.generate(force);
				p.compile(opts.test());
				p.build(opts.test());
			});
		}

	}

	public void _dev(devOptions opts) throws Exception {

		Workspace ws = bnd.getWorkspace(bnd.getBase());
		if (ws == null) {
			bnd.error("No workspace found from %s", bnd.getBase());
			return;
		}

		List<Project> runs = bnd.getRuns(opts._arguments(), opts.project());
		if (runs == null || runs.isEmpty()) {
			bnd.messages.NoProject();
			return;
		}

		ExecutorService buildExecutor = Executors.newFixedThreadPool(2);
		ExecutorService watchExecutor = Executors.newFixedThreadPool(2);
		ScheduledExecutorService buildScheduler = Executors.newSingleThreadScheduledExecutor();
		ExecutorService runExecutor = Executors.newFixedThreadPool(runs.size());

		boolean force = opts.force();
		Collection<Project> projects = ws.getAllProjects();
		buildFullIfNeeded(opts, projects, force);

		buildExecutor.submit(() -> {
			try {
				buildAndWatch(opts.test(), opts.verbose(), force, watchExecutor, buildScheduler, projects);
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		});

		runs.forEach(run -> {
			buildExecutor.submit(() -> {
				try {
					bnd.doRun(run, opts.verify());
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			});
		});

		// Wait for tasks to complete
		buildExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

	}

	private void buildFullIfNeeded(devOptions opts, Collection<Project> projects, boolean force)
		throws Exception, InterruptedException {
		Set<Project> projectsNeedRebuild = new LinkedHashSet<>();
		for (Project project : projects) {
			// TODO there is project.isStale() but it seems true too often.
			// so we just check if there are built files which reduces the
			// number of rebuilts
			File[] files = project.getBuildFiles(false);
			if (files == null) {
				projectsNeedRebuild.add(project);
			}
		}

		if (!projectsNeedRebuild.isEmpty()) {

			bnd.out.format("Stale projects detected (%s of %s). Doing a full build now. [%s]%n",
				projectsNeedRebuild.size(),
				projects.size(), projectsNeedRebuild);

			if (opts.parallel()) {
				boolean test = opts.test();
				long syncms = opts.synctime() <= 0 ? 20000 : opts.synctime();
				bnd.out.format("Build parallel%n");
				buildParallelInternal(projects, force, test, syncms);
			} else {

				Set<Project> projectsDone = new HashSet<>(projects.size());
				for (Project proj : projects) {
					bnd.perProject(proj, opts.verbose(), p -> {
						bnd.out.format("Build Project %s%n", p.getName());
						p.compile(opts.test());
						p.build(opts.test());
					}, true, projectsDone);

				}
			}

			bnd.out.format("Full build finished%n");
		}
	}

	public void buildParallelInternal(Collection<Project> projects, boolean force, boolean test, long syncms)
		throws Exception, InterruptedException {
		ExecutorService pool = Executors.newCachedThreadPool();
		final AtomicBoolean quit = new AtomicBoolean();

		try {
			final Forker<Project> forker = new Forker<>(pool);

			for (final Project proj : projects) {
				forker.doWhen(proj.getDependson(), proj, () -> {
					if (!quit.get()) {

						try {
							proj.getGenerate()
								.generate(force);
							if (!quit.get()) {
								proj.compile(test);
							}
							if (!quit.get())
								proj.build(test);

							if (!proj.isOk()) {
								quit.set(true);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					bnd.getInfo(proj, proj + ": ");
				});
			}
			bnd.err.flush();

			forker.start(syncms);
		} finally {
			pool.shutdownNow();
		}
	}

	private void buildAndWatch(boolean undertest, boolean verbose, boolean force, Executor watchExecutor,
		ScheduledExecutorService buildScheduler, Collection<Project> projects) throws InterruptedException, Exception {

		Set<Project> projectsWellDone = new HashSet<Project>();
		try (BuildWatcher bw = new BuildWatcher(projects, (p) -> {
			try {
				perProjectAnDependents(p, verbose, (p2) -> {
					p.getGenerate()
						.generate(force);
					p2.compile(undertest);
					p2.build(undertest);
				}, true, projectsWellDone);
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		}, watchExecutor, buildScheduler, bnd.out)) {
			bnd.out.format("Watching %s project(s) for changes. Press Ctrl+C to stop.", projects.size());
			new CountDownLatch(1).await();
			return;

		}
	}

	/**
	 * Builds the project and all its dependents
	 */
	private void perProjectAnDependents(Project p, boolean verbose, PerProject run, boolean manageDeps,
		final Set<Project> projectsWellDone) throws Exception {
		bnd.out.println("Build project: " + p.getName());
		run.doit(p);

		if (manageDeps) {
			final Collection<Project> projectDeps = p.getDependents(); // ordered
			if (verbose && !projectDeps.isEmpty()) {
				bnd.out.println("Project dependents for: " + p.getName());
				projectDeps.forEach(pr -> bnd.out
					.println(" + " + pr.getName() + " " + (projectsWellDone.contains(pr) ? "<handled before>" : "")));
			}

			projectDeps.removeAll(projectsWellDone);

			for (Project dep : projectDeps) {
				run.doit(dep);
				projectsWellDone.add(dep);
			}
		}

		bnd.getInfo(p, p + ": ");
	}
}
