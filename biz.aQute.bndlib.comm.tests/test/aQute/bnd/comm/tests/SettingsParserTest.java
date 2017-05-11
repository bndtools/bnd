package aQute.bnd.comm.tests;

import java.io.File;
import java.net.Proxy.Type;
import java.util.List;

import aQute.bnd.connection.settings.ConnectionSettings;
import aQute.bnd.connection.settings.ProxyDTO;
import aQute.bnd.connection.settings.ServerDTO;
import aQute.bnd.connection.settings.SettingsDTO;
import aQute.bnd.connection.settings.SettingsParser;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import junit.framework.TestCase;

public class SettingsParserTest extends TestCase {

	public void testMavenEncryptedPassword() throws Exception {

		System.setProperty(ConnectionSettings.M2_SETTINGS_SECURITY_PROPERTY, "testresources/settings-security.xml");
		Processor proc = new Processor();
		proc.setProperty("-connection-settings", "testresources/server-maven-encrypted-selection.xml");
		try (ConnectionSettings cs = new ConnectionSettings(proc, new HttpClient());) {
			cs.readSettings();
			List<ServerDTO> serverDTOs = cs.getServerDTOs();
			assertEquals(1, serverDTOs.size());

			ServerDTO s = serverDTOs.get(0);

			assertEquals("encrypted-password", s.id);
			assertEquals("FOOBAR", s.password);
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

	public void testSocksAuth() throws Exception {
		SettingsDTO settings = getSettings("socks-auth.xml");
		assertEquals(1, settings.proxies.size());
		ProxyDTO p = settings.proxies.get(0);
		assertEquals("myproxy", p.id);
		assertEquals(true, p.active);
		assertEquals(Type.SOCKS, p.protocol);
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
		assertEquals(Type.SOCKS, p.protocol);
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
		assertEquals(Type.SOCKS, p.protocol);
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
