package aQute.bnd.lsp;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Future;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		int port = -1;

		for (int i = 0; i < args.length; i++) {
			if ("--socket".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
				try {
					port = Integer.parseInt(args[++i]);
				} catch (NumberFormatException e) {
					System.err.println("Invalid port number: " + args[i]);
					System.exit(1);
				}
			}
		}

		BndLanguageServer server = new BndLanguageServer();

		try {
			if (port > 0) {
				logger.info("Starting bnd Language Server on socket port {}", port);
				try (ServerSocket serverSocket = new ServerSocket(port)) {
					Socket socket = serverSocket.accept();
					startServer(server, socket.getInputStream(), socket.getOutputStream());
				}
			} else {
				logger.info("Starting bnd Language Server on stdio");
				startServer(server, System.in, System.out);
			}
		} catch (Exception e) {
			logger.error("bnd Language Server terminated with error", e);
			System.exit(1);
		}
	}

	private static void startServer(BndLanguageServer server, InputStream in, OutputStream out) throws Exception {
		Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, in, out);
		server.connect(launcher.getRemoteProxy());
		Future<Void> listening = launcher.startListening();
		listening.get();
	}
}
