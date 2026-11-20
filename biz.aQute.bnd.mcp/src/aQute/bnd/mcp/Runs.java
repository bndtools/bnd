package aQute.bnd.mcp;

import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.Workspace;
import biz.aQute.resolve.Bndrun;

/** Builds, resolves, and manages live runs without exposing bnd lifecycle APIs. */
public final class Runs implements AutoCloseable {
	private static final int CONSOLE_LINES = 200;
	private static final int DEFAULT_STARTUP_TIMEOUT_MILLIS = 30_000;
	private static final int STOP_TIMEOUT_MILLIS = 5_000;

	public record BuildSpec(
		@Param(value = "Directory of the bnd workspace", required = true) String workspaceDir,
		@Param(value = "Project name", required = true) String project) {}

	public record BuildResult(String project, List<String> outputs, Diagnostics diagnostics)
		implements Diagnostics.Carrier {}

	public record ResolveSpec(
		@Param(value = "Directory of the bnd workspace", required = true) String workspaceDir,
		@Param(value = "Workspace-relative or absolute .bndrun file", required = true) String bndrun,
		@Param("Write changed -runbundles back to the .bndrun file") boolean writeChanges) {}

	public record ResolveResult(String bndrun, String runbundles, Diagnostics diagnostics)
		implements Diagnostics.Carrier {}

	public record StartSpec(
		@Param(value = "Directory of the bnd workspace", required = true) String workspaceDir,
		@Param(value = "Workspace-relative or absolute .bndrun file", required = true) String bndrun,
		@Param("Milliseconds to wait for readiness; defaults to 30000") int startupTimeoutMillis,
		@Param("Readiness check: 'http' (default) waits for the assigned HTTP port to accept connections, 'console' waits for a launcher output line matching readyPattern, 'none' returns as soon as the framework process is running") String readiness,
		@Param("Java regular expression matched against each console line when readiness is 'console'") String readyPattern) {}

	public record StopSpec(@Param(value = "Run ID returned by bnd_run_start", required = true) String runId) {}

	public record StatusSpec(@Param(value = "Run ID returned by bnd_run_start", required = true) String runId) {}

	public record RunResult(String runId, String state, int httpPort, Integer exitCode, List<String> console,
		Diagnostics diagnostics) implements Diagnostics.Carrier {}

	private final Map<String, Session> sessions = new ConcurrentHashMap<>();
	private final AtomicBoolean closed = new AtomicBoolean();

	public BuildResult build(BuildSpec spec) {
		try (Workspace workspace = new Workspace(Path.of(spec.workspaceDir()).toFile())) {
			Project project = workspace.getProject(spec.project());
			if (project == null) {
				return new BuildResult(spec.project(), List.of(), Diagnostics.error("Unknown project: %s", spec.project()));
			}
			File[] files = project.build();
			List<String> outputs = files == null ? List.of() : java.util.Arrays.stream(files)
				.map(File::getAbsolutePath)
				.toList();
			return new BuildResult(project.getName(), outputs,
				new Diagnostics(project.getErrors(), project.getWarnings()));
		} catch (Exception e) {
			return new BuildResult(spec.project(), List.of(), Diagnostics.error("Build failed: %s", e));
		}
	}

	public ResolveResult resolve(ResolveSpec spec) {
		try (Workspace workspace = new Workspace(Path.of(spec.workspaceDir()).toFile())) {
			Path file = runFile(Path.of(spec.workspaceDir()), spec.bndrun());
			try (Bndrun run = Bndrun.createBndrun(workspace, file.toFile())) {
				String bundles = run.resolve(false, spec.writeChanges());
				return new ResolveResult(file.toString(), bundles,
					new Diagnostics(run.getErrors(), run.getWarnings()));
			}
		} catch (Exception e) {
			return new ResolveResult(spec.bndrun(), "", Diagnostics.error("Resolve failed: %s", e));
		}
	}

