package aQute.bnd.lsp.services;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.lsp.models.BndBuildStatus;

/**
 * Builds bnd projects automatically after their inputs changed.
 * <p>
 * A bnd build packages the classes that the Java tooling of the editor
 * compiled, so a build is requested by changes to a project, not by the
 * compilation. Changes are collected for a short while and then built once per
 * workspace: the changed projects always, projects that depend on them only
 * when bnd finds them {@link Project#isStale() stale}. Changes to the workspace
 * configuration check every project for staleness.
 * <p>
 * Builds never run in a workspace that the client did not mark as trusted
 * because a build can execute code from the workspace.
 */
public class BndAutoBuildService {
	public static final long			DEFAULT_DELAY_MS	= 750;

	private static final Logger			logger				= LoggerFactory.getLogger(BndAutoBuildService.class);
	private static final int			MAX_MESSAGES		= 200;

	private final BndWorkspaceManager	workspaceManager;
	private final ScheduledExecutorService scheduler;
	private final long					delayMs;
	private final Map<File, Pending>	pending				= new HashMap<>();

	private volatile boolean			enabled				= true;
	private volatile boolean			trusted;
	private volatile boolean			closed;
	private volatile Consumer<BndBuildStatus> listener		= s -> {};

	/** What is waiting to be built in one workspace. */
	private static class Pending {
		final Set<String>		projects	= new LinkedHashSet<>();
		boolean					all;
		ScheduledFuture<?>		timer;
	}

	public BndAutoBuildService(BndWorkspaceManager workspaceManager, ScheduledExecutorService scheduler) {
		this(workspaceManager, scheduler, DEFAULT_DELAY_MS);
	}

	BndAutoBuildService(BndWorkspaceManager workspaceManager, ScheduledExecutorService scheduler, long delayMs) {
		this.workspaceManager = workspaceManager;
		this.scheduler = scheduler;
		this.delayMs = delayMs;
	}

