package aQute.bnd.lsp.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.ExecuteCommandParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import aQute.bnd.lsp.Constants;

@SuppressWarnings("unchecked")
class BndWorkspaceServiceTest {
	@TempDir
	Path temporary;

	private Path workspace(String buildBnd, String projectBnd) throws Exception {
		Files.createDirectories(temporary.resolve("cnf"));
		Files.writeString(temporary.resolve("cnf/build.bnd"), buildBnd);
		Path project = Files.createDirectories(temporary.resolve("p"));
		Files.writeString(project.resolve("bnd.bnd"), projectBnd);
		return project;
	}

	private Map<String, Object> execute(String command, Object... args) throws Exception {
		BndWorkspaceService service = new BndWorkspaceService(new BndWorkspaceManager());
		try {
			return (Map<String, Object>) service.executeCommand(new ExecuteCommandParams(command, List.of(args)))
				.get();
		} finally {
			service.shutdown();
		}
	}

	@Test
	void cleanRemovesProjectOutput() throws Exception {
		Path project = workspace("", "Bundle-Version: 1.0.0\n");
		Path stale = Files.createDirectories(project.resolve("generated"))
			.resolve("p.jar");
		Files.writeString(stale, "stale");
		Path binStale = Files.createDirectories(project.resolve("bin"))
			.resolve("Stale.class");
		Files.writeString(binStale, "stale");

		Map<String, Object> result = execute(Constants.COMMAND_BUILD_CLEAN, project.resolve("bnd.bnd")
			.toUri()
			.toString());

		assertThat(result.get("success")).isEqualTo(true);
		assertThat((List<String>) result.get("cleanedProjects")).containsExactly("p");
		assertThat(stale).doesNotExist();
		assertThat(binStale).doesNotExist();
	}

	@Test
	void cleanOnWorkspaceFileCleansAllProjects() throws Exception {
		Path project = workspace("", "Bundle-Version: 1.0.0\n");
		Path stale = Files.createDirectories(project.resolve("generated"))
			.resolve("p.jar");
		Files.writeString(stale, "stale");

		Map<String, Object> result = execute(Constants.COMMAND_BUILD_CLEAN, temporary.resolve("cnf/build.bnd")
			.toUri()
			.toString());

		assertThat((List<String>) result.get("cleanedProjects")).containsExactly("p");
		assertThat(stale).doesNotExist();
	}

	@Test
	void baselineWithoutBaselineJarReportsReason() throws Exception {
		Path project = workspace("", "Bundle-Version: 1.0.0\n");

		Map<String, Object> result = execute(Constants.COMMAND_BASELINE, project.resolve("bnd.bnd")
			.toUri()
			.toString());

		List<Map<String, Object>> bundles = (List<Map<String, Object>>) result.get("bundles");
		assertThat(bundles).hasSize(1);
		assertThat(bundles.get(0)).containsEntry("bsn", "p")
			.containsEntry("baselined", false);
		assertThat(bundles.get(0)
			.get("reason")
			.toString()).contains("-baseline");
	}

	@Test
	void baselineComparesAgainstRepositoryJar() throws Exception {
		Path project = workspace(
			"-plugin.release: aQute.lib.deployer.FileRepo;name=Release;location=${workspace}/cnf/release\n",
			"Bundle-Version: 1.0.0\n-baseline: *\n-baselinerepo: Release\n");
		Path older = Files.createDirectories(temporary.resolve("cnf/release/p"))
			.resolve("p-1.0.0.jar");
		try (aQute.bnd.osgi.Jar jar = new aQute.bnd.osgi.Jar("p")) {
			java.util.jar.Manifest manifest = new java.util.jar.Manifest();
			manifest.getMainAttributes()
				.putValue("Manifest-Version", "1.0");
			manifest.getMainAttributes()
				.putValue("Bundle-ManifestVersion", "2");
			manifest.getMainAttributes()
				.putValue("Bundle-SymbolicName", "p");
			manifest.getMainAttributes()
				.putValue("Bundle-Version", "1.0.0");
			jar.setManifest(manifest);
			jar.write(older.toFile());
		}

		Map<String, Object> result = execute(Constants.COMMAND_BASELINE, project.resolve("bnd.bnd")
			.toUri()
			.toString());

		assertThat(result.get("errors")).asList()
			.isEmpty();
		List<Map<String, Object>> bundles = (List<Map<String, Object>>) result.get("bundles");
		assertThat(bundles).hasSize(1);
		Map<String, Object> bundle = bundles.get(0);
		assertThat(bundle).containsEntry("bsn", "p")
			.containsEntry("baselined", true)
			.containsEntry("newerVersion", "1.0.0")
			.containsEntry("olderVersion", "1.0.0");
		assertThat(bundle.get("packages")).isNotNull();
		// same version, different manifest: bnd asks for a bump
		assertThat(result.get("mismatch")).isEqualTo(bundle.get("mismatch"));
		if (Boolean.TRUE.equals(bundle.get("mismatch"))) {
			assertThat(bundle.get("suggestedVersion")).isNotNull();
		}
	}

	@Test
	void resolveDryRunDoesNotTouchFile() throws Exception {
		Path project = workspace("", "Bundle-Version: 1.0.0\n");
		Path bndrun = project.resolve("launch.bndrun");
		String text = "-runfw: org.missing.framework\n-runrequires: osgi.identity;filter:='(osgi.identity=missing)'\n";
		Files.writeString(bndrun, text);

		Map<String, Object> result = execute(Constants.COMMAND_RESOLVE_BNDRUN, bndrun.toUri()
			.toString(), false);

		assertThat(result).doesNotContainEntry("written", true);
		assertThat(Files.readString(bndrun)).isEqualTo(text);
	}
}
