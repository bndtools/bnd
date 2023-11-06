package biz.aQute.bnd.lsp.provider;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
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

        // Create the language server instance
        BndLanguageServer server = new BndLanguageServer();

        // Connect the language server to the client's input and output
        Launcher<LanguageServer> launcher = Launcher.createLauncher(server, LanguageServer.class, in, out);

        // Start listening for incoming requests
        Future<?> startListening = launcher.startListening();

        // Keep the server running
        try {
            startListening.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<Object> shutdown() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void exit() {
		// TODO Auto-generated method stub

	}

	@Override
	public TextDocumentService getTextDocumentService() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WorkspaceService getWorkspaceService() {
		// TODO Auto-generated method stub
		return null;
	}
}
