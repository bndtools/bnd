package aQute.bnd.comm.tests;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import aQute.bnd.http.HttpClient;
import aQute.lib.io.IO;
import biz.aQute.http.testservers.HttpTestServer.Config;
import biz.aQute.http.testservers.Httpbin;
import junit.framework.TestCase;

public class HttpClientCacheTest extends TestCase {

	File			tmp	= IO.getFile("generated/tmp");
	private Httpbin	httpServer;
	String			etag;

	/*
	 * Add a method where we can set the returned etag
	 */
	public class MyHttpBin extends Httpbin {
		public MyHttpBin(Config config) throws Exception {
			super(config);
		}

		@SuppressWarnings("unused")
		public void _testetag(Request rq, Response rsp) throws Exception {
			String requestedTag = rq.headers.get("If-None-Match");

			String qetag = "\"" + etag + "\"";
			if (!etag.isEmpty())
				rsp.headers.put("ETag", qetag);

			if (requestedTag != null) {
				if (requestedTag.equals("*") || requestedTag.equals(qetag)) {
					rsp.code = HttpURLConnection.HTTP_NOT_MODIFIED;
					rsp.length = 0;
					return;
				}
			}

			rsp.content = etag != null ? etag.getBytes(StandardCharsets.UTF_8) : new byte[0];
			return;
		}
	}

	public void setUp() throws Exception {
		IO.delete(tmp);
		tmp.mkdirs();
		Config config = new Config();
		config.https = false;
		httpServer = new MyHttpBin(config);
		httpServer.start();
	}

	public void tearDown() {
		IO.delete(tmp);
	}

	public void testGetNewThenUnmodifiedThenModified() throws URISyntaxException, Exception {
		try (HttpClient client = new HttpClient();) {

			client.setCache(new File(tmp, "cache"));

			// Set a tag, but it must always fetch the file

			etag = "1234";
			File go1 = client.build().useCache().go(new URI(httpServer.getBaseURI() + "/testetag"));
			assertTrue(go1.isFile());
			assertEquals("1234", IO.collect(go1));

			// Set time very old so we can see if the file is updated

			go1.setLastModified(1000);

			File go2 = client.build().useCache().go(new URI(httpServer.getBaseURI() + "/testetag"));
			assertEquals(go1, go2);
			assertEquals("1234", IO.collect(go1));
			assertEquals(1000, go2.lastModified());

			// New tag (i.e. file is updated on server) but we use the stale
			// period to NOT fetch a new one

			etag = "5678";
			go1.setLastModified(System.currentTimeMillis());

			File go3 = client.build().useCache(10000).go(new URI(httpServer.getBaseURI() + "/testetag"));
			assertEquals(go1, go3);
			assertEquals("1234", IO.collect(go3));

			// We have a stale copy, see if we fetch a new copy

			File go4 = client.build().useCache().go(new URI(httpServer.getBaseURI() + "/testetag"));
			assertEquals(go1, go4);
			assertEquals("5678", IO.collect(go3));
		}
	}

}
