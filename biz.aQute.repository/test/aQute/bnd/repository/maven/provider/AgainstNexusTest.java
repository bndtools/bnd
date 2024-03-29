package aQute.bnd.repository.maven.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.RepositoryPlugin.PutOptions;
import aQute.bnd.service.RepositoryPlugin.PutResult;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.bnd.url.BasicAuthentication;
import aQute.bnd.version.Version;
import aQute.http.testservers.HttpTestServer;
import aQute.http.testservers.HttpTestServer.Config;
import aQute.lib.io.IO;

public class AgainstNexusTest {
	private static final String	HTTP_LOCALHOST_8081	= "http://localhost:8081/nexus/content/repositories/snapshots/";
	@InjectTemporaryDirectory
	File						tmp;
	File						local;
	File						remote;
	File						index;
	boolean						skip				= false;

	private MavenBndRepository	repo;

	@BeforeEach
	protected void setUp() throws Exception {
		local = IO.getFile(tmp, "local");
		remote = IO.getFile(tmp, "remote");
		index = IO.getFile(tmp, "index");
		local.mkdirs();
		IO.copy(IO.getFile("testresources/nexus/index.maven"), index);
		Config config = new HttpTestServer.Config();

		try {
			new URL(HTTP_LOCALHOST_8081).openStream();
			skip = false;
		} catch (FileNotFoundException e) {
			skip = false;
		} catch (Exception e) {
			skip = true;
		}
		skip = true;
	}

	@Test
	public void testBasic() throws Exception {
		if (skip)
			return;

		config(null);
		assertEquals("[group:artifact]", repo.list(null)
			.toString());
		assertEquals("[1.0.0.SNAPSHOT]", repo.versions("group:artifact")
			.toString());
		File f = repo.get("group:artifact", new Version("1.0.0.SNAPSHOT"), null);
		assertEquals(1497, f.length());
		System.out.println(f);
	}

	@Test
	public void testRelease() throws Exception {
		if (skip)
			return;
		config(null);
		File r = IO.getFile("testresources/release.jar");
		File s = IO.getFile("testresources/snapshot.jar");
		PutOptions p = new PutOptions();
		p.context = new Processor();
		p.context.setProperty("-maven-release", "remote");
		PutResult put = repo.put(new FileInputStream(s), p);

	}

	void config(Map<String, String> config) throws Exception {
		if (config == null)
			config = new HashMap<>();
		config.put("local", tmp.getAbsolutePath() + "/local");
		config.put("index", tmp.getAbsolutePath() + "/index");
		config.put("releaseUrl", HTTP_LOCALHOST_8081);

		Processor reporter = new Processor();
		HttpClient client = new HttpClient();

		BasicAuthentication ba = new BasicAuthentication("admin", "admin123", reporter);
		client.addURLConnectionHandler(ba);

		Executor executor = Executors.newCachedThreadPool();
		reporter.addBasicPlugin(client);
		reporter.setTrace(true);

		repo = new MavenBndRepository();
		repo.setReporter(reporter);
		repo.setRegistry(reporter);
		repo.setProperties(config);
	}

}
