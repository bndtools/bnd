package aQute.bnd.comm.tests;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import aQute.bnd.http.HttpClient;
import aQute.bnd.service.url.TaggedData;
import aQute.bnd.url.HttpsVerification;
import biz.aQute.http.testservers.HttpTestServer.Config;
import biz.aQute.http.testservers.Httpbin;
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

	public void testFetch() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			String text = hc.build().get(String.class).go(new URL(httpServer.getBaseURI().toString() + "/get"));
			assertNotNull(text);
			assertTrue(text.startsWith("{"));
		}
	}

	public void testRedirect() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			TaggedData tag = hc.build()
					.get(TaggedData.class)
					.go(new URL(httpServer.getBaseURI().toString() + "/redirect/3/200"));
			assertNotNull(tag);
			assertEquals(200, tag.getResponseCode());
		}
	}

	public void testRedirectTooMany() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			TaggedData tag = hc.build().maxRedirects(3).get(TaggedData.class).go(
					new URL(httpServer.getBaseURI().toString() + "/redirect/200/200"));
			assertEquals(3, tag.getResponseCode() / 100);
		}
	}

	public void testRedirectURL() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			hc.addBasicPlugin(new HttpsVerification(httpsServer.getCertificateChain(), false, hc));
			URI uri = new URI(httpsServer.getBaseURI() + "/get");
			URL go = new URL(httpServer.getBaseURI().toString() + "/xlocation");
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
					.go(new URL(httpServer.getBaseURI().toString() + "/etag/1234/0"));
			assertNotNull(data);
			assertEquals("1234", data.getTag());
		}
	}

	public void testNotModifiedEtag() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			TaggedData data = hc.build().get(TaggedData.class).ifNoneMatch("1234").go(
					new URL(httpServer.getBaseURI().toString() + "/etag/1234/0"));
			assertNotNull(data);
			assertEquals("1234", data.getTag());
			assertEquals(HttpURLConnection.HTTP_NOT_MODIFIED, data.getResponseCode());
		}
	}

	public void testModifiedWithEtag() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			TaggedData data = hc.build().get(TaggedData.class).ifNoneMatch("0000").go(
					new URL(httpServer.getBaseURI().toString() + "/etag/1234/0"));
			assertNotNull(data);
			assertEquals("1234", data.getTag());
			assertEquals(200, data.getResponseCode());
		}
	}

	public void testNotModifiedSince() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			TaggedData data = hc.build().get(TaggedData.class).ifNoneMatch("*").ifModifiedSince(20000).go(
					new URL(httpServer.getBaseURI().toString() + "/etag/1234/10000"));
			assertNotNull(data);
			assertEquals("1234", data.getTag());
			assertEquals(HttpURLConnection.HTTP_NOT_MODIFIED, data.getResponseCode());
		}
	}

	public void testNotModifiedSinceAtSameTime() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			TaggedData data = hc.build().get(TaggedData.class).ifNoneMatch("*").ifModifiedSince(20000).go(
					new URL(httpServer.getBaseURI().toString() + "/etag/1234/20000"));
			assertNotNull(data);
			assertEquals("1234", data.getTag());
			assertEquals(HttpURLConnection.HTTP_NOT_MODIFIED, data.getResponseCode());
		}
	}

	public void testModifiedSince() throws Exception {
		try (HttpClient hc = new HttpClient();) {
			TaggedData data = hc.build().get(TaggedData.class).ifNoneMatch("*").ifModifiedSince(10000).go(
					new URL(httpServer.getBaseURI().toString() + "/etag/1234/20000"));
			assertNotNull(data);
			assertEquals("1234", data.getTag());
			assertEquals(200, data.getResponseCode());
		}
	}
}
