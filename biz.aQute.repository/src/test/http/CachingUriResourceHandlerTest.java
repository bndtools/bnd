package test.http;

import java.io.*;
import java.net.URI;

import junit.framework.*;
import test.lib.*;
import aQute.lib.deployer.repository.*;
import aQute.lib.deployer.repository.CachingUriResourceHandle.CachingMode;
import aQute.lib.io.*;

public class CachingUriResourceHandlerTest extends TestCase {

	private static final String	EXPECTED_ETAG	= "64035a95";

	File						currentDir		= new File(System.getProperty("user.dir"));

	public void testLoadFromCache() throws Exception {
		CachingUriResourceHandle handle = new CachingUriResourceHandle(
				new URI("http://localhost:18083/bundles/dummybundle.jar"), new File(
						"testdata/httpcache/1"), CachingMode.PreferRemote);
		File result = handle.request();

		assertEquals(
				new File("testdata/httpcache/1/http%3A%2F%2Flocalhost%3A18083%2Fbundles/dummybundle.jar")
						.getAbsolutePath(),
				result.getAbsolutePath());
	}

	public void testFailedLoadFromRemote() throws Exception {
		CachingUriResourceHandle handle = new CachingUriResourceHandle(
				new URI("http://localhost:18083/bundles/dummybundle.jar"), new File(
						"testdata/httpcache/2"), CachingMode.PreferRemote);

		try {
			handle.request();
			fail("Should throw IOException");
		}
		catch (IOException e) {
			// expected
		}
	}

	public void testLoadFromRemote() throws Exception {
		CachingUriResourceHandle handle = new CachingUriResourceHandle(
				new URI("http://localhost:18083/bundles/dummybundle.jar"), new File(
						"testdata/httpcache/3"), CachingMode.PreferRemote);

		NanoHTTPD httpd = new NanoHTTPD(18083, new File("testdata/http"));
		try {
			File result = handle.request();
			assertEquals(
					new File("testdata/httpcache/3/http%3A%2F%2Flocalhost%3A18083%2Fbundles/dummybundle.jar")
							.getAbsolutePath(),
					result.getAbsolutePath());

			File etagFile = new File(result.getAbsolutePath() + ".etag");
			assertEquals(EXPECTED_ETAG, IO.collect(etagFile));

			result.delete();
			etagFile.delete();
		}
		finally {
			httpd.stop();
		}
	}

	public void testRemoteUnmodifiedETag() throws Exception {
		File cached = new File("testdata/httpcache/4/http%3A%2F%2Flocalhost%3A18083%2Fbundles/dummybundle.jar");
		long cacheTimestamp = cached.lastModified();

		CachingUriResourceHandle handle = new CachingUriResourceHandle(
				new URI("http://localhost:18083/bundles/dummybundle.jar"), new File(
						"testdata/httpcache/4"), CachingMode.PreferRemote);

		NanoHTTPD httpd = new NanoHTTPD(18083, new File("testdata/http"));
		try {
			File result = handle.request();
			assertEquals(cached, result);
			assertEquals("File timestamp should NOT change", cacheTimestamp, result.lastModified());
		}
		finally {
			httpd.stop();
		}
	}

	public void testRemoteModifiedETag() throws Exception {
		File cached = new File("testdata/httpcache/5/http%3A%2F%2Flocalhost%3A18083%2Fbundles/dummybundle.jar");
		long cacheTimestamp = cached.lastModified();

		// Clear the etag so the file appears modified
		File etagFile = new File(cached.getAbsolutePath() + ".etag");
		IO.copy(IO.stream("00000000"), etagFile);

		CachingUriResourceHandle handle = new CachingUriResourceHandle(
				new URI("http://localhost:18083/bundles/dummybundle.jar"), new File(
						"testdata/httpcache/5"), CachingMode.PreferRemote);

		NanoHTTPD httpd = new NanoHTTPD(18083, new File("testdata/http"));
		try {
			File result = handle.request();
			assertEquals(cached, result);
			assertNotSame("File timestamp SHOULD change", cacheTimestamp, result.lastModified());

			assertEquals(EXPECTED_ETAG, IO.collect(etagFile));
		}
		finally {
			httpd.stop();
		}
	}

	public void testPreferCacheEmptyCache() throws Exception {
		File cached = new File("testdata/httpcache/6/http%3A%2F%2Flocalhost%3A18083%2Fbundles/dummybundle.jar");
		cached.delete();

		File etagFile = new File(cached.getAbsolutePath() + ".etag");
		etagFile.delete();

		CachingUriResourceHandle handle = new CachingUriResourceHandle(
				new URI("http://localhost:18083/bundles/dummybundle.jar"), new File(
						"testdata/httpcache/6"), CachingMode.PreferCache);
		NanoHTTPD httpd = new NanoHTTPD(18083, new File("testdata/http"));
		try {
			File result = handle.request();
			assertEquals(cached, result);
			assertEquals(EXPECTED_ETAG, IO.collect(etagFile));
		}
		finally {
			httpd.stop();
		}
	}

	public void testPreferCache() throws Exception {
		File cached = new File("testdata/httpcache/7/http%3A%2F%2Flocalhost%3A18083%2Fbundles/dummybundle.jar");
		long cachedTimestamp = cached.lastModified();

		CachingUriResourceHandle handle = new CachingUriResourceHandle(
				new URI("http://localhost:18083/bundles/dummybundle.jar"), new File(
						"testdata/httpcache/7"), CachingMode.PreferCache);
		NanoHTTPD httpd = new NanoHTTPD(18083, new File("testdata/http"));
		try {
			File result = handle.request();
			assertEquals(cached, result);
			assertEquals("File timestamp should NOT change", cachedTimestamp, result.lastModified());
		}
		finally {
			httpd.stop();
		}
	}

}
