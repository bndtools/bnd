package test.http;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.net.ssl.*;

import junit.framework.*;

import org.eclipse.jetty.security.*;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.server.nio.*;
import org.eclipse.jetty.server.ssl.*;
import org.eclipse.jetty.util.security.*;
import org.eclipse.jetty.util.ssl.*;

import aQute.bnd.deployer.http.*;
import aQute.bnd.service.url.*;
import aQute.lib.io.*;

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

	private Server				jetty;

	private String getUrl(boolean http) {
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

	private Server startJetty() throws Exception {
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

		HTTP_PORT = httpConnector.getLocalPort();
		HTTPS_PORT = sslConnector.getLocalPort();
		assertNotSame(new Integer(0), new Integer(HTTP_PORT));
		assertNotSame(new Integer(-1), new Integer(HTTP_PORT));
		assertNotSame(new Integer(0), new Integer(HTTPS_PORT));
		assertNotSame(new Integer(-1), new Integer(HTTPS_PORT));

		return server;
	}

	public void testConnectTagged() throws Exception {
		DefaultURLConnector connector = new DefaultURLConnector();

		TaggedData data = connector.connectTagged(new URL(getUrl(true) + "bundles/dummybundle.jar"));
		assertNotNull("Data should be non-null because ETag not provided", data);
		data.getInputStream().close();
		assertEquals("ETag is incorrect", EXPECTED_ETAG, data.getTag());
	}

	public void testConnectKnownTag() throws Exception {
		DefaultURLConnector connector = new DefaultURLConnector();

		TaggedData data = connector.connectTagged(new URL(getUrl(true) + "bundles/dummybundle.jar"), EXPECTED_ETAG);
		assertNull("Data should be null since ETag not modified.", data);
	}

	public void testConnectTagModified() throws Exception {
		DefaultURLConnector connector = new DefaultURLConnector();

		TaggedData data = connector.connectTagged(new URL(getUrl(true) + "bundles/dummybundle.jar"), "00000000");
		assertNotNull("Data should be non-null because ETag was different", data);
		data.getInputStream().close();
		assertEquals("ETag is incorrect", EXPECTED_ETAG, data.getTag());
	}

	public void testConnectHTTPS() throws Exception {
		DefaultURLConnector connector = new DefaultURLConnector();
		Map<String,String> config = new HashMap<String,String>();
		config.put("disableServerVerify", "true");
		connector.setProperties(config);

		InputStream stream = connector.connect(new URL(getUrl(false) + "bundles/dummybundle.jar"));
		assertNotNull(stream);
		stream.close();
	}

	public void testConnectHTTPSBadCerficate() throws Exception {
		DefaultURLConnector connector = new DefaultURLConnector();

		InputStream stream = null;
		try {
			stream = connector.connect(new URL(getUrl(false) + "bundles/dummybundle.jar"));
			fail("Expected SSLHandsakeException");
		}
		catch (SSLHandshakeException e) {
			// expected
		}
		finally {
			if (stream != null)
				IO.close(stream);
		}
	}

	public void testConnectTaggedHTTPS() throws Exception {
		DefaultURLConnector connector = new DefaultURLConnector();
		Map<String,String> config = new HashMap<String,String>();
		config.put("disableServerVerify", "true");
		connector.setProperties(config);

		TaggedData data = connector.connectTagged(new URL(getUrl(false) + "bundles/dummybundle.jar"));
		assertNotNull(data);
		data.getInputStream().close();
	}

	public void testConnectTaggedHTTPSBadCerficate() throws Exception {
		DefaultURLConnector connector = new DefaultURLConnector();

		InputStream stream = null;
		try {
			connector.connectTagged(new URL(getUrl(false) + "bundles/dummybundle.jar"));
			fail("Expected SSLHandsakeException");
		}
		catch (SSLHandshakeException e) {
			// expected
		}
		finally {
			if (stream != null)
				IO.close(stream);
		}
	}

	public void testConnectNoUserPass() throws Exception {
		HttpBasicAuthURLConnector connector = new HttpBasicAuthURLConnector();
		Map<String,String> config = new HashMap<String,String>();
		config.put("configs", "");
		connector.setProperties(config);

		try {
			connector.connect(new URL(getUrl(true) + "securebundles/dummybundle.jar"));
			fail("Should have thrown IOException due to missing auth");
		}
		catch (IOException e) {
			// expected
			assertTrue(e.getMessage().startsWith("Server returned HTTP response code: 401"));
		}
	}

	public void testConnectWithUserPass() throws Exception {
		HttpBasicAuthURLConnector connector = new HttpBasicAuthURLConnector();
		Map<String,String> config = new HashMap<String,String>();
		config.put("configs", "testdata/http_auth.properties");
		connector.setProperties(config);

		InputStream stream = connector.connect(new URL(getUrl(true) + "securebundles/dummybundle.jar"));
		assertNotNull(stream);
	}

	public void testConnectHTTPSBadCertificate() throws Exception {
		HttpBasicAuthURLConnector connector = new HttpBasicAuthURLConnector();
		Map<String,String> config = new HashMap<String,String>();
		config.put("configs", "testdata/http_auth.properties");
		connector.setProperties(config);

		try {
			connector.connect(new URL(getUrl(false) + "securebundles/dummybundle.jar"));
			fail("Should have thrown error: invalid server certificate");
		}
		catch (IOException e) {
			// expected
			assertTrue(e instanceof SSLHandshakeException);
		}
	}

	public void testConnectWithUserPassHTTPS() throws Exception {
		HttpBasicAuthURLConnector connector = new HttpBasicAuthURLConnector();
		Map<String,String> config = new HashMap<String,String>();
		config.put("configs", "testdata/http_auth.properties");
		config.put("disableServerVerify", "true");
		connector.setProperties(config);

		try {
			InputStream stream = connector.connect(new URL(getUrl(false) + "securebundles/dummybundle.jar"));
			assertNotNull(stream);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	public void testConnectWithWrongUserPass() throws Exception {
		HttpBasicAuthURLConnector connector = new HttpBasicAuthURLConnector();
		Map<String,String> config = new HashMap<String,String>();
		config.put("configs", "testdata/http_auth_wrong.properties");
		connector.setProperties(config);

		try {
			connector.connect(new URL(getUrl(true) + "securebundles/dummybundle.jar"));
			fail("Should have thrown IOException due to incorrect auth");
		}
		catch (IOException e) {
			// expected
			assertTrue(e.getMessage().startsWith("Server returned HTTP response code: 401"));
		}
	}

	public void testConnectWithWrongUserPassHTTPS() throws Exception {
		HttpBasicAuthURLConnector connector = new HttpBasicAuthURLConnector();
		Map<String,String> config = new HashMap<String,String>();
		config.put("configs", "testdata/http_auth_wrong.properties");
		config.put("disableServerVerify", "true");
		connector.setProperties(config);

		try {
			connector.connect(new URL(getUrl(false) + "securebundles/dummybundle.jar"));
			fail("Should have thrown IOException due to incorrect auth");
		}
		catch (IOException e) {
			// expected
			assertTrue(e.getMessage().startsWith("Server returned HTTP response code: 401"));
		}
	}

	public void testConnectWithUserPassAndTag() throws Exception {
		HttpBasicAuthURLConnector connector = new HttpBasicAuthURLConnector();
		Map<String,String> config = new HashMap<String,String>();
		config.put("configs", "testdata/http_auth.properties");
		connector.setProperties(config);

		TaggedData data = connector.connectTagged(new URL(getUrl(true) + "securebundles/dummybundle.jar"),
				EXPECTED_ETAG);
		assertNull("Data should be null because resource not modified", data);
	}

}
