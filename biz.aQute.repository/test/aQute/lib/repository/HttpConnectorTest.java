package aQute.lib.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import aQute.bnd.service.url.TaggedData;

public class HttpConnectorTest extends TestCase {

	private static final int HTTP_PORT = 18081;
	private static final int AUTH_HTTP_PORT = 18082;
	
	private static final String URL_PREFIX = "http://127.0.0.1:" + HTTP_PORT + "/";
	private static final String AUTH_URL_PREFIX = "http://127.0.0.1:" + AUTH_HTTP_PORT + "/";

	private NanoHTTPD httpd;

	@Override
	protected void setUp() throws Exception {
		File tmpFile = File.createTempFile("cache", ".tmp");
		tmpFile.deleteOnExit();

		httpd = new NanoHTTPD(HTTP_PORT, new File("test/http"));
	}

	@Override
	protected void tearDown() throws Exception {
		httpd.stop();
	}

	public void testConnectTagged() throws Exception {
		DefaultURLConnector connector = new DefaultURLConnector();

		TaggedData data = connector.connectTagged(new URL(URL_PREFIX + "bundles/dummybundle.jar"));
		assertNotNull("Data should be non-null because ETag not provided", data);
		data.getInputStream().close();
		assertEquals("ETag is incorrect", "d5785fff", data.getTag());
	}
	
	public void testConnectKnownTag() throws Exception {
		DefaultURLConnector connector = new DefaultURLConnector();

		TaggedData data = connector.connectTagged(new URL(URL_PREFIX + "bundles/dummybundle.jar"), "d5785fff");
		assertNull("Data should be null since ETag not modified.", data);
	}

	public void testConnectTagModified() throws Exception {
		DefaultURLConnector connector = new DefaultURLConnector();

		TaggedData data = connector.connectTagged(new URL(URL_PREFIX + "bundles/dummybundle.jar"), "00000000");
		assertNotNull("Data should be non-null because ETag was different", data);
		data.getInputStream().close();
		assertEquals("ETag is incorrect", "d5785fff", data.getTag());
	}
	

}