	public RunResult start(StartSpec spec) {
		if (closed.get()) {
			return failed("", "STOPPED", 0, "Run manager is closed");
		}
		Workspace workspace = null;
		Bndrun run = null;
		ProjectLauncher launcher = null;
		try {
			Readiness readiness = Readiness.of(spec.readiness(), spec.readyPattern());
			Path root = Path.of(spec.workspaceDir()).toRealPath();
			Path file = runFile(root, spec.bndrun());
			workspace = new Workspace(root.toFile());
			run = Bndrun.createBndrun(workspace, file.toFile());
			run.resolve(false, false);
			launcher = run.getProjectLauncher();
			int port = freePort();
			launcher.getRunProperties().put("org.osgi.service.http.port", Integer.toString(port));
			Session session = new Session(UUID.randomUUID().toString(), root.toString(), file.toString(), port,
				workspace, run, launcher, readiness.pattern);
			workspace = null;
			run = null;
			launcher = null;
			sessions.put(session.id, session);
			session.start();
			int timeout = spec.startupTimeoutMillis() > 0 ? spec.startupTimeoutMillis()
				: DEFAULT_STARTUP_TIMEOUT_MILLIS;
			if (!awaitReady(readiness, session, timeout)) {
				if (session.thread.isAlive()) {
					stop(new StopSpec(session.id));
					return failed(session.id, "FAILED", port,
						"Run did not become ready (" + readiness.describe() + ") within " + timeout + " ms");
				}
				RunResult exited = status(new StatusSpec(session.id));
				return new RunResult(exited.runId(), exited.state(), exited.httpPort(), exited.exitCode(), exited.console(),
					Diagnostics.error("Run exited before becoming ready (%s; state %s, exit code %s)", readiness.describe(),
						exited.state(), exited.exitCode()));
			}
			return session.result(Diagnostics.none());
		} catch (Exception e) {
			close(launcher);
			close(run);
			close(workspace);
			return failed("", "FAILED", 0, "Run start failed: " + e);
		}
	}

	public RunResult stop(StopSpec spec) {
		Session session = sessions.get(spec.runId());
		if (session == null) {
			return new RunResult(spec.runId(), "STOPPED", 0, null, List.of(), Diagnostics.none());
		}
		if (!session.thread.isAlive()) {
			session.release();
			return session.result(Diagnostics.none());
		}
		session.stopping = true;
		try {
			session.launcher.cancel();
			session.thread.join(STOP_TIMEOUT_MILLIS);
			if (session.thread.isAlive()) {
				return session.result(Diagnostics.error("Run did not stop within %d ms", STOP_TIMEOUT_MILLIS));
			}
			session.release();
			return session.result(Diagnostics.none());
		} catch (Exception e) {
			return session.result(Diagnostics.error("Run stop failed: %s", e));
		}
	}

	public RunResult status(StatusSpec spec) {
		Session session = sessions.get(spec.runId());
		return session == null ? failed(spec.runId(), "UNKNOWN", 0, "Unknown run ID: " + spec.runId())
			: session.result(session.failure == null ? Diagnostics.none() : Diagnostics.error("%s", session.failure));
	}

	@Override
	public void close() {
		if (closed.compareAndSet(false, true)) {
			sessions.values()
				.stream()
				.map(session -> session.id)
				.toList()
				.forEach(id -> stop(new StopSpec(id)));
		}
	}

	/** Readiness strategy for {@link #start}; validated before any resources are created. */
	static final class Readiness {
		final String mode;
		final Pattern pattern;

		private Readiness(String mode, Pattern pattern) {
			this.mode = mode;
			this.pattern = pattern;
		}

		static Readiness of(String mode, String readyPattern) {
			String normalized = mode == null || mode.isBlank() ? "http" : mode.trim().toLowerCase();
			switch (normalized) {
				case "http" :
				case "none" :
					return new Readiness(normalized, null);
				case "console" :
					if (readyPattern == null || readyPattern.isBlank()) {
						throw new IllegalArgumentException("readiness 'console' requires readyPattern");
					}
					return new Readiness(normalized, Pattern.compile(readyPattern));
				default :
					throw new IllegalArgumentException(
						"Unknown readiness '" + mode + "'; expected http, console or none");
			}
		}

		String describe() {
			return switch (mode) {
				case "console" -> "console line matching " + pattern;
				case "none" -> "process running";
				default -> "HTTP port accepting connections";
			};
		}
	}

	private static boolean awaitReady(Readiness readiness, Session session, int timeoutMillis)
		throws InterruptedException {
		long deadline = System.nanoTime() + Duration.ofMillis(timeoutMillis).toNanos();
		while (System.nanoTime() < deadline && session.thread.isAlive()) {
			boolean ready = switch (readiness.mode) {
				case "none" -> "RUNNING".equals(session.state);
				case "console" -> session.console.matched();
				default -> portOpen(session.port);
			};
			if (ready) {
				return true;
			}
			Thread.sleep("none".equals(readiness.mode) ? 10 : 100);
		}
		return false;
	}

