package aQute.bnd.comm.tests;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.util.promise.Promise;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.progress.ProgressPlugin;
import aQute.bnd.service.url.State;
import aQute.bnd.service.url.TaggedData;
import aQute.bnd.url.HttpsVerification;
import aQute.http.testservers.HttpTestServer.Config;
import aQute.http.testservers.Httpbin;
import aQute.lib.io.IO;
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
	}

	public void testTimeout() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			try {
				TaggedData tag = hc.build()
					.asTag()
					.timeout(1000)
					.go(httpServer.getBaseURI("timeout/10"));
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
}
