package aQute.bnd.comm.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.Proxy.Type;
import java.net.URL;
import java.util.List;

import aQute.bnd.build.Workspace;
import aQute.bnd.connection.settings.ConnectionSettings;
import aQute.bnd.connection.settings.ProxyDTO;
import aQute.bnd.connection.settings.ServerDTO;
import aQute.bnd.connection.settings.SettingsDTO;
import aQute.bnd.connection.settings.SettingsParser;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.url.URLConnectionHandler;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class SettingsParserTest extends TestCase {

	public void testMavenEncryptedPassword() throws Exception {

		System.setProperty(ConnectionSettings.M2_SETTINGS_SECURITY_PROPERTY, "testresources/settings-security.xml");

		try (Processor proc = new Processor(); HttpClient hc = new HttpClient()) {
			proc.setProperty("-connection-settings", "testresources/server-maven-encrypted-selection.xml");
			ConnectionSettings cs = new ConnectionSettings(proc, hc);
			cs.readSettings();
			List<ServerDTO> serverDTOs = cs.getServerDTOs();
			assertEquals(1, serverDTOs.size());

			ServerDTO s = serverDTOs.get(0);

			assertEquals("encrypted-password", s.id);
			assertEquals("FOOBAR", s.password);
		}
	}

	public void testValidXLMViaWorkspace() throws Exception {
		try (Workspace ws = new Workspace(IO.getFile("testresources/ws"))) {
			ws.setProperty(Constants.CONNECTION_SETTINGS, "cnf/valid.xml");
			HttpClient plugin = ws.getPlugin(HttpClient.class);
			assertThat(ws.check()).isTrue();
			URLConnectionHandler handler = plugin.findMatchingHandler(new URL("http://httpbin.org"));
			assertThat(handler).isNotNull();
		}
	}

	public void testInvalidXLMViaWorkspace() throws Exception {
		try (Workspace ws = new Workspace(IO.getFile("testresources/ws"))) {
			ws.setProperty(Constants.CONNECTION_SETTINGS, "cnf/invalid.xml");
			HttpClient plugin = ws.getPlugin(HttpClient.class);
			assertThat(ws.check("Invalid XML in connection settings for file ")).isTrue();
			URLConnectionHandler handler = plugin.findMatchingHandler(new URL("http://httpbin.org"));
			assertThat(handler).isNull();
		}
	}

	public void testServerSelectionWithTrust() throws Exception {
		SettingsDTO settings = getSettings("server-trust-selection.xml");
		assertEquals(1, settings.servers.size());
		ServerDTO p = settings.servers.get(0);
		assertEquals("httpbin.org", p.id);
		assertNotNull(p.trust);
	}

	public void testServerSelection() throws Exception {
		SettingsDTO settings = getSettings("server-selection.xml");
		assertEquals(1, settings.servers.size());
		ServerDTO p = settings.servers.get(0);
		assertEquals("httpbin.org", p.id);
		assertEquals(null, p.passphrase);
		assertEquals(null, p.privateKey);
		assertEquals("user", p.username);
		assertEquals("passwd", p.password);
	}

	public void testServerSelectionOAuth2() throws Exception {
		SettingsDTO settings = getSettings("server-selection-oauth2.xml");
		assertEquals(1, settings.servers.size());
		ServerDTO p = settings.servers.get(0);
		assertEquals("httpbin.org", p.id);
		assertEquals(null, p.passphrase);
		assertEquals(null, p.privateKey);
		assertEquals(null, p.username);
		assertEquals("token", p.password);
	}

	public void testProxies() throws Exception {
		SettingsDTO settings = getSettings("proxy-types.xml");
		assertEquals(2, settings.proxies.size());
		ProxyDTO p = settings.proxies.get(0);
		assertEquals("http-proxy", p.id);
		assertEquals(true, p.active);
		assertEquals(Type.HTTP.name(), p.protocol.toUpperCase());
		assertEquals("localhost", p.host);
		assertEquals(80, p.port);
		assertEquals(null, p.nonProxyHosts);
		assertEquals(null, p.username);
		assertEquals(null, p.password);

		p = settings.proxies.get(1);
		assertEquals("https-proxy", p.id);
		assertEquals(true, p.active);
		assertEquals("HTTPS", p.protocol.toUpperCase());
		assertEquals("localhost", p.host);
		assertEquals(443, p.port);
		assertEquals(null, p.nonProxyHosts);
		assertEquals(null, p.username);
		assertEquals(null, p.password);
	}

	public void testSocksAuth() throws Exception {
		SettingsDTO settings = getSettings("socks-auth.xml");
		assertEquals(1, settings.proxies.size());
		ProxyDTO p = settings.proxies.get(0);
		assertEquals("myproxy", p.id);
		assertEquals(true, p.active);
		assertEquals(Type.SOCKS.name(), p.protocol.toUpperCase());
		assertEquals(1080, p.port);
		assertEquals(null, p.nonProxyHosts);
		assertEquals("proxyuser", p.username);
		assertEquals("somepassword", p.password);
	}

	public void testSocksNoAuth() throws Exception {
		SettingsDTO settings = getSettings("socks-noauth.xml");
		assertEquals(1, settings.proxies.size());
		ProxyDTO p = settings.proxies.get(0);
		assertEquals("myproxy", p.id);
		assertEquals(true, p.active);
		assertEquals(Type.SOCKS.name(), p.protocol.toUpperCase());
		assertEquals(1080, p.port);
		assertEquals(null, p.nonProxyHosts);
		assertEquals(null, p.username);
		assertEquals(null, p.password);
	}

	public void testNonProxyHost() throws Exception {
		SettingsDTO settings = getSettings("socks-auth-nonproxyhosts.xml");
		assertEquals(1, settings.proxies.size());
		ProxyDTO p = settings.proxies.get(0);
		assertEquals("myproxy", p.id);
		assertEquals(true, p.active);
		assertEquals(Type.SOCKS.name(), p.protocol.toUpperCase());
		assertEquals(1080, p.port);
		assertEquals("*.google.com|ibiblio.org", p.nonProxyHosts);
		assertEquals(null, p.username);
		assertEquals(null, p.password);

	}

	public SettingsDTO getSettings(String name) throws Exception {
		File f = aQute.lib.io.IO.getFile("testresources/" + name);
		SettingsParser msp = new SettingsParser(f);
		SettingsDTO settings = msp.getSettings();
		return settings;
	}
}
