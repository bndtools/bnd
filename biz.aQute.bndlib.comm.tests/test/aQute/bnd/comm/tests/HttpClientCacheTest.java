package aQute.bnd.comm.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import aQute.bnd.http.HttpClient;
import aQute.bnd.service.url.State;
import aQute.bnd.service.url.TaggedData;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.http.testservers.HttpTestServer.Config;
import aQute.http.testservers.Httpbin;
import aQute.lib.io.IO;

public class HttpClientCacheTest {

	@InjectTemporaryDirectory
	File			tmp;
	File			cache;
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

			String qetag = etag;
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

	@BeforeEach
	public void setUp() throws Exception {
		cache = IO.getFile(tmp, "cache");
		Config config = new Config();
		config.https = false;
		httpServer = new MyHttpBin(config);
		httpServer.start();
	}

	@AfterEach
	public void tearDown() throws Exception {
		IO.close(httpServer);
	}

	@Test
	public void testGetNewThenUnmodifiedThenModified() throws URISyntaxException, Exception {
		try (HttpClient client = new HttpClient();) {

			client.setCache(cache);

			// Set a tag, but it must always fetch the file

			etag = "1234";
			File go1 = client.build()
				.useCache()
				.go(new URI(httpServer.getBaseURI() + "/testetag"));
			assertTrue(go1.isFile());
			assertEquals("1234", IO.collect(go1));

			// Set time very old so we can see if the file is updated

			go1.setLastModified(1000);

			File go2 = client.build()
				.useCache()
				.go(new URI(httpServer.getBaseURI() + "/testetag"));
			assertEquals(go1, go2);
			assertEquals("1234", IO.collect(go1));
			assertEquals(1000, go2.lastModified());

			// New tag (i.e. file is updated on server) but we use the stale
			// period to NOT fetch a new one

			etag = "5678";
			go1.setLastModified(System.currentTimeMillis());

			File go3 = client.build()
				.useCache(10000)
				.go(new URI(httpServer.getBaseURI() + "/testetag"));
			assertEquals(go1, go3);
			assertEquals("1234", IO.collect(go3));

			// We have a stale copy, see if we fetch a new copy

			File go4 = client.build()
				.useCache()
				.go(new URI(httpServer.getBaseURI() + "/testetag"));
			assertEquals(go1, go4);
			assertEquals("5678", IO.collect(go3));
		}
	}

	/**
	 * Use the cached form but use our own file, not one from the central cache
	 *
	 * @throws Exception
	 */

	@Test
	public void testPrivateFile() throws Exception {
		try (HttpClient client = new HttpClient();) {
			client.setCache(new File(tmp, "cache"));
			etag = "1234";
			File t1 = new File(tmp, "abc.txt");

			//
			// File does not exist
			//

			assertFalse(t1.isFile(), "File should not exist");
			TaggedData tag = client.build()
				.useCache(t1)
				.asTag()
				.go(new URI(httpServer.getBaseURI() + "/testetag"));
			assertTrue(tag.isOk(), "Expected the file to be created (not unmodified)");
			assertTrue(t1.isFile(), "Just created the file");
			assertEquals("1234", tag.getTag(), "Should be the tag we set");

			tag = client.build()
				.useCache(t1, 100000)
				.asTag()
				.go(new URI(httpServer.getBaseURI() + "/testetag"));
			assertEquals(State.UNMODIFIED, tag.getState(), "Expected file to be 'fresh'");

			tag = client.build()
				.useCache(t1)
				.asTag()
				.go(new URI(httpServer.getBaseURI() + "/testetag"));
			assertTrue(tag.isNotModified(), "Should have checked so we should have unmodified");

			etag = "5678";

			tag = client.build()
				.useCache(t1, 100000)
				.asTag()
				.go(new URI(httpServer.getBaseURI() + "/testetag"));
			assertEquals(State.UNMODIFIED, tag.getState(), "Since it is still fresh, we expect no new fetch");

			tag = client.build()
				.useCache(t1)
				.asTag()
				.go(new URI(httpServer.getBaseURI() + "/testetag"));
			assertEquals("5678", tag.getTag(), "We should have fetched it with the new etag");
			assertTrue(tag.isOk(), "And it should have been modified");

			String s = client.build()
				.useCache(t1)
				.get(String.class)
				.go(new URI(httpServer.getBaseURI() + "/testetag"));
			assertEquals("5678", s, "Content check");

			byte[] b = client.build()
				.useCache(t1)
				.get(byte[].class)
				.go(new URI(httpServer.getBaseURI() + "/testetag"));
			assertTrue(Arrays.equals("5678".getBytes(), b), "Content check");
		}
	}

}
