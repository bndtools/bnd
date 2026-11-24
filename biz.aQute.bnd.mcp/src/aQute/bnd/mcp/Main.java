package aQute.bnd.mcp;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import aQute.bnd.wstemplates.FragmentTemplateEngine;
import io.modelcontextprotocol.server.McpSyncServer;

/**
 * Runs the bnd MCP server on stdio.
 */
public class Main {

	public static void main(String[] args) throws Exception {
		PrintStream protocol = System.out;
		// stdout carries the protocol; anything bndlib prints must not corrupt it
		System.setOut(System.err);

		CountDownLatch disconnected = new CountDownLatch(1);
		InputStream in = new FilterInputStream(System.in) {
			@Override
			public int read() throws IOException {
				int c = super.read();
				if (c < 0) {
					disconnected.countDown();
				}
				return c;
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				int n = super.read(b, off, len);
				if (n < 0) {
					disconnected.countDown();
				}
				return n;
			}
		};

		try (Workspaces workspaces = new Workspaces(List.of(URI.create(FragmentTemplateEngine.DEFAULT_INDEX)));
			Runs runs = new Runs()) {
			Runtime.getRuntime()
				.addShutdownHook(new Thread(runs::close, "bnd-mcp-shutdown"));
			McpSyncServer server = BndMcpServer.start(in, protocol, workspaces, runs);
			disconnected.await();
			server.closeGracefully();
		}
		System.exit(0);
	}
}
