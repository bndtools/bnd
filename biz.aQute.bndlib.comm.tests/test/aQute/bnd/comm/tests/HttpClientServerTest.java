package aQute.bnd.comm.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import aQute.bnd.connection.settings.ConnectionSettings;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.url.TaggedData;
import aQute.http.testservers.HttpTestServer.Config;
import aQute.http.testservers.Httpbin;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;

/**
 */
public class HttpClientServerTest {
	private File	tmp;
	private Httpbin	httpsServer;

	@BeforeEach
	protected void setUp(TestInfo testInfo) throws Exception {
		tmp = IO.getFile("generated/tmp/test/" + testInfo.getTestClass()
			.get()
			.getName() + "/"
			+ testInfo.getTestMethod()
				.get()
				.getName())
			.getAbsoluteFile();
		IO.delete(tmp);
		IO.mkdirs(tmp);
		Config config = new Config();
		config.https = true;
		httpsServer = new Httpbin(config);
		httpsServer.start();
	}

	@AfterEach
	protected void tearDown() throws Exception {
		IO.close(httpsServer);
	}

	@Test
	public void testSimpleSecureNoVerify() throws Exception {
		assertOk(null, null, false);
	}

	@Test
	public void testSimpleSecureVerify() throws Exception {
		assertOk(null, null, true);
	}

	@Test
	public void testSimpleSecureVerifyBasic() throws Exception {
		assertOk("user", "good", true);
	}

	@Test
	public void testSimpleSecureVerifyBearer() throws Exception {
		assertOk(null, "token", true);
	}

	@SuppressWarnings("resource")
	private void assertOk(String username, String password, boolean verify) throws Exception {
		File log = new File(tmp, "log");
		try (Processor p = new Processor(); HttpClient hc = new HttpClient()) {
			System.out.println(httpsServer.getBaseURI());

			String settings = "server;id=\"" + httpsServer.getBaseURI() + "\";verify=" + verify + ";trust=\""
				+ Strings.join(httpsServer.getTrustedCertificateFiles(tmp)) + "\"";

			URL url;
			if (password == null) {
				url = new URL(httpsServer.getBaseURI() + "/get");
			} else if (username != null) {
				url = new URL(httpsServer.getBaseURI() + "/basic-auth/" + username + "/" + password);
				settings += ";username=\"" + username + "\";password=\"" + password + "\"";
			} else {
				url = new URL(httpsServer.getBaseURI() + "/bearer-auth/" + password);
				settings += ";password=\"" + password + "\"";
			}

			p.setProperty("-connection-log", log.toURI()
				.getPath());
			p.setProperty("-connection-settings", settings);
			hc.setLog(log);

			ConnectionSettings cs = new ConnectionSettings(p, hc);
			cs.readSettings();

			TaggedData tag = hc.connectTagged(url);
			assertNotNull(tag);
			assertEquals(200, tag.getResponseCode());
			InputStream in = tag.getInputStream();
			assertNotNull(in);
			String s = IO.collect(in);
			assertNotNull(s);
			System.out.println(s);
			assertTrue(s.trim()
				.startsWith("{"));
		}
		IO.copy(log, System.out);
	}
}
