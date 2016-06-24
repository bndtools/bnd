package aQute.bnd.comm.tests;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.progress.ProgressPlugin;
import aQute.bnd.service.url.TaggedData;
import aQute.bnd.url.HttpsVerification;
import aQute.http.testservers.HttpTestServer.Config;
import aQute.http.testservers.Httpbin;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class HttpClientTest extends TestCase {
	private Httpbin	httpServer;
	private Httpbin	httpsServer;

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		httpServer.close();
		httpsServer.close();
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Config config = new Config();
		config.https = false;
		httpServer = new Httpbin(config);
		httpServer.start();

		Config configs = new Config();
		configs.https = true;
		httpsServer = new Httpbin(configs);
		httpsServer.start();
	}

	public void testTimeout() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			try {
				TaggedData tag = hc.build().asTag().timeout(1000).go(httpServer.getBaseURI("timeout/10"));
				assertNotNull(tag);
				assertEquals(200, tag.getResponseCode());
				IO.collect(tag.getInputStream());
				fail();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void testCancel() throws Exception {
		final long deadline = System.currentTimeMillis() + 1000L;

		try (HttpClient hc = new HttpClient();) {
			Processor p = new Processor();
			p.addBasicPlugin(new ProgressPlugin() {

				@Override
				public Task startTask(String name, int size) {
					return new Task() {

						@Override
						public void worked(int units) {
							System.out.println("Worked " + units);
						}

						@Override
						public void done(String message, Throwable e) {
							System.out.println("Done " + message + " " + e);
						}

						@Override
						public boolean isCanceled() {
							System.out.println("Cancel check ");
							return System.currentTimeMillis() > deadline;
						}

					};
				}
			});
			hc.setRegistry(p);

			TaggedData tag = hc.build().asTag().go(httpServer.getBaseURI("timeout/50"));
			assertNotNull(tag);
			assertEquals(200, tag.getResponseCode());
			try {
				String s = IO.collect(tag.getInputStream());
				fail();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	public void testFetch() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			String text = hc.build().get(String.class).go(httpServer.getBaseURI("get"));
			assertNotNull(text);
			assertTrue(text.startsWith("{"));
		}
	}

	public void testRedirect() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			TaggedData tag = hc.build().get(TaggedData.class).go(httpServer.getBaseURI("redirect/3/200"));
			assertNotNull(tag);
			assertEquals(200, tag.getResponseCode());
		}
	}

	public void testRedirectTooMany() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			TaggedData tag = hc.build()
					.maxRedirects(3)
					.get(TaggedData.class)
					.go(httpServer.getBaseURI("redirect/200/200"));
			assertEquals(3, tag.getResponseCode() / 100);
		}
	}

	public void testRedirectURL() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			HttpsVerification httpsVerification = new HttpsVerification(httpsServer.getCertificateChain(), false,
					hc.getReporter());
			hc.addURLConnectionHandler(httpsVerification);
			URI uri = httpsServer.getBaseURI("get");
			URL go = httpServer.getBaseURI("xlocation").toURL();

			TaggedData tag = hc.build()
					.maxRedirects(3)
					.get(TaggedData.class)
					.headers("XLocation", uri.toString())
					.go(go);
			assertNotNull(tag);
			assertEquals(200, tag.getResponseCode());
		}
	}

	public void testETag() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			TaggedData data = hc.build().get(TaggedData.class).go(httpServer.getBaseURI("etag/1234/0"));
			assertNotNull(data);
			assertEquals("1234", data.getTag());
		}
	}

	public void testNotModifiedEtag() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			TaggedData data = hc.build()
					.get(TaggedData.class)
					.ifNoneMatch("1234")
					.go(httpServer.getBaseURI("etag/1234/0"));
			assertNotNull(data);
			assertEquals("1234", data.getTag());
			assertEquals(HttpURLConnection.HTTP_NOT_MODIFIED, data.getResponseCode());
		}
	}

	public void testModifiedWithEtag() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			TaggedData data = hc.build()
					.get(TaggedData.class)
					.ifNoneMatch("0000")
					.go(httpServer.getBaseURI("etag/1234/0"));
			assertNotNull(data);
			assertEquals("1234", data.getTag());
			assertEquals(200, data.getResponseCode());
		}
	}

	public void testNotModifiedSince() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			TaggedData data = hc.build().get(TaggedData.class).ifNoneMatch("*").ifModifiedSince(20000).go(
					httpServer.getBaseURI("etag/1234/10000"));
			assertNotNull(data);
			assertEquals("1234", data.getTag());
			assertEquals(HttpURLConnection.HTTP_NOT_MODIFIED, data.getResponseCode());
		}
	}

	public void testNotModifiedSinceAtSameTime() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			TaggedData data = hc.build().get(TaggedData.class).ifNoneMatch("*").ifModifiedSince(20000).go(
					httpServer.getBaseURI("etag/1234/20000"));
			assertNotNull(data);
			assertEquals("1234", data.getTag());
			assertEquals(HttpURLConnection.HTTP_NOT_MODIFIED, data.getResponseCode());
		}
	}

	public void testModifiedSince() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			TaggedData data = hc.build()
					.get(TaggedData.class)
					.ifModifiedSince(10000)
					.go(httpServer.getBaseURI("etag/1234/20000"));
			assertNotNull(data);
			assertEquals("1234", data.getTag());
			assertEquals(200, data.getResponseCode());
		}
		try (HttpClient hc = new HttpClient();) {
			TaggedData data = hc.build()
					.get(TaggedData.class)
					.ifModifiedSince(20000)
					.go(httpServer.getBaseURI("etag/1234/10000"));
			assertNotNull(data);
			assertEquals("1234", data.getTag());
			assertEquals(304, data.getResponseCode());
		}
	}

	public void testMultipleProgressPlugins() throws Exception {
		final long deadline = System.currentTimeMillis() + 1000L;

		try (HttpClient hc = new HttpClient();) {
			Processor p = new Processor();

			final int[] counts = new int[2];
			counts[0] = counts[1] = 0;

			p.addBasicPlugin(new ProgressPlugin() {
				@Override
				public Task startTask(String name, int size) {
					return new Task() {
						@Override
						public void worked(int units) {
							counts[0]++;
						}

						@Override
						public void done(String message, Throwable e) {
							counts[0]++;
						}

						@Override
						public boolean isCanceled() {
							return false;
						}
					};
				}
			});
			p.addBasicPlugin(new ProgressPlugin() {
				@Override
				public Task startTask(String name, int size) {
					return new Task() {
						@Override
						public void worked(int units) {
							counts[1]++;
						}

						@Override
						public void done(String message, Throwable e) {
							counts[1]++;
						}

						@Override
						public boolean isCanceled() {
							return false;
						}
					};
				}
			});
			hc.setRegistry(p);

			String text = hc.build().get(String.class).go(httpServer.getBaseURI("get"));
			assertNotNull(text);
			assertTrue(counts[0] > 0);
			assertEquals(counts[0], counts[1]);
		}
	}
}