	public void setListener(Consumer<BndBuildStatus> listener) {
		this.listener = listener != null ? listener : s -> {};
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isTrusted() {
		return trusted;
	}

	/** Whether changes lead to builds: switched on and a trusted workspace. */
	public boolean isActive() {
		return enabled && trusted && !closed;
	}

	/** Set the initial state without telling the client, which asks for it. */
	public void configure(boolean enabled, boolean trusted) {
		this.enabled = enabled;
		this.trusted = trusted;
		if (!isActive()) {
			cancelPending();
		}
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		if (!isActive()) {
			cancelPending();
		}
		listener.accept(status(isActive() ? BndBuildStatus.STATE_IDLE : BndBuildStatus.STATE_DISABLED));
	}

	/** The state a client can show when it asks without a build running. */
	public BndBuildStatus currentStatus() {
		return status(isActive() ? BndBuildStatus.STATE_IDLE : BndBuildStatus.STATE_DISABLED);
	}

	public void close() {
		closed = true;
		cancelPending();
	}

	/**
	 * Report files that changed. Files that cannot influence a build are
	 * ignored. Returns immediately; the work happens on the scheduler.
	 */
	public void filesChanged(Collection<File> files) {
		if (!isActive() || files.isEmpty()) {
			return;
		}
		List<File> copy = new ArrayList<>(files);
		try {
			scheduler.execute(() -> collect(copy));
		} catch (RejectedExecutionException e) {
			logger.debug("Ignoring changes after shutdown");
		}
	}

	private void collect(List<File> files) {
		Map<File, Pending> found = new HashMap<>();
		Map<File, Project> byDirectory = new HashMap<>();
		for (File file : files) {
			try {
				classify(file, found, byDirectory);
			} catch (Exception e) {
				logger.debug("Could not classify {}", file, e);
			}
		}
		if (found.isEmpty()) {
			return;
		}
		synchronized (pending) {
			if (!isActive()) {
				return;
			}
			found.forEach((root, change) -> {
				Pending target = pending.computeIfAbsent(root, r -> new Pending());
				target.projects.addAll(change.projects);
				target.all |= change.all;
				if (target.timer != null) {
					target.timer.cancel(false);
				}
				target.timer = scheduler.schedule(() -> fire(root), delayMs, TimeUnit.MILLISECONDS);
			});
		}
	}

	private void classify(File file, Map<File, Pending> found, Map<File, Project> byDirectory) throws Exception {
		Workspace ws = workspaceManager.getWorkspaceForFile(file);
		if (ws == null) {
			return;
		}
		Path path = file.getAbsoluteFile()
			.toPath()
			.normalize();

		Path cnf = ws.getBuildDir()
			.getAbsoluteFile()
			.toPath()
			.normalize();
		if (path.startsWith(cnf)) {
			if (isWorkspaceConfiguration(cnf.relativize(path))) {
				found.computeIfAbsent(ws.getBase(), r -> new Pending()).all = true;
			}
			return;
		}

		File directory = file.isDirectory() ? file : file.getAbsoluteFile()
			.getParentFile();
		Project project = byDirectory.containsKey(directory) ? byDirectory.get(directory)
			: workspaceManager.getProjectForFile(file);
		byDirectory.put(directory, project);
		if (project == null || !isProjectInput(project, path)) {
			return;
		}
		found.computeIfAbsent(ws.getBase(), r -> new Pending()).projects.add(project.getName());
	}

	/** Only the bnd files in cnf, not its caches or the local repositories. */
	private static boolean isWorkspaceConfiguration(Path relative) {
		String name = relative.getFileName()
			.toString();
		boolean config = name.endsWith(".bnd") || name.endsWith(".mvn");
		return config && (relative.getNameCount() == 1 || relative.getName(0)
			.toString()
			.equals("ext"));
	}

	/**
	 * What a build writes, hidden files and the run descriptors do not
	 * influence the bundles of a project. Ignoring the outputs also prevents
	 * a build from triggering itself.
	 */
	private static boolean isProjectInput(Project project, Path path) throws Exception {
		Path base = project.getBase()
			.getAbsoluteFile()
			.toPath()
			.normalize();
		if (!path.startsWith(base)) {
			return true;
		}
		for (Path segment : base.relativize(path)) {
			if (segment.toString()
				.startsWith(".")) {
				return false;
			}
		}
		if (path.getFileName()
			.toString()
			.endsWith(".bndrun")) {
			return false;
		}
		for (File output : new File[] {
			project.getTarget(), project.getTestOutput()
		}) {
			if (output != null && path.startsWith(output.getAbsoluteFile()
				.toPath()
				.normalize())) {
				return false;
			}
		}
		return true;
	}

	private void fire(File root) {
		Pending work;
		synchronized (pending) {
			work = pending.remove(root);
		}
		if (work == null || !isActive()) {
			return;
		}
		try {
			workspaceManager.executeWithWorkspaceLock(root, ws -> {
				if (ws != null) {
					build(ws, work);
				}
				return null;
			});
		} catch (RejectedExecutionException e) {
			logger.debug("Dropping build of {} after shutdown", root);
		}
	}

	private void build(Workspace ws, Pending work) {
		try {
			ws.refresh();
			List<Project> order = buildOrder(ws);

			Set<Project> affected = new HashSet<>();
			List<String> queue = new ArrayList<>();
			for (Project project : order) {
				if (work.all || work.projects.contains(project.getName())
					|| !Collections.disjoint(project.getDependson(), affected)) {
					affected.add(project);
					queue.add(project.getName());
				}
			}

			BndBuildStatus.Summary summary = new BndBuildStatus.Summary();
			List<BndBuildStatus.Message> messages = new ArrayList<>();
			Set<Project> failed = new HashSet<>();
			boolean started = false;

			for (Project project : order) {
				if (!isActive()) {
					break;
				}
				if (!affected.contains(project) || project.isNoBundles()) {
					continue;
				}
				boolean requested = work.projects.contains(project.getName());
				if (!requested && !project.isStale()) {
					continue;
				}
				if (!Collections.disjoint(project.getDependson(), failed)) {
					summary.skipped.add(project.getName());
					continue;
				}

				started = true;
				BndBuildStatus building = status(BndBuildStatus.STATE_BUILDING);
				building.workspace = ws.getBase()
					.toURI()
					.toString();
				building.projects = queue;
				building.current = project.getName();
				listener.accept(building);

				if (!buildProject(project, summary, messages)) {
					failed.add(project);
				}
			}

			if (started || !summary.skipped.isEmpty()) {
				BndBuildStatus done = status(isActive() ? BndBuildStatus.STATE_IDLE : BndBuildStatus.STATE_DISABLED);
				done.workspace = ws.getBase()
					.toURI()
					.toString();
				done.projects = queue;
				done.summary = summary;
				done.messages = messages.size() > MAX_MESSAGES ? messages.subList(0, MAX_MESSAGES) : messages;
				listener.accept(done);
			}
		} catch (Exception e) {
			logger.warn("Automatic build of {} failed", ws.getBase(), e);
			BndBuildStatus failure = status(BndBuildStatus.STATE_IDLE);
			failure.workspace = ws.getBase()
				.toURI()
				.toString();
			failure.summary = new BndBuildStatus.Summary();
			failure.summary.errors = 1;
			failure.messages.add(new BndBuildStatus.Message(null, "error", String.valueOf(e)));
			listener.accept(failure);
		}
	}

	private boolean buildProject(Project project, BndBuildStatus.Summary summary,
		List<BndBuildStatus.Message> messages) {
		String name = project.getName();
		try {
			project.clear();
			project.refresh();
			project.build();
		} catch (Exception e) {
			logger.debug("Build of {} threw", name, e);
			messages.add(new BndBuildStatus.Message(name, "error", String.valueOf(e)));
			summary.errors++;
			summary.failed.add(name);
			return false;
		}
		for (String error : project.getErrors()) {
			messages.add(new BndBuildStatus.Message(name, "error", error));
		}
		for (String warning : project.getWarnings()) {
			messages.add(new BndBuildStatus.Message(name, "warning", warning));
		}
		summary.errors += project.getErrors()
			.size();
		summary.warnings += project.getWarnings()
			.size();
		if (project.isOk()) {
			summary.built.add(name);
			return true;
		}
		summary.failed.add(name);
		return false;
	}

	/** Projects with the projects they depend on first, tolerating cycles. */
	private static List<Project> buildOrder(Workspace ws) throws Exception {
		Set<Project> result = new LinkedHashSet<>();
		Set<Project> visiting = new HashSet<>();
		for (Project project : ws.getAllProjects()) {
			visit(project, result, visiting);
		}
		return new ArrayList<>(result);
	}

	private static void visit(Project project, Set<Project> result, Set<Project> visiting) throws Exception {
		if (result.contains(project) || !visiting.add(project)) {
			return;
		}
		for (Project dependency : project.getDependson()) {
			visit(dependency, result, visiting);
		}
		result.add(project);
	}

	private void cancelPending() {
		synchronized (pending) {
			for (Pending p : pending.values()) {
				if (p.timer != null) {
					p.timer.cancel(false);
				}
			}
			pending.clear();
		}
	}

	private BndBuildStatus status(String state) {
		BndBuildStatus status = new BndBuildStatus();
		status.state = state;
		status.enabled = enabled;
		status.active = isActive();
		if (enabled && !trusted) {
			status.reason = BndBuildStatus.REASON_UNTRUSTED;
		}
		return status;
	}
}
