package aQute.bnd.lsp.services;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.ProjectTester;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.lib.io.IO;

/**
 * Prepares OSGi launches for an external debugger and keeps the prepared
 * launchers alive until the client disposes them.
 */
public class BndLaunchService {
	private static final Logger		logger	= LoggerFactory.getLogger(BndLaunchService.class);

	public enum Kind {
		run,
		test
	}

	public record Request(File file, Kind kind, List<String> tests, boolean build) {}

	public record Result(String launchId, String mainClass, List<String> classPaths, List<String> vmArgs,
		List<String> args, Map<String, String> env, String cwd, String javaExecutable, String runee, String name,
		List<String> warnings, List<String> sourcePaths) {}

	public static class LaunchException extends Exception {
		private static final long	serialVersionUID	= 1L;
		private final List<String>	errors;

		public LaunchException(List<String> errors) {
			super(String.join("\n", errors));
			this.errors = List.copyOf(errors);
		}

		public List<String> getErrors() {
			return errors;
		}
	}

	private record Session(Project project, ProjectLauncher launcher, boolean ownsProject) {}

	private final BndWorkspaceManager		workspaceManager;
	private final Map<String, Session>		sessions	= new ConcurrentHashMap<>();

	public BndLaunchService(BndWorkspaceManager workspaceManager) {
		this.workspaceManager = workspaceManager;
	}

	public Result prepare(Request request) throws Exception {
		File file = request.file();
		if (file == null || !file.isFile()) {
			throw new IllegalArgumentException("Expected an existing .bndrun or bnd.bnd file: " + file);
		}
		Workspace ws = workspaceManager.getWorkspaceForFile(file);
		if (ws == null) {
			throw new IllegalStateException("Not in a bnd workspace: " + file);
		}

		boolean bndrun = file.getName()
			.endsWith(".bndrun");
		Project project;
		if (bndrun) {
			project = Run.createRun(ws, file);
		} else if (file.getName()
			.equals(Project.BNDFILE)) {
			project = workspaceManager.getProjectForFile(file);
			if (project == null) {
				throw new IllegalStateException("Could not find project for: " + file);
			}
			project.refresh();
			project.clear();
		} else {
			throw new IllegalArgumentException("Expected a .bndrun or bnd.bnd file: " + file);
		}

		ProjectLauncher launcher = null;
		try {
			if (request.build()) {
				build(project, bndrun);
			}
			if (request.kind() == Kind.test) {
				ProjectTester tester = project.getProjectTester();
				for (String test : request.tests()) {
					tester.addTest(test);
				}
				launcher = tester.getProjectLauncher();
				tester.prepare();
			} else {
				launcher = project.getProjectLauncher();
				launcher.prepare();
			}

			List<String> errors = new ArrayList<>(project.getErrors());
			errors.addAll(launcher.getErrors());
			if (!errors.isEmpty()) {
				throw new LaunchException(errors);
			}

			String id = UUID.randomUUID()
				.toString();
			Result result = describe(id, project, launcher);
			sessions.put(id, new Session(project, launcher, bndrun));
			return result;
		} catch (Exception e) {
			close(project, launcher, bndrun);
			throw e;
		}
	}

	static Result describe(String id, Project project, ProjectLauncher launcher) throws Exception {
		List<String> warnings = new ArrayList<>(project.getWarnings());
		warnings.addAll(launcher.getWarnings());
		if (launcher.getRunJdb() != null) {
			warnings.add(Constants.RUNJDB + " is ignored because the VS Code debugger owns the JDWP connection.");
		}

		List<String> vmArgs = new ArrayList<>();
		if (project.is(Constants.JAVAAGENT)) {
			launcher.getCommand()
				.getArguments()
				.stream()
				.filter(arg -> arg.startsWith("-javaagent:"))
				.forEach(vmArgs::add);
		}
		vmArgs.addAll(launcher.getRunVM());

		File cwd = launcher.getCwd() != null ? launcher.getCwd() : project.getBase();
		String java = launcher.getJavaExecutable("java");
		if ("java".equals(java)) {
			// bnd default; let the client pick a runtime matching -runee
			java = null;
		}
		var sources = new LinkedHashSet<String>();
		for (Project candidate : project.getWorkspace().getAllProjects()) {
			boolean launched = candidate.getBase().equals(project.getBase()) || launcher.getRunBundles().stream()
				.anyMatch(bundle -> new File(bundle).toPath().startsWith(candidate.getBase().toPath()));
			if (launched) {
				candidate.getSourcePath().stream().filter(File::isDirectory).map(IO::absolutePath).forEach(sources::add);
				if (candidate.getTestSrc().isDirectory()) sources.add(IO.absolutePath(candidate.getTestSrc()));
			}
		}
		return new Result(id, launcher.getMainTypeName(), List.copyOf(launcher.getClasspath()), vmArgs,
			List.copyOf(launcher.getRunProgramArgs()), new LinkedHashMap<>(launcher.getRunEnv()),
			IO.absolutePath(cwd), java, project.getProperty(Constants.RUNEE), project.getName(), warnings, List.copyOf(sources));
	}

	private static void build(Project project, boolean bndrun) throws Exception {
		Collection<Project> dependencies = project.getDependson();
		for (Project dependency : dependencies) {
			dependency.build();
			if (!dependency.isOk()) {
				project.getInfo(dependency, dependency.getName() + ": ");
			}
		}
		if (!bndrun) {
			project.build();
		}
	}

	public boolean dispose(String id) {
		Session session = id == null ? null : sessions.remove(id);
		if (session == null) {
			return false;
		}
		close(session.project(), session.launcher(), session.ownsProject());
		return true;
	}

	public int size() {
		return sessions.size();
	}

	public void shutdown() {
		for (String id : List.copyOf(sessions.keySet())) {
			dispose(id);
		}
	}

	private static void close(Project project, ProjectLauncher launcher, boolean ownsProject) {
		if (launcher != null) {
			try {
				launcher.cleanup();
				launcher.close();
			} catch (Exception e) {
				logger.debug("Failed to clean up launcher for {}", project, e);
			}
		}
		if (ownsProject) {
			try {
				project.close();
			} catch (Exception e) {
				logger.debug("Failed to close {}", project, e);
			}
		}
	}
}
