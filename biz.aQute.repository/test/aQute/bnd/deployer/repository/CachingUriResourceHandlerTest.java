package aQute.bnd.deployer.repository;

import java.io.*;
import java.net.*;

import junit.framework.*;
import test.lib.*;
import aQute.bnd.deployer.http.*;
import aQute.lib.io.*;

public class CachingUriResourceHandlerTest extends TestCase {

	private static final String EXPECTED_SHA = "d0002141a722ef03ecd8fd2e0d3e4d3bc680ba91483cb4962f68a41a12dd01ab".toUpperCase();

	static File						currentDir		= new File(System.getProperty("user.dir"));

	public static void testLoadFromCache() throws Exception {
		CachingUriResourceHandle handle = new CachingUriResourceHandle(
				new URI("http://localhost:18083/bundles/dummybundle.jar"), new File(
						"testdata/httpcache/1"), new DefaultURLConnector(), (String) null);
		File result = handle.request();

		assertEquals(
				new File("testdata/httpcache/1/http%3A%2F%2Flocalhost%3A18083%2Fbundles/dummybundle.jar")
						.getAbsolutePath(),
				result.getAbsolutePath());
	}

	public static void testFailedLoadFromRemote() throws Exception {
		CachingUriResourceHandle handle = new CachingUriResourceHandle(
				new URI("http://localhost:18083/bundles/dummybundle.jar"), new File(
						"testdata/httpcache/2"), new DefaultURLConnector(), (String) null);

		try {
			handle.request();
			fail("Should throw IOException");
		}
		catch (IOException e) {
			// expected
		}
	}

	public static void testLoadFromRemote() throws Exception {
		CachingUriResourceHandle handle = new CachingUriResourceHandle(
				new URI("http://localhost:18083/bundles/dummybundle.jar"), new File(
						"testdata/httpcache/3"), new DefaultURLConnector(), (String) null);

		NanoHTTPD httpd = new NanoHTTPD(18083, new File("testdata/http"));
		try {
			File result = handle.request();
			assertEquals(
					new File("testdata/httpcache/3/http%3A%2F%2Flocalhost%3A18083%2Fbundles/dummybundle.jar")
							.getAbsolutePath(),
					result.getAbsolutePath());

			File shaFile = new File(result.getAbsolutePath() + AbstractIndexedRepo.REPO_INDEX_SHA_EXTENSION);
			assertEquals(EXPECTED_SHA, IO.collect(shaFile));

			result.delete();
			shaFile.delete();
		}
		finally {
			httpd.stop();
		}
	}

	public static void testUseCached() throws Exception {
		File cached = new File("testdata/httpcache/4/http%3A%2F%2Flocalhost%3A18083%2Fbundles/dummybundle.jar");
		long cacheTimestamp = cached.lastModified();

		CachingUriResourceHandle handle = new CachingUriResourceHandle(
				new URI("http://localhost:18083/bundles/dummybundle.jar"), new File("testdata/httpcache/4"), new DefaultURLConnector(), EXPECTED_SHA);

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

	public static void testReplaceCache() throws Exception {
		File cached = new File("testdata/httpcache/5/http%3A%2F%2Flocalhost%3A18083%2Fbundles/dummybundle.jar");
		long cacheTimestamp = cached.lastModified();

		// Clear the SHA so the file appears modified
		File shaFile = new File(cached.getAbsolutePath() + AbstractIndexedRepo.REPO_INDEX_SHA_EXTENSION);
		IO.copy(IO.stream("00000000"), shaFile);

		CachingUriResourceHandle handle = new CachingUriResourceHandle(
				new URI("http://localhost:18083/bundles/dummybundle.jar"), new File("testdata/httpcache/5"), new DefaultURLConnector(), EXPECTED_SHA);

		NanoHTTPD httpd = new NanoHTTPD(18083, new File("testdata/http"));
		try {
			File result = handle.request();
			assertEquals(cached, result);
			assertNotSame("File timestamp SHOULD change", cacheTimestamp, result.lastModified());

			assertEquals(EXPECTED_SHA, IO.collect(shaFile));
		}
		finally {
			httpd.stop();
		}
	}

	public static void testEmptyCache() throws Exception {
		File cached = new File("testdata/httpcache/6/http%3A%2F%2Flocalhost%3A18083%2Fbundles/dummybundle.jar");
		cached.delete();

		File shaFile = new File(cached.getAbsolutePath() + AbstractIndexedRepo.REPO_INDEX_SHA_EXTENSION);
		shaFile.delete();

		CachingUriResourceHandle handle = new CachingUriResourceHandle(
				new URI("http://localhost:18083/bundles/dummybundle.jar"), new File("testdata/httpcache/6"), new DefaultURLConnector(), EXPECTED_SHA);
		NanoHTTPD httpd = new NanoHTTPD(18083, new File("testdata/http"));
		try {
			File result = handle.request();
			assertEquals(cached, result);
			assertEquals(EXPECTED_SHA, IO.collect(shaFile));
		}
		finally {
			httpd.stop();
		}
	}
	
	public static void testUseCacheWhenRemoteUnavailable() throws Exception {
		File cached = new File("testdata/httpcache/7/http%3A%2F%2Flocalhost%3A18083%2Fbundles/dummybundle.jar");
		CachingUriResourceHandle handle = new CachingUriResourceHandle(new URI("http://localhost:18083/bundles/dummybundle.jar"), new File("testdata/httpcache/7"), new DefaultURLConnector(), (String) null);

		// whoops where's the server...

		File result = handle.request();
		assertEquals(cached, result);
	}

}
