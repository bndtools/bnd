package aQute.bnd.mcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Performs bounded HTTP checks against loopback services only. */
public final class LocalHttp {
	private static final int DEFAULT_TIMEOUT_MILLIS = 5_000;
	private static final int RETRY_DELAY_MILLIS = 100;

	public record GetSpec(
		@Param(value = "Loopback HTTP URL", required = true) String url,
		@Param("Expected HTTP status; defaults to 200") int expectStatus,
		@Param("Expected substring in the response body") String expectContains,
		@Param("Total retry timeout in milliseconds; defaults to 5000") int timeoutMillis) {}

	public record GetResult(String url, int status, String body, int attempts, Diagnostics diagnostics)
		implements Diagnostics.Carrier {}

	public GetResult get(GetSpec spec) {
		URI uri;
		try {
			uri = URI.create(spec.url());
			if (!"http".equalsIgnoreCase(uri.getScheme()) || !isLoopback(uri.getHost()) || uri.getUserInfo() != null) {
				return failed(spec.url(), "Only HTTP URLs with a loopback host are allowed");
			}
		} catch (Exception e) {
			return failed(spec.url(), "Invalid URL: " + e.getMessage());
		}

		int timeout = spec.timeoutMillis() > 0 ? spec.timeoutMillis() : DEFAULT_TIMEOUT_MILLIS;
		int expectedStatus = spec.expectStatus() > 0 ? spec.expectStatus() : 200;
		long deadline = System.nanoTime() + Duration.ofMillis(timeout).toNanos();
		int attempts = 0;
		int status = 0;
		String body = "";
		String lastFailure = "No response";
		HttpClient client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofMillis(Math.min(timeout, 2_000)))
			.build();
		while (System.nanoTime() < deadline) {
			attempts++;
			try {
				long remainingMillis = Math.max(1, Duration.ofNanos(deadline - System.nanoTime()).toMillis());
				HttpRequest request = HttpRequest.newBuilder(uri)
					.timeout(Duration.ofMillis(remainingMillis))
					.GET()
					.build();
				HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
				status = response.statusCode();
				body = response.body();
				boolean statusMatches = status == expectedStatus;
				boolean bodyMatches = spec.expectContains() == null || spec.expectContains().isEmpty()
					|| body.contains(spec.expectContains());
				if (statusMatches && bodyMatches) {
					return new GetResult(spec.url(), status, body, attempts, Diagnostics.none());
				}
				lastFailure = "Expected status " + expectedStatus + " and body to contain '" + spec.expectContains()
					+ "', received status " + status;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return new GetResult(spec.url(), status, body, attempts, Diagnostics.error("HTTP check interrupted"));
			} catch (IOException e) {
				lastFailure = e.getMessage();
			}
			try {
				Thread.sleep(Math.min(RETRY_DELAY_MILLIS,
					Math.max(1, Duration.ofNanos(deadline - System.nanoTime()).toMillis())));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return new GetResult(spec.url(), status, body, attempts, Diagnostics.error("HTTP check interrupted"));
			}
		}
		return new GetResult(spec.url(), status, body, attempts,
			Diagnostics.error("HTTP check timed out after %d ms: %s", timeout, lastFailure));
	}

	private static boolean isLoopback(String host) {
		if (host == null) {
			return false;
		}
		if ("localhost".equalsIgnoreCase(host)) {
			return true;
		}
		if (!host.matches("[0-9.]+") && !host.contains(":")) {
			return false;
		}
		try {
			return InetAddress.getByName(host.replace("[", "")
				.replace("]", ""))
				.isLoopbackAddress();
		} catch (Exception e) {
			return false;
		}
	}

	private static GetResult failed(String url, String error) {
		return new GetResult(url, 0, "", 0, Diagnostics.error("%s", error));
	}
}