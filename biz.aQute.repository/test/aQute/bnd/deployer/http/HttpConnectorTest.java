package aQute.bnd.deployer.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLHandshakeException;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import aQute.bnd.service.url.TaggedData;
import aQute.lib.io.IO;
import junit.framework.TestCase;
import test.http.ETaggingResourceHandler;

public class HttpConnectorTest extends TestCase {

	private static final String	LOCALHOST		= "127.0.0.1";

	private static int			HTTP_PORT		= 0;
	private static int			HTTPS_PORT		= 0;

	private static final String	RESOURCE_BASE	= "testdata/http";
	private static final String	SECURED_PATH	= "/securebundles/*";

	private static final String	USER_ROLE_FILE	= "testdata/jetty-users.properties";
	private static final String	KEYSTORE_PATH	= "testdata/example.keystore";
	private static final String	KEYSTORE_PASS	= "opensesame";

	private static final String	EXPECTED_ETAG	= "64035a95";

	private static Server		jetty;

	private static String getUrl(boolean http) {
		if (http) {
			return "http://127.0.0.1:" + HTTP_PORT + "/";
		}
		return "https://127.0.0.1:" + HTTPS_PORT + "/";
	}

	@Override
	protected void setUp() throws Exception {
		File tmpFile = File.createTempFile("cache", ".tmp");
		tmpFile.deleteOnExit();

		jetty = startJetty();
	}

	@Override
	protected void tearDown() throws Exception {
		jetty.stop();
	}

	private static Server startJetty() throws Exception {
		Server server = new Server();

		// Create the login service
		String REQUIRED_ROLE = "users";
		HashLoginService loginSvc = new HashLoginService(REQUIRED_ROLE, USER_ROLE_FILE);
		server.addBean(loginSvc);

		// Start HTTP and HTTPS connectors
		SelectChannelConnector httpConnector = new SelectChannelConnector();
		httpConnector.setPort(0);
		httpConnector.setHost(LOCALHOST);
		server.addConnector(httpConnector);

		SslSelectChannelConnector sslConnector = new SslSelectChannelConnector();
		sslConnector.setPort(0);
		sslConnector.setHost(LOCALHOST);
		SslContextFactory contextFactory = sslConnector.getSslContextFactory();
		contextFactory.setKeyStorePath(KEYSTORE_PATH);
		contextFactory.setKeyStorePassword(KEYSTORE_PASS);
		server.addConnector(sslConnector);

		// Create the resource handler to serve files
		ResourceHandler resourceHandler = new ETaggingResourceHandler();
		resourceHandler.setResourceBase(RESOURCE_BASE);
		resourceHandler.setDirectoriesListed(true);

		// Setup user role constraints
		Constraint constraint = new Constraint();
		constraint.setName(Constraint.__BASIC_AUTH);
		constraint.setRoles(new String[] {
			REQUIRED_ROLE
		});
		constraint.setAuthenticate(true);

		// Map constraints to the secured directory
		ConstraintMapping cm = new ConstraintMapping();
		cm.setConstraint(constraint);
		cm.setPathSpec(SECURED_PATH);

		// Setup the constraint handler
		ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
		securityHandler.setAuthMethod("BASIC");
		securityHandler.setHandler(resourceHandler);
		securityHandler.setLoginService(loginSvc);
		securityHandler.setConstraintMappings(new ConstraintMapping[] {
			cm
		});

		// Finally!! Start the server
		server.setHandler(securityHandler);
		server.start();

		while (!server.isRunning()) {
			Thread.sleep(10);
		}

		HTTP_PORT = httpConnector.getLocalPort();
		HTTPS_PORT = sslConnector.getLocalPort();
		assertNotSame(Integer.valueOf(0), Integer.valueOf(HTTP_PORT));
		assertNotSame(Integer.valueOf(-1), Integer.valueOf(HTTP_PORT));
		assertNotSame(Integer.valueOf(0), Integer.valueOf(HTTPS_PORT));
		assertNotSame(Integer.valueOf(-1), Integer.valueOf(HTTPS_PORT));
		assertNotSame(Integer.valueOf(HTTP_PORT), Integer.valueOf(HTTPS_PORT));

		return server;
	}

	public static void testConnectTagged() throws Exception {
		DefaultURLConnector connector = new DefaultURLConnector();

		TaggedData data = connector.connectTagged(new URL(getUrl(true) + "bundles/dummybundle.jar"));
		assertNotNull("Data should be non-null because ETag not provided", data);
		data.getInputStream()
			.close();
		assertEquals("ETag is incorrect", EXPECTED_ETAG, data.getTag());
	}

	public static void testConnectKnownTag() throws Exception {
		DefaultURLConnector connector = new DefaultURLConnector();

		TaggedData data = connector.connectTagged(new URL(getUrl(true) + "bundles/dummybundle.jar"), EXPECTED_ETAG);
		assertNull("Data should be null since ETag not modified.", data);
	}

	public static void testConnectTagModified() throws Exception {
		DefaultURLConnector connector = new DefaultURLConnector();

		TaggedData data = connector.connectTagged(new URL(getUrl(true) + "bundles/dummybundle.jar"), "00000000");
		assertNotNull("Data should be non-null because ETag was different", data);
		data.getInputStream()
			.close();
		assertEquals("ETag is incorrect", EXPECTED_ETAG, data.getTag());
	}

