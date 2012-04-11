package test.lib.deployer.obr;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import test.lib.NanoHTTPD;
import aQute.lib.deployer.obr.CachingURLResourceHandle;
import aQute.lib.deployer.obr.CachingURLResourceHandle.CachingMode;
import aQute.lib.io.IO;

public class CachingURLResourceHandlerTest extends TestCase {
	
	public void testLoadFromCache() throws Exception {
		CachingURLResourceHandle handle = new CachingURLResourceHandle("http://localhost:18083/bundles/dummybundle.jar", "http://localhost:18083", new File("test/httpcache/1"), CachingMode.PreferRemote);
		File result = handle.request();
		
		assertEquals(new File("test/httpcache/1/http%3A%2F%2Flocalhost%3A18083%2Fbundles/dummybundle.jar").getAbsolutePath(), result.getAbsolutePath());
	}
	
	public void testFailedLoadFromRemote() throws Exception {
		CachingURLResourceHandle handle = new CachingURLResourceHandle("http://localhost:18083/bundles/dummybundle.jar", "http://localhost:18083", new File("test/httpcache/2"), CachingMode.PreferRemote);
		
		try {
			handle.request();
			fail("Should throw IOException");
		} catch (IOException e) {
			// expected
		}
	}
	
	public void testLoadFromRemote() throws Exception {
		CachingURLResourceHandle handle = new CachingURLResourceHandle("http://localhost:18083/bundles/dummybundle.jar", "http://localhost:18083", new File("test/httpcache/3"), CachingMode.PreferRemote);
		
		NanoHTTPD httpd = new NanoHTTPD(18083, new File("test/http"));
		try { 
			File result = handle.request();
			assertEquals(new File("test/httpcache/3/http%3A%2F%2Flocalhost%3A18083%2Fbundles/dummybundle.jar").getAbsolutePath(), result.getAbsolutePath());
			
			File etagFile = new File(result.getAbsolutePath() + ".etag");
			assertEquals("d5785fff", IO.collect(etagFile));
			
			result.delete();
			etagFile.delete();
		} finally {
			httpd.stop();
		}
	}
	
	public void testRemoteUnmodifiedETag() throws Exception {
		File cached = new File("test/httpcache/4/http%3A%2F%2Flocalhost%3A18083%2Fbundles/dummybundle.jar");
		long cacheTimestamp = cached.lastModified();
		
		CachingURLResourceHandle handle = new CachingURLResourceHandle("http://localhost:18083/bundles/dummybundle.jar", "http://localhost:18083", new File("test/httpcache/4"), CachingMode.PreferRemote);
		
		NanoHTTPD httpd = new NanoHTTPD(18083, new File("test/http"));
		try { 
			File result = handle.request();
			assertEquals(cached, result);
			assertEquals("File timestamp should NOT change", cacheTimestamp, result.lastModified());
		} finally {
			httpd.stop();
		}
	}
	
	public void testRemoteModifiedETag() throws Exception {
		File cached = new File("test/httpcache/5/http%3A%2F%2Flocalhost%3A18083%2Fbundles/dummybundle.jar");
		long cacheTimestamp = cached.lastModified();
		
		// Clear the etag so the file appears modified
		File etagFile = new File(cached.getAbsolutePath() + ".etag");
		IO.copy(IO.stream("00000000"), etagFile);
		
		CachingURLResourceHandle handle = new CachingURLResourceHandle("http://localhost:18083/bundles/dummybundle.jar", "http://localhost:18083", new File("test/httpcache/5"), CachingMode.PreferRemote);
		
		NanoHTTPD httpd = new NanoHTTPD(18083, new File("test/http"));
		try { 
			File result = handle.request();
			assertEquals(cached, result);
			assertNotSame("File timestamp SHOULD change", cacheTimestamp, result.lastModified());
			
			assertEquals("d5785fff", IO.collect(etagFile));
		} finally {
			httpd.stop();
		}
	}
	
	public void testPreferCacheEmptyCache() throws Exception {
		File cached = new File("test/httpcache/6/http%3A%2F%2Flocalhost%3A18083%2Fbundles/dummybundle.jar");
		cached.delete();
		
		File etagFile = new File(cached.getAbsolutePath() + ".etag");
		etagFile.delete();
		
		CachingURLResourceHandle handle = new CachingURLResourceHandle("http://localhost:18083/bundles/dummybundle.jar", "http://localhost:18083", new File("test/httpcache/6"), CachingMode.PreferCache);
		NanoHTTPD httpd = new NanoHTTPD(18083, new File("test/http"));
		try {
			File result = handle.request();
			assertEquals(cached, result);
			assertEquals("d5785fff", IO.collect(etagFile));
		} finally {
			httpd.stop();
		}
	}

	public void testPreferCache() throws Exception {
		File cached = new File("test/httpcache/7/http%3A%2F%2Flocalhost%3A18083%2Fbundles/dummybundle.jar");
		long cachedTimestamp = cached.lastModified();
		
		CachingURLResourceHandle handle = new CachingURLResourceHandle("http://localhost:18083/bundles/dummybundle.jar", "http://localhost:18083", new File("test/httpcache/7"), CachingMode.PreferCache);
		NanoHTTPD httpd = new NanoHTTPD(18083, new File("test/http"));
		try {
			File result = handle.request();
			assertEquals(cached, result);
			assertEquals("File timestamp should NOT change", cachedTimestamp, result.lastModified());
		} finally {
			httpd.stop();
		}
	}

}
