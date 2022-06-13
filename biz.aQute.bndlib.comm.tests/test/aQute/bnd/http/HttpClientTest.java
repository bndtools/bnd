package aQute.bnd.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.util.promise.Promise;

import aQute.bnd.connection.settings.ConnectionSettings;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.progress.ProgressPlugin;
import aQute.bnd.service.url.State;
import aQute.bnd.service.url.TaggedData;
import aQute.bnd.url.HttpsVerification;
import aQute.http.testservers.HttpTestServer.Config;
import aQute.http.testservers.Httpbin;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import junit.framework.TestCase;

public class HttpClientTest extends TestCase {
	private TestServer	httpServer;
	private Httpbin		httpsServer;
	private File		tmp;

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		httpServer.close();
		httpsServer.close();
		IO.delete(tmp);
	}

	public static class TestServer extends Httpbin {
		boolean second = false;

		public TestServer(Config config) throws Exception {
			super(config);
		}

		public void _mftch(Request rq, Response rsp) throws Exception {
			Thread.sleep(1000);
			rsp.headers.put("ETag", "FOO");
			if (second) {
				rsp.code = 304;
				return;
			}
			rsp.code = 200;
			rsp.content = "OK!".getBytes();
			second = true;
		}

		public String _cheshire(Request rq, Response rsp) throws Exception {
			if (second) {
				rsp.code = 404;
				return "404";
			} else {
				rsp.code = 200;
				return "ok";
			}
		}

		AtomicInteger n = new AtomicInteger();

		public void _solitary(Request rq, Response rsp, int max, int id) throws Exception {
			int x = n.incrementAndGet();
			System.out.println("entering solitary " + id + " max = " + max + " n=" + x);
			try {
				if (x > max) {
					rsp.code = 400;
					return;
				}
				Thread.sleep(200);
			} finally {
				System.out.println("leaving solitary " + id + " max = " + max + " n=" + x);
				n.decrementAndGet();
			}
		}

	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Config config = new Config();
		config.https = false;
		httpServer = new TestServer(config);
		httpServer.start();

		Config configs = new Config();
		configs.https = true;
		httpsServer = new Httpbin(configs);
		httpsServer.start();

		tmp = IO.getFile("generated/tmp");
		IO.delete(tmp);
		tmp.mkdirs();
		httpServer.second = false;
	}

	public void testHttpsVerification() throws Exception {
		try (Processor p = new Processor()) {
			p.setProperty("-connection-settings", "server;id=\"" + httpsServer.getBaseURI() + "\";verify=" + true
				+ ";trust=\"" + Strings.join(httpsServer.getTrustedCertificateFiles(tmp)) + "\"");
			HttpClient client = new HttpClient();
			client.setReporter(p);
			ConnectionSettings cs = new ConnectionSettings(p, client);
			cs.readSettings();

			//
			// First try a public HTTPS
			//

			String PUBLIC_OK_HTTPS = "https://github.com/bndtools/bnd/blob/master/biz.aQute.bndall.tests/bnd.bnd";

			TaggedData go1 = client.build()
				.asTag()
				.go(new URI(PUBLIC_OK_HTTPS));

			assertEquals(200, go1.getResponseCode());

			//
			// Try the private https for which we set the trust anchors file
			//

			TaggedData go2 = client.build()
				.asTag()
				.go(httpsServer.getBaseURI("get/foo"));

			assertEquals(200, go2.getResponseCode());

			//
			// Create a new private https for which we do not have a trust
			// anchor
			//

			Config configs = new Config();
			configs.https = true;
			try (Httpbin extraServer = new Httpbin(configs)) {
				extraServer.start();

				TaggedData go3 = client.build()
					.asTag()
					.go(extraServer.getBaseURI("get/foo"));

				assertEquals(526, go3.getResponseCode());
			}
		}
	}

	public void testTimeout() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			try {
				TaggedData tag = hc.build()
					.asTag()
					.timeout(1000)
					.go(httpServer.getBaseURI("timeout/60"));
				assertNotNull(tag);
				assertEquals(200, tag.getResponseCode());
				IO.collect(tag.getInputStream());
				fail();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void testClearCacheOn404() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			hc.setCache(tmp);
			URLCache cache = hc.cache();

			URI cheshire = httpServer.getBaseURI("cheshire");
			assertThat(cache.isCached(cheshire)).isFalse();

			TaggedData tag = hc.build()
				.useCache()
				.asTag()
				.go(cheshire);
			assertNotNull(tag);
			assertEquals(200, tag.getResponseCode());

			assertThat(cache.isCached(cheshire)).isTrue();

			httpServer.second = true;

			tag = hc.build()
				.useCache()
				.asTag()
				.go(cheshire);
			assertNotNull(tag);
			assertEquals(404, tag.getResponseCode());
			assertThat(cache.isCached(cheshire)).isFalse();
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

			TaggedData tag = hc.build()
				.asTag()
				.go(httpServer.getBaseURI("timeout/50"));
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

	public void testCachingMultipleFetch() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			hc.setCache(tmp);

			Promise<TaggedData> a = hc.build()
				.useCache()
				.age(1, TimeUnit.DAYS)
				.asTag()
				.async(httpServer.getBaseURI("mftch"));
			Thread.sleep(100);

			Promise<TaggedData> b = hc.build()
				.useCache()
				.age(1, TimeUnit.DAYS)
				.asTag()
				.async(httpServer.getBaseURI("mftch"));

			TaggedData ta = a.getValue();
			assertEquals("FOO", ta.getTag());
			assertEquals(200, ta.getResponseCode());
			assertEquals(true, ta.hasPayload());
			assertEquals(true, ta.isOk());
			assertEquals(State.UPDATED, ta.getState());

			TaggedData tb = b.getValue();
			assertEquals("FOO", tb.getTag());
			assertEquals(304, tb.getResponseCode());
			assertEquals(false, tb.hasPayload());
			assertEquals(State.UNMODIFIED, tb.getState());

			System.out.println(a.getValue());
			System.out.println(b.getValue());
		}
	}

	public void testFetch() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			String text = hc.build()
				.get(String.class)
				.go(httpServer.getBaseURI("get"));
			assertNotNull(text);
			assertTrue(text.startsWith("{"));
		}
	}

	public void testURLResource() throws Exception {
		try (HttpClient hc = new HttpClient()) {
			URL url = httpServer.getBaseURI("get")
				.toURL();
			try (Resource resource = Resource.fromURL(url, hc)) {
				ByteBuffer bb = resource.buffer();
				assertThat(bb).isNotNull();
				String text = IO.collect(bb, UTF_8);
				assertThat(text).startsWith("{");
			}
		}
	}

	public void testRedirect() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			TaggedData tag = hc.build()
				.get(TaggedData.class)
				.go(httpServer.getBaseURI("redirect/3/200"));
			assertNotNull(tag);
			assertEquals(200, tag.getResponseCode());
		}
	}

	public void testRedirectRelative() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			TaggedData tag = hc.build()
				.get(TaggedData.class)
				.go(httpServer.getBaseURI("redirect/3/200?relative=true"));
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

	public void testThatHttpWithUnverifiedNameGeneratesError() throws Exception {
		Processor p = new Processor();
		try (HttpClient hc = new HttpClient();) {
			hc.setReporter(p);
			URI go = httpsServer.getBaseURI("get/foo");

			TaggedData tag = hc.build()
				.get(TaggedData.class)
				.go(go);
			assertNotNull(tag);
			assertEquals(526, tag.getResponseCode());
		}
		assertTrue(p.check());
	}

	public void testThatHttpWithUnverifiedNameButMatchingHandlerIsOk() throws Exception {
		Processor p = new Processor();
		try (HttpClient hc = new HttpClient();) {
			hc.setReporter(p);
			HttpsVerification httpsVerification = new HttpsVerification(httpsServer.getCertificateChain(), true,
				hc.getReporter());
			httpsVerification.addMatcher(httpsServer.getBaseURI()
				.toString() + "/*");
			hc.addURLConnectionHandler(httpsVerification);
			URI go = httpsServer.getBaseURI("get/foo");

			TaggedData tag = hc.build()
				.get(TaggedData.class)
				.go(go);
			assertNotNull(tag);
			assertEquals(200, tag.getResponseCode());
		}
		assertTrue(p.check());
	}

	public void testInvalidTrustAnchors() throws Exception {
		Processor p = new Processor();
		try (HttpClient hc = new HttpClient();) {
			hc.setReporter(p);

			Config c = new Config();
			c.https = true;
			Httpbin httpbin = new Httpbin(c);
			X509Certificate[] invalidChain = httpbin.getCertificateChain();
			httpbin.close();

			HttpsVerification httpsVerification = new HttpsVerification(invalidChain, true, hc.getReporter());
			hc.addURLConnectionHandler(httpsVerification);
			URI go = httpsServer.getBaseURI("get/foo");

			TaggedData tag = hc.build()
				.get(TaggedData.class)
				.go(go);
			assertNotNull(tag);
			assertEquals(526, tag.getResponseCode());
		}
		assertTrue(p.check());
	}

	public void testRedirectURL() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			HttpsVerification httpsVerification = new HttpsVerification(httpsServer.getCertificateChain(), false,
				hc.getReporter());
			hc.addURLConnectionHandler(httpsVerification);
			URI uri = httpsServer.getBaseURI("get");
			URL go = httpServer.getBaseURI("xlocation")
				.toURL();

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
			TaggedData data = hc.build()
				.get(TaggedData.class)
				.go(httpServer.getBaseURI("etag/1234/0"));
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
			TaggedData data = hc.build()
				.get(TaggedData.class)
				.ifNoneMatch("*")
				.ifModifiedSince(20000)
				.go(httpServer.getBaseURI("etag/1234/10000"));
			assertNotNull(data);
			assertEquals("1234", data.getTag());
			assertEquals(HttpURLConnection.HTTP_NOT_MODIFIED, data.getResponseCode());
		}
	}

	public void testNotModifiedSinceAtSameTime() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			TaggedData data = hc.build()
				.get(TaggedData.class)
				.ifNoneMatch("*")
				.ifModifiedSince(20000)
				.go(httpServer.getBaseURI("etag/1234/20000"));
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

			String text = hc.build()
				.get(String.class)
				.go(httpServer.getBaseURI("get"));
			assertNotNull(text);
			assertTrue(counts[0] > 0);
			assertEquals(counts[0], counts[1]);
		}
	}

	public void testPut() throws URISyntaxException, Exception {
		try (Processor p = new Processor();) {
			final AtomicBoolean done = new AtomicBoolean();
			p.addBasicPlugin(new ProgressPlugin() {

				@Override
				public Task startTask(final String name, int size) {
					System.out.println("start " + name);
					return new Task() {

						@Override
						public void worked(int units) {
							System.out.println("worked " + name + " " + units);
						}

						@Override
						public void done(String message, Throwable e) {
							System.out.println("done " + name + " " + message + " " + e);
							done.set(true);
						}

						@Override
						public boolean isCanceled() {
							return false;
						}
					};
				}
			});
			try (HttpClient c = new HttpClient();) {
				c.setRegistry(p);
				TaggedData go = c.build()
					.verb("PUT")
					.upload("hello")
					.asTag()
					.go(httpServer.getBaseURI("put"));
				go.getInputStream()
					.close();
				assertEquals(200, go.getResponseCode());
				assertTrue(done.get());
			}
		}
	}

	public void testNoLimitConnection() throws Exception {
		try (Processor p = new Processor()) {
			try (HttpClient client = new HttpClient()) {
				client.maxConcurrentConnections = 0;
				xtestParallel(client, 100);
			}
		}
	}

	public void testLimitConnectionSet() throws Exception {
		try (Processor p = new Processor()) {
			p.setProperty("-x-max-concurrent-connections", "55");
			try (HttpClient client = new HttpClient()) {
				client.readSettings(p);
				assertThat(client.maxConcurrentConnections).isEqualTo(55);
			}
		}
	}

	public void testLimitConnections() throws Exception {
		try (Processor p = new Processor()) {
			try (HttpClient client = new HttpClient()) {
				client.maxConcurrentConnections = 3;
				xtestParallel(client, 3);
			}
		}
	}

	AtomicInteger id = new AtomicInteger(1000);
	private void xtestParallel(HttpClient client, int max) {
		System.out.println("testing with max " + max);
		List<CompletableFuture<TaggedData>> fs = new ArrayList<>();
		for (int n = 0; n < 10; n++) {
			int local = id.getAndIncrement();
			CompletableFuture<TaggedData> f = CompletableFuture.supplyAsync(() -> {
				try {
					TaggedData tag = client.build()
						.timeout(5000)
						.asTag()
						.go(httpServer.getBaseURI("solitary/" + max + "/" + local));
					System.out.println("response " + tag.getResponseCode() + " Local " + local);
					return tag;
				} catch (Exception e) {
					throw new RuntimeException();
				}
			});
			fs.add(f);
		}

		fs.forEach(tag -> {
			try {
				TaggedData d = tag.get();
				assertThat(d).isNotNull();
				TaggedData taggedData = tag.get();
				assertThat(taggedData.getResponseCode()).isEqualTo(200);
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException();
			}
		});
	}
}
