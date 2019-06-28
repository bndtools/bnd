package aQute.bnd.deployer.http;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import aQute.bnd.http.HttpClient;
import aQute.lib.io.IO;
import aQute.service.reporter.Reporter;
import junit.framework.TestCase;
import test.lib.NanoHTTPD;

public class HttpRedirectionTest extends TestCase {

	public void testFollowRedirect() throws Exception {
		Reporter reporter = mock(Reporter.class);

		NanoHTTPD httpd = new NanoHTTPD(0, new File(".")) {
			@Override
			public Response serve(String uri, String method, Properties header, Properties parms, Properties files) {
				Response r;
				if ("/foo".equals(uri)) {
					r = new Response("302 Found", "text/plain", "over there");
					r.header.put("Location", "/bar");
				} else if ("/bar".equals(uri)) {
					r = new Response(NanoHTTPD.HTTP_OK, "text/plain", "got it");
				} else {
					r = new Response(NanoHTTPD.HTTP_BADREQUEST, "text/plain", "sod off");
				}
				return r;
			}
		};
		try {
			String baseUrl = "http://localhost:" + httpd.getPort() + "/";
			String originalUrl = baseUrl + "foo";
			String redirectUrl = baseUrl + "bar";

			try (HttpClient connector = new HttpClient()) {
				connector.setReporter(reporter);

				InputStream stream = connector.connect(new URL(originalUrl));
				String result = IO.collect(stream);

				assertEquals("got it", result);
			}
		} finally {
			httpd.stop();
		}
	}

	public void testDetectRedirectLoop() throws Exception {
		final NanoHTTPD httpd = new NanoHTTPD(0, new File(".")) {
			@Override
			public Response serve(String uri, String method, Properties header, Properties parms, Properties files) {
				Response r;
				if ("/foo".equals(uri)) {
					r = new Response("302 Found", "text/plain", "over there");
					r.header.put("Location", "/bar");
				} else if ("/bar".equals(uri)) {
					r = new Response("302 Found", "text/plain", "go back");
					r.header.put("Location", "/foo");
				} else {
					r = new Response(NanoHTTPD.HTTP_BADREQUEST, "text/plain", "sod off");
				}
				return r;
			}
		};

		try {
			// Use a future to ensure we timeout after 1s if the redirect does
			// actually loop forever
			ExecutorService executor = Executors.newSingleThreadExecutor();
			Future<?> future = executor.submit(() -> {
				try (HttpClient connector = new HttpClient()) {
					try {
						InputStream stream = connector.build()
							.retries(0)
							.get(InputStream.class)
							.go(new URL("http://localhost:" + httpd.getPort() + "/foo"));
						IO.collect(stream);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			try {
				future.get(5, TimeUnit.SECONDS);
			} finally {
				future.cancel(true);
				executor.shutdownNow();
			}
		} finally {
			httpd.stop();
		}
	}
}
