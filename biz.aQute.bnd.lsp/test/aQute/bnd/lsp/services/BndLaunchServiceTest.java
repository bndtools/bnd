package aQute.bnd.lsp.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.ExecuteCommandParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.Workspace;
import aQute.bnd.lsp.Constants;

class BndLaunchServiceTest {
	@TempDir
	Path temporary;

	static class FakeLauncher extends ProjectLauncher {
		FakeLauncher(Project project) throws Exception {
			super(project);
			updateFromProject();
		}

		@Override
		public String getMainTypeName() {
			return "test.Main";
		}
	}

	private Path workspace() throws Exception {
		Files.createDirectories(temporary.resolve("cnf"));
		Files.writeString(temporary.resolve("cnf/build.bnd"), "");
		Path project = Files.createDirectory(temporary.resolve("p"));
		Files.writeString(project.resolve("bnd.bnd"),
			"-runvm: -Xmx64m, -Dfoo=bar\n-runprogramargs: one, two\n-runenv: A=1\n-runjdb: 8000\n-runee: JavaSE-17\n");
		return project;
	}

	@Test
	void describeMapsLauncherSettings() throws Exception {
		Path dir = workspace();
		try (Workspace ws = new Workspace(temporary.toFile()); Project project = ws.getProject("p");
			FakeLauncher launcher = new FakeLauncher(project)) {
			var result = BndLaunchService.describe("id", project, launcher);
			assertThat(result.launchId()).isEqualTo("id");
			assertThat(result.mainClass()).isEqualTo("test.Main");
			assertThat(result.vmArgs()).containsExactly("-Xmx64m", "-Dfoo=bar");
			assertThat(result.args()).containsExactly("one", "two");
			assertThat(result.env()).containsEntry("A", "1");
			assertThat(result.runee()).isEqualTo("JavaSE-17");
			assertThat(result.javaExecutable()).isNull();
			assertThat(result.cwd()).isEqualTo(dir.toFile()
				.getAbsolutePath()
				.replace('\\', '/'));
			assertThat(result.warnings()).anySatisfy(w -> assertThat(w).contains("-runjdb"));
		}
	}

	@Test
	void prepareRejectsUnsupportedFiles() throws Exception {
		Path dir = workspace();
		Path other = Files.writeString(dir.resolve("other.bnd"), "");
		var service = new BndLaunchService(new BndWorkspaceManager());
		assertThatThrownBy(() -> service
			.prepare(new BndLaunchService.Request(other.toFile(), BndLaunchService.Kind.run, List.of(), false)))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> service.prepare(new BndLaunchService.Request(dir.resolve("missing.bndrun")
			.toFile(), BndLaunchService.Kind.run, List.of(), false))).isInstanceOf(IllegalArgumentException.class);
		assertThat(service.size()).isZero();
	}

	@Test
	void findsProjectForBndFile() throws Exception {
		Path dir = workspace();
		var manager = new BndWorkspaceManager();
		try {
			Project project = manager.getProjectForFile(dir.resolve("bnd.bnd")
				.toFile());
			assertThat(project).isNotNull();
			assertThat(project.getName()).isEqualTo("p");
		} finally {
			manager.shutdown();
		}
	}

	@Test
	void disposeUnknownLaunchIsSafe() {
		var service = new BndLaunchService(new BndWorkspaceManager());
		assertThat(service.dispose("unknown")).isFalse();
		assertThat(service.dispose(null)).isFalse();
	}

	@Test
	void launchRequiresTrustedWorkspace() throws Exception {
		Path dir = workspace();
		var service = new BndWorkspaceService(new BndWorkspaceManager());
		service.configureEffectiveProperties(null, false);
		Object result = service.executeCommand(new ExecuteCommandParams(Constants.COMMAND_LAUNCH_PREPARE,
			List.of(Map.of("uri", dir.resolve("bnd.bnd")
				.toUri()
				.toString()))))
			.get();
		assertThat(result).isInstanceOfSatisfying(Map.class,
			map -> assertThat(String.valueOf(map.get("error"))).contains("trusted"));
	}
}
