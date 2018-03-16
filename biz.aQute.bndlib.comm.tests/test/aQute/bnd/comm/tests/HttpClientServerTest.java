package aQute.bnd.comm.tests;

import java.io.File;
import java.net.URL;

import aQute.bnd.connection.settings.ConnectionSettings;
import aQute.bnd.connection.settings.ServerDTO;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.url.TaggedData;
import aQute.http.testservers.HttpTestServer.Config;
import aQute.http.testservers.Httpbin;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import junit.framework.TestCase;

/**
 */
public class HttpClientServerTest extends TestCase {
	File tmp = IO.getFile("generated/tmp");

	{
		IO.delete(tmp);
		tmp.mkdirs();

	}

	public void testSimpleSecureNoVerify() throws Exception {
		createSecureServer();
		assertOk(null, false);
	}

	public void testSimpleSecureVerify() throws Exception {
		createSecureServer();
		assertOk(null, true);
	}

	@SuppressWarnings("resource")
	private void assertOk(String password, boolean verify) throws Exception {
		File log = new File(tmp, "log");
		Processor p = new Processor();
		p.setProperty("-connection-log", log.toURI()
			.getPath());

		HttpClient hc = new HttpClient();
		hc.setLog(log);
		ConnectionSettings cs = new ConnectionSettings(p, hc);

		ServerDTO server = new ServerDTO();
		server.id = httpServer.getBaseURI()
			.toString();
		server.verify = verify;
		if (password != null) {
			server.username = "user";
			server.password = password;
		}

		server.trust = Strings.join(httpServer.getTrustedCertificateFiles(IO.getFile("generated")));

		cs.add(server);

		System.out.println(httpServer.getBaseURI());

		URL url = password == null ? new URL(httpServer.getBaseURI() + "/get")
			: new URL(httpServer.getBaseURI() + "/basic-auth/user/good");
		TaggedData tag = hc.connectTagged(url);
		assertNotNull(tag);
		String s = IO.collect(tag.getInputStream());
		assertNotNull(s);
		assertTrue(s.trim()
			.startsWith("{"));
		IO.copy(log, System.out);
	}

	private Httpbin httpServer;

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		if (httpServer != null)
			httpServer.close();
	}

	public void createSecureServer() throws Exception {
		Config config = new Config();
		config.https = true;
		httpServer = new Httpbin(config);
		httpServer.start();
	}

}
