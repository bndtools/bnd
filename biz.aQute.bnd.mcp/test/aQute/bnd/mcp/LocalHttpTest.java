package aQute.bnd.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class LocalHttpTest {
	@Test
	void getsAndChecksALoopbackResponse() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
		server.createContext("/", exchange -> {
			byte[] body = "Hello from local".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			try (var output = exchange.getResponseBody()) {
				output.write(body);
			}
		});
		server.start();
		try {
			LocalHttp.GetResult result = new LocalHttp().get(new LocalHttp.GetSpec(
				"http://127.0.0.1:" + server.getAddress().getPort() + "/", 200, "local", 1_000));

			assertThat(result.diagnostics().errors()).isEmpty();
			assertThat(result.status()).isEqualTo(200);
			assertThat(result.body()).contains("Hello from local");
			assertThat(result.attempts()).isEqualTo(1);
		} finally {
			server.stop(0);
		}
	}

	@Test
	void rejectsNonLoopbackUrlsBeforeRequesting() {
		LocalHttp.GetResult result = new LocalHttp().get(new LocalHttp.GetSpec("https://example.com", 200, "", 100));

		assertThat(result.status()).isZero();
		assertThat(result.diagnostics().errors()).singleElement().asString().contains("Only HTTP URLs");
	}
}