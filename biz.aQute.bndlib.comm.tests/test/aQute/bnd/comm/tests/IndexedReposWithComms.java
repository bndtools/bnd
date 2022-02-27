package aQute.bnd.comm.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.List;
import java.util.function.IntSupplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.bnd.repository.osgi.OSGiRepository;
import aQute.bnd.service.url.URLConnector;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.bnd.test.net.EphemeralPort;
import aQute.http.testservers.HttpTestServer;
import aQute.http.testservers.Httpbin;
import aQute.lib.io.IO;
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

public class IndexedReposWithComms {
	static final IntSupplier	portSupplier	= EphemeralPort.AUTOMATIC;

	@InjectTemporaryDirectory
	File						testDir;
	int							port;

	@BeforeEach
	void setUp() {
		port = portSupplier.getAsInt();
	}

	@Test
	public void testBasicWorkspace() throws Exception {
		try (HttpTestServer ht = http();
			AutoCloseable socks5Proxy = createSecureSocks5();
			Workspace ws = createWorkspace("workspaces/basic", "cnf/settings.xml")) {

			List<URLConnector> connectors = ws.getPlugins(URLConnector.class);
			assertNotNull(connectors);
			assertEquals(1, connectors.size());
			assertTrue(connectors.get(0) instanceof HttpClient);

			HttpClient hc = (HttpClient) connectors.get(0);

			try (InputStream connect = hc.connect(new URL(ht.getBaseURI() + "/basic-auth/user/good"))) {
				assertNotNull(connect);
				IO.copy(connect, System.out);
				System.out.println();
			}
		}
	}

	private Workspace createWorkspace(String folder, String settings) throws Exception {
		IO.copy(IO.getFile(folder), testDir);
		File settingsFile = new File(testDir, settings);
		assertThat(settingsFile).isFile();
		String contents = IO.collect(settingsFile);
		contents = contents.replace("9090", Integer.toString(port));
		IO.store(contents, settingsFile);
		return new Workspace(testDir);
	}
	/*
	 * Uses workspaces/indexed Sets up a OSGiRepository to the local server.
	 */

	@Test
	public void testIndexedRepo() throws Exception {
		try (HttpTestServer ht = http();
			AutoCloseable socks5Proxy = createSecureSocks5();
			Workspace ws = createWorkspace("workspaces/indexed", "cnf/settings.xml")) {
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

	@Test
	public void testIndexedRepoWithPassword() throws Exception {
		try (HttpTestServer ht = https();
			AutoCloseable socks5Proxy = createSecureSocks5();
			Workspace ws = createWorkspace("workspaces/indexed", "cnf/settings-withpassword.xml")) {
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

	private AutoCloseable createSecureSocks5() throws IOException, InterruptedException {
		UserManager userManager = new MemoryBasedUserManager();
		userManager.create(new User("proxyuser", "good"));
		SocksServerBuilder builder = SocksServerBuilder.newSocks5ServerBuilder();
		builder.setBindAddr(InetAddress.getLoopbackAddress());
		builder.setBindPort(port);
		builder.addSocksMethods(new UsernamePasswordMethod(new UsernamePasswordAuthenticator(userManager) {
			@Override
			public void doAuthenticate(Credentials arg0, Session arg1) throws AuthenticationException {
				System.out.println("Authenticating"); // does not get called?
				super.doAuthenticate(arg0, arg1);

			}
		}));
		SocksProxyServer socks5Proxy = builder.build();

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
		return socks5Proxy::shutdown;
	}

	HttpTestServer http() throws Exception {
		HttpTestServer.Config config = new HttpTestServer.Config();
		config.host = "localhost";
		HttpTestServer ht = new Httpbin(config);
		ht.start();
		return ht;
	}

	HttpTestServer https() throws Exception {
		HttpTestServer.Config config = new HttpTestServer.Config();
		config.host = "localhost";
		config.https = true;
		HttpTestServer ht = new Httpbin(config);
		ht.start();
		return ht;
	}

}