	private static boolean portOpen(int port) {
		try (Socket socket = new Socket("127.0.0.1", port)) {
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static int freePort() throws Exception {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

	private static Path runFile(Path root, String name) throws Exception {
		Path normalizedRoot = root.toAbsolutePath().normalize();
		Path file = Path.of(name);
		if (!file.isAbsolute()) {
			file = normalizedRoot.resolve(file);
		}
		file = file.toRealPath();
		if (!file.startsWith(normalizedRoot.toRealPath()) || !Files.isRegularFile(file)
			|| !file.getFileName().toString().endsWith(".bndrun")) {
			throw new IllegalArgumentException("Not a workspace .bndrun file: " + name);
		}
		return file;
	}

	private static RunResult failed(String id, String state, int port, String error) {
		return new RunResult(id, state, port, null, List.of(), Diagnostics.error("%s", error));
	}

	private static void close(AutoCloseable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (Exception ignored) {}
		}
	}

	private static final class Session {
		final String id;
		final String workspaceDir;
		final String bndrun;
		final int port;
		final Workspace workspace;
		final Bndrun run;
		final ProjectLauncher launcher;
		final RingBuffer console;
		final AtomicBoolean released = new AtomicBoolean();
		volatile String state = "STARTING";
		volatile Integer exitCode;
		volatile String failure;
		volatile boolean stopping;
		Thread thread;

		Session(String id, String workspaceDir, String bndrun, int port, Workspace workspace, Bndrun run,
			ProjectLauncher launcher, Pattern readyPattern) {
			this.id = id;
			this.workspaceDir = workspaceDir;
			this.bndrun = bndrun;
			this.port = port;
			this.workspace = workspace;
			this.run = run;
			this.launcher = launcher;
			this.console = new RingBuffer(CONSOLE_LINES, readyPattern);
			launcher.setStreams(console, console);
			launcher.registerForNotifications((type, message) -> console.add(type + ": " + message));
		}

		void start() {
			thread = new Thread(() -> {
				state = "RUNNING";
				try {
					exitCode = launcher.launch();
					state = stopping ? "STOPPED" : "EXITED";
				} catch (Exception e) {
					failure = e.toString();
					state = stopping ? "STOPPED" : "FAILED";
				} finally {
					close(launcher);
				}
			}, "bnd-mcp-run-" + id);
			thread.setDaemon(true);
			thread.start();
		}

		RunResult result(Diagnostics diagnostics) {
			return new RunResult(id, state, port, exitCode, console.snapshot(), diagnostics);
		}

		void release() {
			if (released.compareAndSet(false, true)) {
				close(run);
				close(workspace);
			}
		}
	}

	static final class RingBuffer implements Appendable {
		private final int limit;
		private final Pattern readyPattern;
		private final ArrayDeque<String> lines = new ArrayDeque<>();
		private final StringBuilder current = new StringBuilder();
		private volatile boolean matched;

		RingBuffer(int limit, Pattern readyPattern) {
			this.limit = limit;
			this.readyPattern = readyPattern;
		}

		@Override
		public synchronized Appendable append(CharSequence value) {
			return append(value, 0, value.length());
		}

		@Override
		public synchronized Appendable append(CharSequence value, int start, int end) {
			for (int i = start; i < end; i++) {
				char ch = value.charAt(i);
				if (ch == '\n') {
					add(current.toString());
					current.setLength(0);
				} else if (ch != '\r') {
					current.append(ch);
				}
			}
			return this;
		}

		@Override
		public synchronized Appendable append(char value) {
			return append(String.valueOf(value));
		}

		synchronized void add(String line) {
			if (!line.isEmpty()) {
				if (readyPattern != null && !matched && readyPattern.matcher(line).find()) {
					matched = true;
				}
				lines.addLast(line);
				while (lines.size() > limit) {
					lines.removeFirst();
				}
			}
		}

		boolean matched() {
			return matched;
		}

		synchronized List<String> snapshot() {
			ArrayDeque<String> snapshot = new ArrayDeque<>(lines);
			if (!current.isEmpty()) {
				snapshot.addLast(current.toString());
			}
			return List.copyOf(snapshot);
		}
	}
}
