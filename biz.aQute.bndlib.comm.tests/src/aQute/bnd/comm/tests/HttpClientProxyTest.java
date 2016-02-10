package aQute.bnd.comm.tests;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy.Type;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.ProxyAuthenticator;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import aQute.bnd.connection.settings.ConnectionSettings;
import aQute.bnd.connection.settings.ProxyDTO;
import aQute.bnd.connection.settings.ServerDTO;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.url.TaggedData;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import biz.aQute.http.testservers.HttpTestServer;
import biz.aQute.http.testservers.Httpbin;
import junit.framework.TestCase;
import sockslib.common.AuthenticationException;
import sockslib.common.Credentials;
import sockslib.common.methods.UsernamePasswordMethod;
import sockslib.server.Session;
import sockslib.server.SocksProxyServer;
import sockslib.server.SocksServerBuilder;
import sockslib.server.UsernamePasswordAuthenticator;
import sockslib.server.listener.CloseSessionException;
import sockslib.server.listener.SessionListener;
import sockslib.server.manager.MemoryBasedUserManager;
import sockslib.server.manager.User;
import sockslib.server.manager.UserManager;
import sockslib.server.msg.CommandMessage;

/**
 * This test verifies the HttpClient against a Http Proxy and a SOCKS 5 proxy.
 * Different combinations are tried out with a secure and unsecure server.
 * 
 * See https://github.com/fengyouchao/sockslib for socks
 */
public class HttpClientProxyTest extends TestCase {

	public void testPromiscuousProxyWithNoUser() throws Exception {
		createPromiscuousHttpProxy();
		createUnsecureServer();
		assertHttpProxy(null, false);
	}

	public void testPromiscuousProxyWithBadUser() throws Exception {
		createPromiscuousHttpProxy();
		createUnsecureServer();
		assertHttpProxy("bad", false);
	}

	public void testAuthenticatingProxyWithGoodUser() throws Exception {
		createAuthenticationHttpProxy();
		createUnsecureServer();

		assertHttpProxy("good", true);
	}

	public void testPromiscuousProxyWithGoodUser() throws Exception {
		createPromiscuousHttpProxy();
		createUnsecureServer();
		assertHttpProxy("good", false);
	}

	public void testNoProxyUnsecure() throws Exception {
		createUnsecureServer();
		assertHttpProxy("good", false);
	}

	public void testNoProxySecure() throws Exception {
		createUnsecureServer();
		assertHttpProxy("good", false);
	}

	public void testPromiscuousProxyWithGoodUserSecure() throws Exception {
		createPromiscuousHttpProxy();
		createSecureServer();
		assertHttpProxy("good", false);
	}

	public void testAuthenticatingProxyNoUser() throws Exception {
		try {
			createAuthenticationHttpProxy();
			createUnsecureServer();

			assertHttpProxy(null, true);
			fail("Expected authentication failure");
		} catch (Exception e) {
			// ok
		}
	}

	public void testAuthenticatingProxyBadUser() throws Exception {
		try {
			createAuthenticationHttpProxy();
			createUnsecureServer();

			assertHttpProxy("bad", true);
			fail("Expected authentication failure");
		} catch (Exception e) {
			// ok
		}
	}

	public void testSecureSocksAuthenticatingWithGoodUserSecure() throws Exception {
		createSecureSocks5();
		createSecureServer();
		assertSocks5Proxy("good", true);
	}

	public void testSecureSocksAuthenticatingWithBadUserSecure() throws Exception {
		try {
			createSecureSocks5();
			createSecureServer();
			assertSocks5Proxy("bad", true);
			fail("Expected the transfer to fail");
		} catch (Exception e) {
			// ok
		}
	}

	public void testSecureSocksAuthenticatingWithGoodUser() throws Exception {
		createSecureSocks5();
		createUnsecureServer();
		assertSocks5Proxy("good", true);
	}

	public void testSecureSocksAuthenticatingWithBadUser() throws Exception {
		try {
			createSecureSocks5();
			createUnsecureServer();
			assertSocks5Proxy("bad", true);
			fail("Expected the transfer to fail");
		} catch (Exception e) {
			// ok
		}
	}

	/*
	 * 
	 */

	private HttpProxyServer				httpProxy;
	private AtomicBoolean				authenticationCalled	= new AtomicBoolean();
	private HttpTestServer				httpTestServer;
	private SocksProxyServer			socks5Proxy;
	private static int					httpProxyPort			= 2080;
	private static int					socksProxyPort			= 3080;
	private AtomicReference<Throwable>	exception				= new AtomicReference<>();
	private AtomicInteger				created					= new AtomicInteger();

	
	// we use different ports because the servers seem to linger

	void createAuthenticationHttpProxy() {
		HttpProxyServerBootstrap bootstrap = DefaultHttpProxyServer.bootstrap().withPort(++httpProxyPort);
		bootstrap.withProxyAuthenticator(new ProxyAuthenticator() {

			@Override
			public boolean authenticate(String user, String password) {
				System.out.println("Authenticating " + user + " : " + password);
				authenticationCalled.set(true);
				return "proxyuser".equals(user) && "good".equals(password);

			}
		});
		httpProxy = bootstrap.start();
	}

