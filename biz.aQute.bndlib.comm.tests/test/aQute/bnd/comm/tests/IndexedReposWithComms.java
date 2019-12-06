package aQute.bnd.comm.tests;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.List;

import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.bnd.repository.osgi.OSGiRepository;
import aQute.bnd.service.url.URLConnector;
import aQute.http.testservers.HttpTestServer;
import aQute.http.testservers.Httpbin;
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

public class IndexedReposWithComms extends TestCase {

	private static SocksProxyServer	socks5Proxy;

	public void testBasicWorkspace() throws Exception {
		HttpTestServer ht = http();
		try {
			createSecureSocks5();
			Workspace ws = Workspace.getWorkspace(aQute.lib.io.IO.getFile("workspaces/basic"));
			assertNotNull(ws);

			List<URLConnector> connectors = ws.getPlugins(URLConnector.class);
			assertNotNull(connectors);
			assertEquals(1, connectors.size());
			assertTrue(connectors.get(0) instanceof HttpClient);

			HttpClient hc = (HttpClient) connectors.get(0);

			InputStream connect = hc.connect(new URL(ht.getBaseURI() + "/basic-auth/user/good"));
			assertNotNull(connect);
			aQute.lib.io.IO.copy(connect, System.out);
			connect.close();

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		} finally {
			ht.close();
		}
	}

	/*
	 * Uses workspaces/indexed Sets up a OSGiRepository to the local server.
	 */

	public void testIndexedRepo() throws IOException, Exception {
		try (HttpTestServer ht = http();) {
			createSecureSocks5();

			Workspace ws = Workspace.getWorkspace(aQute.lib.io.IO.getFile("workspaces/indexed"));
			assertNotNull(ws);
			ws.setProperty("repo", ht.getBaseURI()
				.toASCIIString() + "/index");
			OSGiRepository plugin = ws.getPlugin(OSGiRepository.class);
			assertTrue(ws.check());
			assertNotNull(plugin);

			List<String> list = plugin.list(null);
			assertTrue(ws.check());
			assertTrue(list.size() > 0);

		}
	}

	/*
	 * Uses workspaces/indexed Sets up a OSGiRepository to the local server.
	 */

	public void testIndexedRepoWithPassword() throws IOException, Exception {
		try (HttpTestServer ht = https();) {
			createSecureSocks5();

			Workspace ws = Workspace.getWorkspace(aQute.lib.io.IO.getFile("workspaces/indexed"));
			assertNotNull(ws);
			ws.setProperty("-connection-settings", "${build}/settings-withpassword.xml");
			ws.setProperty("repo", ht.getBaseURI()
				.toASCIIString() + "/index-auth/user/good");
			OSGiRepository plugin = ws.getPlugin(OSGiRepository.class);
			assertTrue(ws.check());
			assertNotNull(plugin);

			List<String> list = plugin.list(null);
			assertTrue(ws.check());
			assertTrue(list.size() > 0);

		}
	}

	private void createSecureSocks5() throws IOException, InterruptedException {
		UserManager userManager = new MemoryBasedUserManager();
		userManager.create(new User("proxyuser", "good"));
		SocksServerBuilder builder = SocksServerBuilder.newSocks5ServerBuilder();
		builder.setBindAddr(InetAddress.getLoopbackAddress());
		builder.setBindPort(9090);
		builder.addSocksMethods(new UsernamePasswordMethod(new UsernamePasswordAuthenticator(userManager) {
			@Override
			public void doAuthenticate(Credentials arg0, Session arg1) throws AuthenticationException {
				System.out.println("Authenticating"); // does not get called?
				super.doAuthenticate(arg0, arg1);

			}
		}));
		socks5Proxy = builder.build();

		socks5Proxy.getSessionManager()
			.addSessionListener("abc", new SessionListener() {

				@Override
				public void onException(Session arg0, Exception arg1) {
					System.err.println("Exception " + arg0 + " " + arg1);
					arg1.printStackTrace();
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
				}
			});

		socks5Proxy.start();
	}

	@Override
	public void tearDown() {
		if (socks5Proxy != null)
			socks5Proxy.shutdown();
	}

	HttpTestServer http() throws Exception, IOException {
		HttpTestServer.Config config = new HttpTestServer.Config();
		config.host = "localhost";
		HttpTestServer ht = new Httpbin(config);
		ht.start();
		return ht;
	}

	HttpTestServer https() throws Exception, IOException {
		HttpTestServer.Config config = new HttpTestServer.Config();
		config.host = "localhost";
		config.https = true;
		HttpTestServer ht = new Httpbin(config);
		ht.start();
		return ht;
	}

}
