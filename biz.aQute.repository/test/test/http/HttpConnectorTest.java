package test.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import test.lib.AuthNanoHTTPD;
import test.lib.NanoHTTPD;

import junit.framework.TestCase;
import aQute.bnd.service.url.TaggedData;
import aQute.lib.deployer.http.HttpBasicAuthURLConnector;
import aQute.lib.deployer.repository.DefaultURLConnector;

public class HttpConnectorTest extends TestCase {

	private static final int HTTP_PORT      = 18081;
	private static final int AUTH_HTTP_PORT = 18082;
	
	private static final String URL_PREFIX      = "http://127.0.0.1:18081/";
	private static final String AUTH_URL_PREFIX = "http://127.0.0.1:18082/";

	private NanoHTTPD httpd;

	@Override
	protected void setUp() throws Exception {
		File tmpFile = File.createTempFile("cache", ".tmp");
		tmpFile.deleteOnExit();

		httpd = new NanoHTTPD(HTTP_PORT, new File("testdata/http"));
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
		assertEquals("ETag is incorrect", "281e8cff", data.getTag());
	}
	
	public void testConnectKnownTag() throws Exception {
		DefaultURLConnector connector = new DefaultURLConnector();

		TaggedData data = connector.connectTagged(new URL(URL_PREFIX + "bundles/dummybundle.jar"), "281e8cff");
		assertNull("Data should be null since ETag not modified.", data);
	}

	public void testConnectTagModified() throws Exception {
		DefaultURLConnector connector = new DefaultURLConnector();

		TaggedData data = connector.connectTagged(new URL(URL_PREFIX + "bundles/dummybundle.jar"), "00000000");
		assertNotNull("Data should be non-null because ETag was different", data);
		data.getInputStream().close();
		assertEquals("ETag is incorrect", "281e8cff", data.getTag());
	}
	
	public void testConnectNoUserPass() throws Exception {
		AuthNanoHTTPD authHttpd = new AuthNanoHTTPD(AUTH_HTTP_PORT, new File("testdata/http"), "Ali Baba", "open sesame");
		
		HttpBasicAuthURLConnector connector = new HttpBasicAuthURLConnector();
		Map<String, String> config = new HashMap<String, String>();
		config.put("configs", "");
		connector.setProperties(config);
		
		try {
			connector.connect(new URL(AUTH_URL_PREFIX + "bundles/dummybundle.jar"));
			fail("Should have thrown IOException due to missing auth");
		} catch (IOException e) {
			// expected
		} finally {
			authHttpd.stop();
		}
	}
	
	public void testConnectWithUserPass() throws Exception {
		AuthNanoHTTPD authHttpd = new AuthNanoHTTPD(AUTH_HTTP_PORT, new File("testdata/http"), "Ali Baba", "open sesame");
		
		HttpBasicAuthURLConnector connector = new HttpBasicAuthURLConnector();
		Map<String, String> config = new HashMap<String, String>();
		config.put("configs", "testdata/http_auth.properties");
		connector.setProperties(config);
		
		try {
			InputStream stream = connector.connect(new URL(AUTH_URL_PREFIX + "bundles/dummybundle.jar"));
			assertNotNull(stream);
			stream.close();
		} finally {
			authHttpd.stop();
		}
	}
	
	public void testConnectWithWrongUserPass() throws Exception {
		AuthNanoHTTPD authHttpd = new AuthNanoHTTPD(AUTH_HTTP_PORT, new File("testdata/http"), "Ali Baba", "let me in");
		
		HttpBasicAuthURLConnector connector = new HttpBasicAuthURLConnector();
		Map<String, String> config = new HashMap<String, String>();
		config.put("configs", "testdata/http_auth.properties");
		connector.setProperties(config);
		
		try {
			connector.connect(new URL(AUTH_URL_PREFIX + "bundles/dummybundle.jar"));
			fail("Should have thrown IOException due to incorrect auth");
		} catch (IOException e) {
			// expected
		} finally {
			authHttpd.stop();
		}
	}
	
	public void testConnectWithUserPassAndTag() throws Exception {
		AuthNanoHTTPD authHttpd = new AuthNanoHTTPD(AUTH_HTTP_PORT, new File("testdata/http"), "Ali Baba", "open sesame");
		
		HttpBasicAuthURLConnector connector = new HttpBasicAuthURLConnector();
		Map<String, String> config = new HashMap<String, String>();
		config.put("configs", "testdata/http_auth.properties");
		connector.setProperties(config);
		
		try {
			TaggedData data = connector.connectTagged(new URL(AUTH_URL_PREFIX + "bundles/dummybundle.jar"), "281e8cff");
			assertNull("Data should be null because resource not modified", data);
		} finally {
			authHttpd.stop();
		}
	}
	

}
