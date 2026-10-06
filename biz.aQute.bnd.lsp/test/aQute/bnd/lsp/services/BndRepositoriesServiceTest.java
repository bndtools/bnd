package aQute.bnd.lsp.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.InitializeParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import aQute.bnd.lsp.BndLanguageServer;
import aQute.bnd.lsp.Constants;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;

class BndRepositoriesServiceTest {
	@TempDir
	Path temporary;

	private String workspace() throws Exception {
		Files.createDirectories(temporary.resolve("cnf/local"));
		Files.writeString(temporary.resolve("cnf/build.bnd"),
			"-plugin.local: aQute.lib.deployer.FileRepo;name=Local;location=${build}/local\n");
		Path project = Files.createDirectories(temporary.resolve("p"));
		Files.writeString(project.resolve("bnd.bnd"), "Bundle-Version: 1.0.0\n");
		return temporary.toUri()
			.toString();
	}

	private File bundle(String bsn, String version) throws Exception {
		File file = temporary.resolve(bsn + "-" + version + ".jar")
			.toFile();
		try (Builder builder = new Builder()) {
			builder.setBundleSymbolicName(bsn);
			builder.setBundleVersion(version);
			builder.setProperty("Export-Package", "");
			builder.setProperty("-includeresource", "x.txt;literal=x");
			builder.setProperty("Provide-Capability", "test.ns;test.ns=" + bsn);
			try (Jar jar = builder.build()) {
				jar.write(file);
			}
		}
		return file;
	}

	private static JsonObject request(Map<String, Object> values) {
		return new Gson().toJsonTree(values)
			.getAsJsonObject();
	}

	@SuppressWarnings("unchecked")
	private static <T> T field(Object result, String key) {
		return (T) ((Map<String, Object>) result).get(key);
	}

	@Test
	void listsBrowsesAndPutsBundles() throws Exception {
		String ws = workspace();
		var service = new BndRepositoriesService(new BndWorkspaceManager());

		List<Map<String, Object>> repos = field(service.list(request(Map.of("workspace", ws))), "repositories");
		assertThat(repos.get(0)
			.get("kind")).isEqualTo(BndRepositoriesService.WORKSPACE_KIND);
		Map<String, Object> local = repos.stream()
			.filter(r -> "Local".equals(r.get("name")))
			.findFirst()
			.orElseThrow();
		int index = ((Number) local.get("index")).intValue();
		assertThat(local.get("kind")).isEqualTo(BndRepositoriesService.PLUGIN_KIND);
		assertThat(local.get("name")).isEqualTo("Local");
		assertThat(local.get("writable")).isEqualTo(true);
		assertThat(local.get("actionable")).isEqualTo(true);

		Map<String, Object> repo = Map.of("workspace", ws, "repo", index, "repoName", "Local");
		Object put = service.put(request(Map.of("workspace", ws, "repo", index, "repoName", "Local", "files",
			List.of(bundle("com.example.a", "1.2.3").toURI()
				.toString(), bundle("com.example.b", "2.0.0").toURI()
					.toString()))));
		assertThat((List<?>) field(put, "added")).hasSize(2);

		List<Map<String, Object>> bundles = field(service.bundles(request(repo)), "bundles");
		assertThat(bundles).extracting(b -> b.get("bsn"))
			.containsExactlyInAnyOrder("com.example.a", "com.example.b");
		List<Map<String, Object>> filtered = field(
			service.bundles(request(Map.of("workspace", ws, "repo", index, "repoName", "Local", "filter", "example.a"))),
			"bundles");
		assertThat(filtered).extracting(b -> b.get("bsn"))
			.containsExactly("com.example.a");

		List<Map<String, Object>> versions = field(service.versions(request(
			Map.of("workspace", ws, "repo", index, "repoName", "Local", "bsn", "com.example.a"))), "versions");
		assertThat(versions).extracting(v -> v.get("version"))
			.containsExactly("1.2.3");

		Object got = service.get(request(
			Map.of("workspace", ws, "repo", index, "repoName", "Local", "bsn", "com.example.a", "version", "1.2.3")));
		assertThat(new File((String) field(got, "file"))).isFile();

		Object downloaded = service.download(request(repo));
		assertThat((List<?>) field(downloaded, "errors")).isEmpty();

		assertThat((List<?>) field(service.actions(request(repo)), "actions")).isNotNull();
		assertThat((List<String>) field(service.refresh(request(Map.of("workspace", ws))), "refreshed")).contains("Local");
		assertThat((List<?>) field(service.search(
			request(Map.of("workspace", ws, "namespace", "osgi.wiring.package", "filter", "(osgi.wiring.package=x)"))),
			"results")).isEmpty();

		List<Map<String, Object>> workspaceBundles = field(
			service.bundles(request(Map.of("workspace", ws, "repo", 0))), "bundles");
		assertThat(workspaceBundles).anySatisfy(b -> {
			assertThat(b.get("bsn")).isEqualTo("p");
			assertThat(b.get("project")).isEqualTo("p");
		});
	}

	@Test
	void togglesOfflineAndRejectsStaleRepository() throws Exception {
		String ws = workspace();
		var service = new BndRepositoriesService(new BndWorkspaceManager());
		assertThat((Boolean) field(service.offline(request(Map.of("workspace", ws, "offline", true))), "offline"))
			.isTrue();
		assertThat((Boolean) field(service.list(request(Map.of("workspace", ws))), "offline")).isTrue();
		assertThat((Boolean) field(service.offline(request(Map.of("workspace", ws, "offline", false))), "offline"))
			.isFalse();
		assertThatThrownBy(() -> service.bundles(request(Map.of("workspace", ws, "repo", 1, "repoName", "Other"))))
			.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> service.list(request(Map.of("workspace", Files.createTempDirectory("no-bnd")
			.toUri()
			.toString())))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void requiresTrustedWorkspaceAndAdvertisesCommands() throws Exception {
		String ws = workspace();
		BndLanguageServer server = new BndLanguageServer();
		try {
			InitializeParams params = new InitializeParams();
			assertThat(server.initialize(params)
				.get()
				.getCapabilities()
				.getExecuteCommandProvider()
				.getCommands()).contains(Constants.COMMAND_REPOSITORIES_LIST, Constants.COMMAND_REPOSITORIES_PUT,
					Constants.COMMAND_WORKSPACE_OFFLINE);
			ExecuteCommandParams request = new ExecuteCommandParams(Constants.COMMAND_REPOSITORIES_LIST,
				List.of(Map.of("workspace", ws)));
			assertThat(server.getWorkspaceService()
				.executeCommand(request)
				.get()
				.toString()).contains("trusted");
			params.setInitializationOptions(Map.of("workspaceTrusted", true));
			server.initialize(params)
				.get();
			assertThat(server.getWorkspaceService()
				.executeCommand(request)
				.get()
				.toString()).contains("Local");
		} finally {
			server.shutdown()
				.get();
		}
	}
}
