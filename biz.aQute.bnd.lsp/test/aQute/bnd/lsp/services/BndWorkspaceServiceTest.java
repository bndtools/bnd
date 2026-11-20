package aQute.bnd.lsp.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.ExecuteCommandParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import aQute.bnd.lsp.Constants;
import aQute.libg.cryptography.SHA1;
import aQute.libg.filelock.ProcessLock;

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

	private Map<String, Object> executeTrusted(String command, Object... args) throws Exception {
		BndWorkspaceService service = new BndWorkspaceService(new BndWorkspaceManager());
		service.configureEffectiveProperties(null, true);
		try {
			return (Map<String, Object>) service.executeCommand(new ExecuteCommandParams(command, List.of(args)))
				.get();
		} finally {
			service.shutdown();
		}
	}

	@Test
	void everyAdvertisedCommandIsDispatched() throws Exception {
		workspace("", "Bundle-Version: 1.0.0\n");
		var server = new aQute.bnd.lsp.BndLanguageServer();
		List<String> commands = server.initialize(new org.eclipse.lsp4j.InitializeParams())
			.get()
			.getCapabilities()
			.getExecuteCommandProvider()
			.getCommands();
		assertThat(commands).contains(Constants.COMMAND_EXPORT);
		BndWorkspaceService service = new BndWorkspaceService(new BndWorkspaceManager());
		try {
			for (String command : commands) {
				// unknown commands return null; known ones a result or an error
				assertThat(service.executeCommand(new ExecuteCommandParams(command, List.of()))
					.get()).as(command)
					.isNotNull();
			}
		} finally {
			service.shutdown();
			server.shutdown();
		}
	}

	@Test
	void buildProjectAndWorkspaceReportBuiltOutput() throws Exception {
		Path project = workspace("", "Bundle-Version: 1.0.0\n-includeresource: a.txt;literal=a\n");
		String uri = project.resolve("bnd.bnd")
			.toUri()
			.toString();

		Map<String, Object> built = execute(Constants.COMMAND_BUILD_PROJECT, uri);
		assertThat(built.get("success")).isEqualTo(true);
		assertThat((List<String>) built.get("builtFiles")).singleElement()
			.asString()
			.endsWith("p.jar");

		Map<String, Object> workspace = execute(Constants.COMMAND_BUILD_WORKSPACE, uri);
		assertThat((List<String>) workspace.get("builtProjects")).containsExactly("p");
	}

	@Test
	void repoListAndJarPrintDescribeContent() throws Exception {
		Path project = workspace(
			"-plugin.release: aQute.lib.deployer.FileRepo;name=Release;location=${workspace}/cnf/release\n",
			"Bundle-SymbolicName: p\nBundle-Version: 1.0.0\n-includeresource: a.txt;literal=a\n");
		execute(Constants.COMMAND_BUILD_PROJECT, project.resolve("bnd.bnd")
			.toUri()
			.toString());

		Map<String, Object> repos = execute(Constants.COMMAND_REPO_LIST, project.resolve("bnd.bnd")
			.toUri()
			.toString());
		assertThat((List<Map<String, Object>>) repos.get("repositories")).extracting(r -> r.get("name"))
			.contains("Release");

		Map<String, Object> printed = execute(Constants.COMMAND_JAR_PRINT, project.resolve("generated/p.jar")
			.toString());
		assertThat((Map<String, String>) printed.get("manifest")).containsEntry("Bundle-SymbolicName", "p")
			.containsEntry("Bundle-Version", "1.0.0");
	}

	@Test
	void exportWritesRunbundlesFolder() throws Exception {
		Path project = workspace("",
			"Bundle-SymbolicName: p\nBundle-Version: 1.0.0\n-includeresource: a.txt;literal=a\n");
		Path bndrun = Files.writeString(project.resolve("app.bndrun"), "-runbundles: p;version=snapshot\n");

		Map<String, Object> result = executeTrusted(Constants.COMMAND_EXPORT,
			Map.of("uri", bndrun.toUri()
				.toString(), "exporter", "bnd.runbundles"));

		assertThat(result).as("%s", result)
			.containsEntry("success", true)
			.containsEntry("exporter", "bnd.runbundles");
		Path folder = project.resolve("generated/export/app.bndrun");
		assertThat(Path.of((String) result.get("output"))).isEqualTo(folder);
		assertThat(folder.resolve("p.jar")).isRegularFile();
	}

	@Test
	void exportWritesExecutableJar() throws Exception {
		Path project = workspace("",
			"Bundle-SymbolicName: p\nBundle-Version: 1.0.0\n-includeresource: a.txt;literal=a\n");
		Path bndrun = Files.writeString(project.resolve("app.bndrun"), "-runbundles: p;version=snapshot\n");

		Map<String, Object> result = executeTrusted(Constants.COMMAND_EXPORT, Map.of("uri", bndrun.toUri()
			.toString()));

		assertThat(result).as("%s", result)
			.containsEntry("success", true)
			.containsEntry("exporter", "bnd.executablejar");
		Path jar = Path.of((String) result.get("output"));
		assertThat(jar).isRegularFile()
			.hasParent(project.resolve("generated/export"));
		try (var exported = new aQute.bnd.osgi.Jar(jar.toFile())) {
			assertThat(exported.getManifest()
				.getMainAttributes()
				.getValue("Main-Class")).isNotBlank();
		}
	}

	@Test
	void exportReportsErrorsAndRequiresTrust() throws Exception {
		Path project = workspace("", "Bundle-Version: 1.0.0\n");
		Path bndrun = Files.writeString(project.resolve("app.bndrun"), "-runbundles: p;version=snapshot\n");
		Map<String, Object> request = Map.of("uri", bndrun.toUri()
			.toString(), "exporter", "unknown.exporter", "build", false);

		assertThat(String.valueOf(execute(Constants.COMMAND_EXPORT, request).get("error"))).contains("trusted");

		Map<String, Object> result = executeTrusted(Constants.COMMAND_EXPORT, request);
		assertThat(result).containsEntry("success", false)
			.doesNotContainKey("output");
		assertThat((List<String>) result.get("errors")).anySatisfy(e -> assertThat(e).contains("unknown.exporter"));
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
	void resolveWriteWaitsForAnotherProcessThatResolvesTheSameFile() throws Exception {
		Path project = workspace("", "Bundle-Version: 1.0.0\n");
		Path bndrun = project.resolve("launch.bndrun");
		Files.writeString(bndrun,
			"-runfw: org.missing.framework\n-runrequires: osgi.identity;filter:='(osgi.identity=missing)'\n");
		// the lock of another process, for example a second window
		File lockFile = temporary.resolve("cnf/cache/locks/bndrun-" + SHA1.digest(bndrun.toRealPath()
			.toString()
			.getBytes(StandardCharsets.UTF_8))
			.asHex() + ".lock")
			.toFile();

		CompletableFuture<Map<String, Object>> resolved;
		try (ProcessLock other = ProcessLock.tryLock(lockFile)
			.orElseThrow()) {
			resolved = CompletableFuture.supplyAsync(() -> {
				try {
					return execute(Constants.COMMAND_RESOLVE_BNDRUN, bndrun.toUri()
						.toString());
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			});
			Thread.sleep(500);
			assertThat(resolved).isNotDone();

			// a dry run does not write and does not wait
			assertThat(execute(Constants.COMMAND_RESOLVE_BNDRUN, bndrun.toUri()
				.toString(), false)).doesNotContainEntry("written", true);
		}
		assertThat(resolved.get(60, TimeUnit.SECONDS)).containsKey("success");
		assertThat(lockFile).isFile();
		ProcessLock.tryLock(lockFile)
			.orElseThrow()
			.close();
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
