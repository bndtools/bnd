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
