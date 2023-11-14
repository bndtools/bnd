package biz.aQute.bnd.lsp.provider;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.SemanticTokenModifiers;
import org.eclipse.lsp4j.SemanticTokenTypes;
import org.eclipse.lsp4j.SemanticTokensLegend;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

public class BndLanguageServer implements LanguageServer {

    // Implement methods of LanguageServer interface (initialize, shutdown, getTextDocumentService, getWorkspaceService)

    public static void main(String[] args) {
        // Use System.in and System.out for communication to the client
        InputStream in = System.in;
        OutputStream out = System.out;

        Future<?> startListening = run(in, out);

        // Keep the server running
        try {
            startListening.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

	/**
	 * Run {@link BndLanguageServer} with the given in/output streams used for
	 * communication
	 *
	 * @param in the stream used for inbound communication
	 * @param out the stream used for outbound communication
	 * @return a future that returns null when the listener thread is terminated
	 */
	public static Future<?> run(InputStream in, OutputStream out) {
		// Create the language server instance
        BndLanguageServer server = new BndLanguageServer();

        // Connect the language server to the client's input and output
        Launcher<LanguageServer> launcher = Launcher.createLauncher(server, LanguageServer.class, in, out);

        // Start listening for incoming requests
        return launcher.startListening();
	}

	private BNDTextDocumentService	documentService;
	private BNDWorkspaceService		workspaceService;
	private ExecutorService			executorService;

	public BndLanguageServer() {
		executorService = Executors.newWorkStealingPool();
		documentService = new BNDTextDocumentService(executorService);
		workspaceService = new BNDWorkspaceService();
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		final InitializeResult res = new InitializeResult(new ServerCapabilities());
		res.getCapabilities()
			.setCompletionProvider(new CompletionOptions(false, List.of()));
		res.getCapabilities()
			.setTextDocumentSync(TextDocumentSyncKind.Full);
		res.getCapabilities()
			.setSemanticTokensProvider(new SemanticTokensWithRegistrationOptions(
				new SemanticTokensLegend(List.of(SemanticTokenTypes.Property, SemanticTokenTypes.Comment),
					List.of(SemanticTokenModifiers.Readonly)),
				Boolean.TRUE));
		return CompletableFuture.completedFuture(res);
	}

	@Override
	public CompletableFuture<Object> shutdown() {
		return CompletableFuture.supplyAsync(() -> {
			executorService.shutdown();
			return Boolean.TRUE;
		});
	}

	@Override
	public void exit() {
		executorService.shutdownNow();
		try {
			executorService.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread()
				.interrupt();
		}
	}

	@Override
	public TextDocumentService getTextDocumentService() {
		return documentService;
	}

	@Override
	public WorkspaceService getWorkspaceService() {
		return workspaceService;
	}
}