	public static void testConnectHTTPS() throws Exception {
		DefaultURLConnector connector = new DefaultURLConnector();
		Map<String, String> config = new HashMap<>();
		config.put(HttpsUtil.PROP_DISABLE_SERVER_CERT_VERIFY, "true");
		connector.setProperties(config);

		InputStream stream = connector.connect(new URL(getUrl(false) + "bundles/dummybundle.jar"));
		assertNotNull(stream);
		stream.close();
	}

	public static void testConnectHTTPSBadCerficate() throws Exception {
		DefaultURLConnector connector = new DefaultURLConnector();

		InputStream stream = null;
		try {
			stream = connector.connect(new URL(getUrl(false) + "bundles/dummybundle.jar"));
			fail("Expected SSLHandsakeException");
		} catch (SSLHandshakeException e) {
			// expected
		} finally {
			if (stream != null)
				IO.close(stream);
		}
	}

	public static void testConnectTaggedHTTPS() throws Exception {
		DefaultURLConnector connector = new DefaultURLConnector();
		Map<String, String> config = new HashMap<>();
		config.put(HttpsUtil.PROP_DISABLE_SERVER_CERT_VERIFY, "true");
		connector.setProperties(config);

		TaggedData data = connector.connectTagged(new URL(getUrl(false) + "bundles/dummybundle.jar"));
		assertNotNull(data);
		data.getInputStream()
			.close();
	}

	public static void testConnectTaggedHTTPSBadCerficate() throws Exception {
		DefaultURLConnector connector = new DefaultURLConnector();

		InputStream stream = null;
		try {
			connector.connectTagged(new URL(getUrl(false) + "bundles/dummybundle.jar"));
			fail("Expected SSLHandsakeException");
		} catch (SSLHandshakeException e) {
			// expected
		} finally {
			if (stream != null)
				IO.close(stream);
		}
	}

	public static void testConnectNoUserPass() throws Exception {
		HttpBasicAuthURLConnector connector = new HttpBasicAuthURLConnector();
		Map<String, String> config = new HashMap<>();
		config.put("configs", "");
		connector.setProperties(config);

		try {
			connector.connect(new URL(getUrl(true) + "securebundles/dummybundle.jar"));
			fail("Should have thrown IOException due to missing auth");
		} catch (IOException e) {
			// expected
			assertTrue(e.getMessage()
				.startsWith("Server returned HTTP response code: 401"));
		}
	}

	public static void testConnectWithUserPass() throws Exception {
		HttpBasicAuthURLConnector connector = new HttpBasicAuthURLConnector();
		Map<String, String> config = new HashMap<>();
		config.put("configs", "testdata/http_auth.properties");
		connector.setProperties(config);

		InputStream stream = connector.connect(new URL(getUrl(true) + "securebundles/dummybundle.jar"));
		assertNotNull(stream);
		stream.close();
	}

	public static void testConnectHTTPSBadCertificate() throws Exception {
		HttpBasicAuthURLConnector connector = new HttpBasicAuthURLConnector();
		Map<String, String> config = new HashMap<>();
		config.put("configs", "testdata/http_auth.properties");
		connector.setProperties(config);

		try {
			connector.connect(new URL(getUrl(false) + "securebundles/dummybundle.jar"));
			fail("Should have thrown error: invalid server certificate");
		} catch (IOException e) {
			// expected
			assertTrue(e instanceof SSLHandshakeException);
		}
	}

	public static void testConnectWithUserPassHTTPS() throws Exception {
		HttpBasicAuthURLConnector connector = new HttpBasicAuthURLConnector();
		Map<String, String> config = new HashMap<>();
		config.put("configs", "testdata/http_auth.properties");
		config.put(HttpsUtil.PROP_DISABLE_SERVER_CERT_VERIFY, "true");
		connector.setProperties(config);

		InputStream stream = connector.connect(new URL(getUrl(false) + "securebundles/dummybundle.jar"));
		assertNotNull(stream);
		stream.close();
	}

	public static void testConnectWithWrongUserPass() throws Exception {
		HttpBasicAuthURLConnector connector = new HttpBasicAuthURLConnector();
		Map<String, String> config = new HashMap<>();
		config.put("configs", "testdata/http_auth_wrong.properties");
		connector.setProperties(config);

		try {
			connector.connect(new URL(getUrl(true) + "securebundles/dummybundle.jar"));
			fail("Should have thrown IOException due to incorrect auth");
		} catch (IOException e) {
			// expected
			assertTrue(e.getMessage()
				.startsWith("Server returned HTTP response code: 401"));
		}
	}

	public static void testConnectWithWrongUserPassHTTPS() throws Exception {
		HttpBasicAuthURLConnector connector = new HttpBasicAuthURLConnector();
		Map<String, String> config = new HashMap<>();
		config.put("configs", "testdata/http_auth_wrong.properties");
		config.put(HttpsUtil.PROP_DISABLE_SERVER_CERT_VERIFY, "true");
		connector.setProperties(config);

		try {
			connector.connect(new URL(getUrl(false) + "securebundles/dummybundle.jar"));
			fail("Should have thrown IOException due to incorrect auth");
		} catch (IOException e) {
			// expected
			assertTrue(e.getMessage()
				.startsWith("Server returned HTTP response code: 401"));
		}
	}

	public static void testConnectWithUserPassAndTag() throws Exception {
		HttpBasicAuthURLConnector connector = new HttpBasicAuthURLConnector();
		Map<String, String> config = new HashMap<>();
		config.put("configs", "testdata/http_auth.properties");
		connector.setProperties(config);

		TaggedData data = connector.connectTagged(new URL(getUrl(true) + "securebundles/dummybundle.jar"),
			EXPECTED_ETAG);
		assertNull("Data should be null because resource not modified", data);
	}

}
