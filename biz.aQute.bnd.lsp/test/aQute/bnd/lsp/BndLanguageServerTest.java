package aQute.bnd.lsp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.TextDocumentItem;
import aQute.bnd.lsp.services.BndEffectivePropertiesService;

public class BndLanguageServerTest {
	@TempDir
	Path temporary;

	@Test
	public void documentSymbolsUseSourceRanges() throws Exception {
		String text = "Bundle-Version: 1\r\n  Bundle-Name:   \r\n-buildpath: \\\r\n  example.long.name,\\\r\n  x";
		String uri = temporary.resolve("bnd.bnd").toUri().toString();
		BndLanguageServer server = new BndLanguageServer();
		try {
			server.initialize(new InitializeParams()).get();
			server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
				new TextDocumentItem(uri, "bnd", 1, text)));
			var symbols = server.getTextDocumentService().documentSymbol(new org.eclipse.lsp4j.DocumentSymbolParams(
				new org.eclipse.lsp4j.TextDocumentIdentifier(uri))).get();
			assertThat(symbols).hasSize(3);
			var version = symbols.get(0).getRight();
			assertThat(version.getRange()).isEqualTo(new org.eclipse.lsp4j.Range(
				new org.eclipse.lsp4j.Position(0, 0), new org.eclipse.lsp4j.Position(0, 17)));
			var name = symbols.get(1).getRight();
			assertThat(name.getSelectionRange()).isEqualTo(new org.eclipse.lsp4j.Range(
				new org.eclipse.lsp4j.Position(1, 2), new org.eclipse.lsp4j.Position(1, 13)));
			assertThat(name.getRange().getEnd()).isEqualTo(new org.eclipse.lsp4j.Position(1, 17));
			var buildpath = symbols.get(2).getRight();
			assertThat(buildpath.getRange().getEnd()).isEqualTo(new org.eclipse.lsp4j.Position(4, 3));
		} finally {
			server.shutdown().get();
		}
	}

	@Test
	public void effectivePropertiesRequiresTrustAndUsesVersionedText() throws Exception {
		Path file = temporary.resolve("launch.bndrun");
		Files.writeString(file, "value: saved\n");
		String uri = file.toUri().toString();
		BndLanguageServer server = new BndLanguageServer();
		try {
			InitializeParams params = new InitializeParams();
			assertThat(server.initialize(params).get().getCapabilities().getExecuteCommandProvider().getCommands())
				.contains(Constants.COMMAND_EFFECTIVE_PROPERTIES);
			ExecuteCommandParams request = new ExecuteCommandParams(Constants.COMMAND_EFFECTIVE_PROPERTIES,
				List.of(Map.of("uri", uri, "documentVersion", 3)));
			assertThat(server.getWorkspaceService().executeCommand(request).get().toString()).contains("trusted");
			params.setInitializationOptions(Map.of("workspaceTrusted", true));
			server.initialize(params).get();
			server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
				new TextDocumentItem(uri, "bnd", 3, "value: unsaved\n")));
			var result = (BndEffectivePropertiesService.Result) server.getWorkspaceService().executeCommand(request).get();
			assertThat(result.documentVersion()).isEqualTo(3);
			assertThat(result.rows()).anySatisfy(row -> {
				assertThat(row.key()).isEqualTo("value");
				assertThat(row.value()).isEqualTo("unsaved");
			});
			request.setArguments(List.of(Map.of("uri", uri, "documentVersion", 4)));
			assertThat(server.getWorkspaceService().executeCommand(request).get().toString()).contains("staleDocument");
		} finally {
			server.shutdown().get();
		}
	}

	@Test
	public void resolutionAnalysisReturnsStructuredRequirementsAndCapabilities() throws Exception {
		Path consumer = temporary.resolve("consumer.jar");
		Path provider = temporary.resolve("provider.jar");
		writeBundle(consumer, "consumer", "Import-Package", "demo.api;version=\"[1.0.0,2.0.0)\"");
		writeBundle(provider, "provider", "Export-Package", "demo.api;version=\"1.5.0\"");

		BndLanguageServer server = new BndLanguageServer();
		try {
			InitializeParams params = new InitializeParams();
			params.setInitializationOptions(Map.of("workspaceTrusted", true));
			assertThat(server.initialize(params).get().getCapabilities().getExecuteCommandProvider().getCommands())
				.contains(Constants.COMMAND_RESOLUTION_ANALYZE);
			ExecuteCommandParams request = new ExecuteCommandParams(Constants.COMMAND_RESOLUTION_ANALYZE,
				List.of(Map.of("uris", List.of(consumer.toUri().toString(), provider.toUri().toString()))));
			@SuppressWarnings("unchecked")
			Map<String, Object> result = (Map<String, Object>) server.getWorkspaceService().executeCommand(request).get();
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> requirements = (List<Map<String, Object>>) result.get("requirements");
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> capabilities = (List<Map<String, Object>>) result.get("capabilities");
			assertThat(requirements).anySatisfy(row -> {
				assertThat(row.get("namespace")).isEqualTo("osgi.wiring.package");
				assertThat(row.get("resolved")).isEqualTo(true);
			});
			assertThat(capabilities).anySatisfy(row -> assertThat(row.get("namespace")).isEqualTo("osgi.wiring.package"));
		} finally {
			server.shutdown().get();
		}
	}

	private void writeBundle(Path file, String bsn, String header, String value) throws Exception {
		try (aQute.bnd.osgi.Jar jar = new aQute.bnd.osgi.Jar(bsn)) {
			java.util.jar.Manifest manifest = new java.util.jar.Manifest();
			manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
			manifest.getMainAttributes().putValue("Bundle-ManifestVersion", "2");
			manifest.getMainAttributes().putValue("Bundle-SymbolicName", bsn);
			manifest.getMainAttributes().putValue(header, value);
			jar.setManifest(manifest);
			jar.write(file.toFile());
		}
	}

	@Test
	public void jarPrintTextReportsAllSections() throws Exception {
		Path file = temporary.resolve("sample.jar");
		try (aQute.bnd.osgi.Jar jar = new aQute.bnd.osgi.Jar("sample")) {
			jar.putResource("sample/readme.txt", new aQute.bnd.osgi.EmbeddedResource("hello", 0L));
			java.util.jar.Manifest manifest = new java.util.jar.Manifest();
			manifest.getMainAttributes()
				.putValue("Manifest-Version", "1.0");
			manifest.getMainAttributes()
				.putValue("Bundle-SymbolicName", "sample");
			jar.setManifest(manifest);
			jar.write(file.toFile());
		}
		BndLanguageServer server = new BndLanguageServer();
		try {
			assertThat(server.initialize(new InitializeParams()).get().getCapabilities().getExecuteCommandProvider().getCommands())
				.contains(Constants.COMMAND_JAR_PRINT_TEXT);
			ExecuteCommandParams request = new ExecuteCommandParams(Constants.COMMAND_JAR_PRINT_TEXT,
				List.of(Map.of("uri", file.toUri().toString())));
			assertThat(server.getWorkspaceService().executeCommand(request).get().toString())
				.contains("[MANIFEST]", "Bundle-SymbolicName", "[LIST]", "readme.txt");
		} finally {
			server.shutdown().get();
		}
	}

	@Test
	public void testInitialize() throws Exception {
		BndLanguageServer server = new BndLanguageServer();

		InitializeParams params = new InitializeParams();
		CompletableFuture<InitializeResult> initFuture = server.initialize(params);
		InitializeResult result = initFuture.get();

		assertThat(result).isNotNull();
		ServerCapabilities capabilities = result.getCapabilities();
		assertThat(capabilities).isNotNull();
		assertThat(capabilities.getHoverProvider().getLeft()).isTrue();
		assertThat(capabilities.getCompletionProvider()).isNotNull();
		assertThat(capabilities.getSemanticTokensProvider()).isNotNull();
		assertThat(capabilities.getDocumentSymbolProvider().getLeft()).isTrue();
		assertThat(capabilities.getExecuteCommandProvider().getCommands()).contains(Constants.COMMAND_RESOLVE_BNDRUN);

		server.shutdown().get();
	}
}
