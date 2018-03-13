package aQute.bnd.comm.tests;

import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.Proxy.Type;
import java.net.URI;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSource;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.MitmManager;
import org.littleshoot.proxy.ProxyAuthenticator;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import aQute.bnd.connection.settings.ConnectionSettings;
import aQute.bnd.connection.settings.ProxyDTO;
import aQute.bnd.connection.settings.ServerDTO;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.url.State;
import aQute.bnd.service.url.TaggedData;
import aQute.http.testservers.HttpTestServer;
import aQute.http.testservers.Httpbin;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContextBuilder;
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
 * Different combinations are tried out with a secure and unsecure server. See
 * https://github.com/fengyouchao/sockslib for socks
 */
public class HttpClientProxyTest extends TestCase {

	private final class ProxyCheckingFilter implements HttpFiltersSource {

		private boolean secure;

		public ProxyCheckingFilter(boolean secure) {
			this.secure = secure;
		}

		@Override
		public int getMaximumResponseBufferSizeInBytes() {
			return 0;
		}

		@Override
		public int getMaximumRequestBufferSizeInBytes() {
			return 0;
		}

		@Override
		public HttpFilters filterRequest(HttpRequest arg0, ChannelHandlerContext arg1) {
			return new HttpFiltersAdapter(arg0, arg1) {

				@Override
				public HttpResponse clientToProxyRequest(HttpObject obj) {

					String scheme = URI.create(originalRequest.uri())
						.getScheme();

					if (!secure && "http".equalsIgnoreCase(scheme)) {
						// Insecure using HTTP
						proxyCalled.set(true);
						return null;
					} else if (secure && !"http".equalsIgnoreCase(scheme)) {
						// The scheme gets eaten when we use SSL, so we just
						// have to check that it's not HTTP
						proxyCalled.set(true);
						return null;
					} else {
						return new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
					}
				}
			};
		}
	}

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
		assertHttpProxy("good", Type.HTTP.name(), false, false);
	}

	public void testNoProxySecure() throws Exception {
		createUnsecureServer();
		assertHttpProxy("good", Type.HTTP.name(), false, false);
	}

	public void testPromiscuousProxyWithGoodUserSecure() throws Exception {
		createSecureServer();
		createSecurePromiscuousHttpProxy();
		assertHttpProxy("good", "HTTPS", true, false);
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
	private AtomicBoolean				proxyCalled				= new AtomicBoolean();
	private HttpTestServer				httpTestServer;
	private SocksProxyServer			socks5Proxy;
	private static int					httpProxyPort			= 2080;
	private static int					socksProxyPort			= 3080;
	private AtomicReference<Throwable>	exception				= new AtomicReference<>();
	private AtomicInteger				created					= new AtomicInteger();

	// we use different ports because the servers seem to linger

	void createAuthenticationHttpProxy() {
		HttpProxyServerBootstrap bootstrap = DefaultHttpProxyServer.bootstrap()
			.withPort(++httpProxyPort)
			.withProxyAuthenticator(new ProxyAuthenticator() {

				@Override
				public boolean authenticate(String user, String password) {
					System.out.println("Authenticating " + user + " : " + password);
					authenticationCalled.set(true);
					return "proxyuser".equals(user) && "good".equals(password);

				}

				@Override
				public String getRealm() {
					return null;
				}
			})
			.withFiltersSource(new ProxyCheckingFilter(false));
		httpProxy = bootstrap.start();
	}

	void createPromiscuousHttpProxy() {
		HttpProxyServerBootstrap bootstrap = DefaultHttpProxyServer.bootstrap()
			.withPort(++httpProxyPort)
			.withFiltersSource(new ProxyCheckingFilter(false));
		httpProxy = bootstrap.start();
	}

	void createSecurePromiscuousHttpProxy() {
		HttpProxyServerBootstrap bootstrap = DefaultHttpProxyServer.bootstrap()
			.withPort(++httpProxyPort)
			.withManInTheMiddle(new MitmManager() {

				SecureRandom random = new SecureRandom();

				@Override
				public SSLEngine serverSslEngine(String arg0, int arg1) {
					return serverSslEngine();
				}

				@Override
				public SSLEngine serverSslEngine() {
					try {
						return SslContextBuilder.forClient()
							.trustManager(httpTestServer.getCertificateChain())
							.build()
							.newEngine(UnpooledByteBufAllocator.DEFAULT);
					} catch (SSLException e) {
						throw new RuntimeException(e);
					}
				}

				@SuppressWarnings("restriction")
				private X509Certificate[] createSelfSignedCertifcate(KeyPair keyPair) throws Exception {
					X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
					nameBuilder.addRDN(BCStyle.CN, "localhost");

					Date notBefore = new Date();
					Date notAfter = new Date(System.currentTimeMillis() + 24 * 3 * 60 * 60 * 1000);

					BigInteger serialNumber = new BigInteger(128, random);

					X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(nameBuilder.build(),
						serialNumber, notBefore, notAfter, nameBuilder.build(), keyPair.getPublic());
					ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
						.build(keyPair.getPrivate());
					X509Certificate certificate = new JcaX509CertificateConverter()
						.getCertificate(certificateBuilder.build(contentSigner));
					return new X509Certificate[] {
						certificate
					};
				}

				private KeyPair createKey() throws NoSuchAlgorithmException {
					KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
					keyGen.initialize(2048, random);
					KeyPair pair = keyGen.generateKeyPair();
					return pair;
				}

				@Override
				public SSLEngine clientSslEngineFor(HttpRequest arg0, SSLSession arg1) {
					try {
						KeyPair keyPair = createKey();
						X509Certificate[] selfSignedCertifcate = createSelfSignedCertifcate(keyPair);

						return SslContextBuilder.forServer(keyPair.getPrivate(), selfSignedCertifcate)
							.build()
							.newEngine(UnpooledByteBufAllocator.DEFAULT);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			})
			.withFiltersSource(new ProxyCheckingFilter(true));
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
		SocksServerBuilder builder = SocksServerBuilder.newSocks5ServerBuilder()
			.setBindPort(++socksProxyPort)
			.addSocksMethods(new UsernamePasswordMethod(new UsernamePasswordAuthenticator(userManager) {
				@Override
				public void doAuthenticate(Credentials arg0, Session arg1) throws AuthenticationException {
					System.out.println("Authenticating"); // does not get
															// called?
					super.doAuthenticate(arg0, arg1);
					authenticationCalled.set(true);

				}
			}));
		socks5Proxy = builder.build();

		socks5Proxy.getSessionManager()
			.addSessionListener("abc", new SessionListener() {

				@Override
				public void onException(Session arg0, Exception arg1) {
					System.err.println("Exception " + arg0 + " " + arg1);
					arg1.printStackTrace();
					exception.set(arg1);
				}

				@Override
				public void onCommand(Session arg0, CommandMessage arg1) throws CloseSessionException {
					proxyCalled.set(true);
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

		proxyCalled.set(false);
		authenticationCalled.set(false);

		if (httpProxy != null)
			httpProxy.abort();

		if (httpTestServer != null)
			httpTestServer.close();

		if (socks5Proxy != null)
			socks5Proxy.shutdown();
	}

	void assertHttpProxy(String password, boolean authenticationCalled) throws MalformedURLException, Exception {
		assertHttpProxy(password, Type.HTTP.name(), true, authenticationCalled);
	}

	@SuppressWarnings("resource")
	void assertHttpProxy(String password, String protocol, boolean proxyCalled, boolean authenticationCalled)
		throws MalformedURLException, Exception {
		Processor p = new Processor();
		p.setProperty("-connectionsettings", "" + false);
		HttpClient hc = new HttpClient();
		ConnectionSettings cs = new ConnectionSettings(p, hc);

		if (httpProxy != null) {
			ProxyDTO proxy = new ProxyDTO();
			proxy.active = true;
			proxy.host = "localhost";
			proxy.port = httpProxyPort;
			proxy.protocol = protocol;
			if (password != null) {
				proxy.username = "proxyuser";
				proxy.password = password;
			}
			hc.addProxyHandler(ConnectionSettings.createProxyHandler(proxy));

			if ("HTTPS".equals(protocol)) {

			}
		}

		ServerDTO server = new ServerDTO();
		server.id = httpTestServer.getBaseURI()
			.toString();
		server.verify = false;
		server.trust = Strings.join(httpTestServer.getTrustedCertificateFiles(IO.getFile("generated")));
		cs.add(server);

		URL url = new URL(httpTestServer.getBaseURI() + "/get-tag/ABCDEFGH");
		TaggedData tag = hc.connectTagged(url);
		assertNotNull(tag);
		if (tag.getState() != State.OTHER)
			assertEquals("ABCDEFGH", tag.getTag());
		String s = IO.collect(tag.getInputStream());
		assertNotNull(s);
		assertTrue(s.trim()
			.startsWith("{"));
		assertEquals(proxyCalled, this.proxyCalled.get());
		assertEquals(authenticationCalled, this.authenticationCalled.get());

	}

	@SuppressWarnings("resource")
	void assertSocks5Proxy(String password, boolean authenticationCalled) throws MalformedURLException, Exception {
		Processor p = new Processor();
		p.setProperty("-connectionsettings", "" + false);
		HttpClient hc = new HttpClient();
		ConnectionSettings cs = new ConnectionSettings(p, hc);

		if (socks5Proxy != null) {
			ProxyDTO proxy = new ProxyDTO();
			proxy.active = true;
			proxy.host = "localhost";
			proxy.port = socksProxyPort;
			proxy.protocol = Type.SOCKS.name();
			if (password != null) {
				proxy.username = "proxyuser";
				proxy.password = password;
			}
			hc.addProxyHandler(ConnectionSettings.createProxyHandler(proxy));
		}

		ServerDTO server = new ServerDTO();
		server.id = httpTestServer.getBaseURI()
			.toString();
		server.verify = false;
		server.trust = Strings.join(httpTestServer.getTrustedCertificateFiles(IO.getFile("generated")));
		cs.add(server);

		URL url = new URL(httpTestServer.getBaseURI() + "/get-tag/ABCDEFGH");
		TaggedData tag = hc.connectTagged(url);
		assertNotNull(tag);
		assertEquals("ABCDEFGH", tag.getTag());
		String s = IO.collect(tag.getInputStream());
		assertNotNull(s);
		assertTrue(s.trim()
			.startsWith("{"));
		assertTrue(proxyCalled.get());
		// assertTrue(this.authenticationCalled.get() == authenticationCalled);

	}

}
