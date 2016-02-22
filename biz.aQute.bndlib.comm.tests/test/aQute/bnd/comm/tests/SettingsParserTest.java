package aQute.bnd.comm.tests;

import java.io.File;
import java.io.IOException;
import java.net.Proxy.Type;
import java.net.URL;

import aQute.bnd.connection.settings.ProxyDTO;
import aQute.bnd.connection.settings.ServerDTO;
import aQute.bnd.connection.settings.SettingsDTO;
import aQute.bnd.connection.settings.SettingsParser;
import junit.framework.TestCase;

public class SettingsParserTest extends TestCase {

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

	public void testAuthority() throws IOException {
		URL url = new URL("http://abc:def@httpbin.org/headers");
		System.out.println(url.getUserInfo());
		System.out.println(aQute.lib.io.IO.collect(url.openStream()));
	}
}