	void createPromiscuousHttpProxy() {
		HttpProxyServerBootstrap bootstrap = DefaultHttpProxyServer.bootstrap().withPort(++httpProxyPort);
		httpProxy = bootstrap.start();
	}

	void createSecureServer() throws Exception {
		Httpbin.Config config = new Httpbin.Config();
		config.https = true;
		httpTestServer = new Httpbin(config);
		httpTestServer.start();
	}

	void createUnsecureServer() throws Exception {
		Httpbin.Config config = new Httpbin.Config();
		config.https = false;
		httpTestServer = new Httpbin(config);
		httpTestServer.start();
	}

	private void createSecureSocks5() throws IOException, InterruptedException {
		UserManager userManager = new MemoryBasedUserManager();
		userManager.create(new User("proxyuser", "good"));
		SocksServerBuilder builder = SocksServerBuilder.newSocks5ServerBuilder();
		builder.setBindPort(++socksProxyPort);
		builder.setUserManager(userManager);
		builder.addSocksMethods(new UsernamePasswordMethod(new UsernamePasswordAuthenticator() {
			@Override
			public void doAuthenticate(Credentials arg0, Session arg1) throws AuthenticationException {
				System.out.println("Authenticating"); // does not get called?
				super.doAuthenticate(arg0, arg1);
				authenticationCalled.set(true);

			}
		}));
		socks5Proxy = builder.build();

		socks5Proxy.getSessionManager().addSessionListener("abc", new SessionListener() {

			@Override
			public void onException(Session arg0, Exception arg1) {
				System.err.println("Exception " + arg0 + " " + arg1);
				arg1.printStackTrace();
				exception.set(arg1);
			}

			@Override
			public void onCommand(Session arg0, CommandMessage arg1) throws CloseSessionException {
				System.err.println("Command " + arg0 + " " + arg1);
			}

			@Override
			public void onClose(Session arg0) {
				System.err.println("Close " + arg0);
			}

			@Override
			public void onCreate(Session arg0) throws CloseSessionException {
				System.err.println("Create " + arg0);
				created.incrementAndGet();
			}
		});

		socks5Proxy.start();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		if (httpProxy != null)
			httpProxy.abort();

		if (httpTestServer != null)
			httpTestServer.close();

		if (socks5Proxy != null)
			socks5Proxy.shutdown();
	}

	void assertHttpProxy(String password, boolean authenticationCalled) throws MalformedURLException, Exception {
		Processor p = new Processor();
		p.setProperty("-connectionsettings", "" + false);
		ConnectionSettings cs = new ConnectionSettings(p);

		HttpClient hc = new HttpClient(p);

		if (httpProxy != null) {
			ProxyDTO proxy = new ProxyDTO();
			proxy.active = true;
			proxy.host = "localhost";
			proxy.port = httpProxyPort;
			proxy.protocol = Type.HTTP;
			if (password != null) {
				proxy.username = "proxyuser";
				proxy.password = password;
			}
			p.addBasicPlugin(ConnectionSettings.createProxyHandler(proxy));
		}

		ServerDTO server = new ServerDTO();
		server.id = httpTestServer.getBaseURI().toString();
		server.verify = false;
		server.trust = Strings.join(httpTestServer.getTrustedCertificateFiles(IO.getFile("generated")));
		cs.add( server );

		URL url = new URL(httpTestServer.getBaseURI() + "/get-tag/ABCDEFGH");
		TaggedData tag = hc.connectTagged(url);
		assertNotNull(tag);
		assertEquals("ABCDEFGH", tag.getTag());
		String s = IO.collect(tag.getInputStream());
		assertNotNull(s);
		assertTrue(s.trim().startsWith("{"));
		assertTrue(this.authenticationCalled.get() == authenticationCalled);

	}

	void assertSocks5Proxy(String password, boolean authenticationCalled) throws MalformedURLException, Exception {
		Processor p = new Processor();
		p.setProperty("-connectionsettings", "" + false);
		ConnectionSettings cs = new ConnectionSettings(p);

		HttpClient hc = new HttpClient(p);

		if (socks5Proxy != null) {
			ProxyDTO proxy = new ProxyDTO();
			proxy.active = true;
			proxy.host = "localhost";
			proxy.port = socksProxyPort;
			proxy.protocol = Type.SOCKS;
			if (password != null) {
				proxy.username = "proxyuser";
				proxy.password = password;
			}
			p.addBasicPlugin(ConnectionSettings.createProxyHandler(proxy));
		}

		ServerDTO server = new ServerDTO();
		server.id = httpTestServer.getBaseURI().toString();
		server.verify = false;
		server.trust = Strings.join(httpTestServer.getTrustedCertificateFiles(IO.getFile("generated")));
		cs.add(server);

		URL url = new URL(httpTestServer.getBaseURI() + "/get-tag/ABCDEFGH");
		TaggedData tag = hc.connectTagged(url);
		assertNotNull(tag);
		assertEquals("ABCDEFGH", tag.getTag());
		String s = IO.collect(tag.getInputStream());
		assertNotNull(s);
		assertTrue(s.trim().startsWith("{"));
		// assertTrue(this.authenticationCalled.get() == authenticationCalled);

	}

}
