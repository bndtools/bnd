package aQute.bnd.repository.maven.pom.provider;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.lib.io.IO;
import aQute.libg.reporter.slf4j.Slf4jReporter;
import aQute.maven.api.Archive;
import aQute.maven.api.Program;
import aQute.maven.api.Revision;
import aQute.maven.provider.MavenBackingRepository;
import aQute.maven.provider.MavenRepository;
import aQute.service.reporter.Reporter;
import junit.framework.TestCase;

public class PomRepositoryTest extends TestCase {
	Reporter	reporter	= new Slf4jReporter();
	File		tmp			= IO.getFile("generated/tmp");
	File		localRepo	= IO.getFile("~/.m2/repository");
	File		location	= IO.getFile(tmp, "index.xml");
	HttpClient	client		= new HttpClient();

	public void setUp() {
		IO.delete(tmp);
		tmp.mkdirs();
	}
	public void testPom() throws Exception {
		MavenRepository mr = getRepo();

		Revision revision = Program.valueOf("org.apache.activemq", "activemq-camel").version("5.9.1");

		Traverser t = new Traverser(mr, revision, Processor.getExecutor());
		Map<Archive,Resource> value = t.getResources().getValue();
		// System.out.println(value.size());
		// System.out.println(Strings.join("\n", value.keySet()));
		assertEquals(294, value.size());
		assertAllBndCap(value);
	}

	public void testRepository() throws Exception {
		MavenRepository repo = getRepo();
		Revision revision = Revision.valueOf("bcel:bcel:5.1");

		PomRepository pom = new PomRepository(repo, location, revision);

		assertTrue(location.isFile());

		XMLResourceParser xp = new XMLResourceParser(location);
		List<Resource> parse = xp.parse();
		assertEquals(parse.size(), pom.getResources().size());
	}

	MavenRepository getRepo() throws Exception {
		List<MavenBackingRepository> central = MavenBackingRepository.create("https://repo1.maven.org/maven2/",
				reporter, localRepo, client);
		List<MavenBackingRepository> apache = MavenBackingRepository
				.create("https://repository.apache.org/content/groups/snapshots/", reporter, localRepo, client);

		MavenRepository mr = new MavenRepository(localRepo, "test", central, apache, Processor.getExecutor(), reporter,
				null);
		return mr;
	}

	void assertAllBndCap(Map<Archive,Resource> value) {
		for (Resource resource : value.values()) {
			List<Capability> capabilities = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
			assertNotNull(capabilities);
			assertEquals(1, capabilities.size());

			capabilities = resource.getCapabilities("bnd.info");
			Capability c = capabilities.get(0);
			String a = (String) c.getAttributes().get("name");
			Archive archive = Archive.valueOf(a);
			assertNotNull(archive);
		}
	}
}
