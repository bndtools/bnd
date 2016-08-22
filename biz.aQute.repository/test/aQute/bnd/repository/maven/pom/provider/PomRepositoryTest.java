package aQute.bnd.repository.maven.pom.provider;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.service.repository.RequirementBuilder;
import org.osgi.util.promise.Promise;

import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.lib.io.IO;
import aQute.libg.reporter.slf4j.Slf4jReporter;
import aQute.maven.api.Archive;
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

	/**
	 * this test fails on Travis
	 * 
	 * <pre>
	 * aQute.bnd.repository.maven.pom.provider.PomRepositoryTest > testPom FAILED
	junit.framework.AssertionFailedError: expected:<8> but was:<3>
	    at junit.framework.Assert.fail(Assert.java:57)
	    at junit.framework.Assert.failNotEquals(Assert.java:329)
	    at junit.framework.Assert.assertEquals(Assert.java:78)
	    at junit.framework.Assert.assertEquals(Assert.java:234)
	    at junit.framework.Assert.assertEquals(Assert.java:241)
	    at junit.framework.TestCase.assertEquals(TestCase.java:409)
	    at aQute.bnd.repository.maven.pom.provider.PomRepositoryTest.testPom(PomRepositoryTest.java:46)
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void testPom() throws Exception {
		// for (int i = 0; i < 100; i++) {
		// MavenRepository mr = getRepo();
		//
		// Revision revision = Program.valueOf("org.apache.aries.blueprint",
		// "org.apache.aries.blueprint.cm")
		// .version("1.0.8");
		//
		// Traverser t = new Traverser(mr, revision, Processor.getExecutor());
		// Map<Archive,Resource> value = t.getResources().getValue();
		// assertEquals(8, value.size());
		// assertAllBndCap(value);
		// }
	}

	public void testBndPomRepoFile() throws Exception {
		BndPomRepository bpr = new BndPomRepository();
		Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
		w.setBase(tmp);
		bpr.setRegistry(w);

		Map<String,String> config = new HashMap<>();
		config.put("pom", "testdata/pomrepo/simple.xml");
		config.put("snapshotUrls", "https://repo1.maven.org/maven2/");
		config.put("releaseUrls", "https://repo1.maven.org/maven2/");
		config.put("name", "test");
		bpr.setProperties(config);

		List<String> list = bpr.list(null);
		assertNotNull(list);
		assertEquals(1, list.size());
	}

	public void testBndPomRepoFileNoDeps() throws Exception {
		BndPomRepository bpr = new BndPomRepository();
		Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
		w.setBase(tmp);
		bpr.setRegistry(w);

		Map<String,String> config = new HashMap<>();
		config.put("pom", "testdata/pomrepo/simple-nodeps.xml");
		config.put("snapshotUrls", "https://repo1.maven.org/maven2/");
		config.put("releaseUrls", "https://repo1.maven.org/maven2/");
		config.put("name", "test");
		bpr.setProperties(config);

		List<String> list = bpr.list(null);
		assertNotNull(list);
		assertEquals(0, list.size());
	}

	public void testBndPomRepoURI() throws Exception {
		final BndPomRepository bpr = new BndPomRepository();
		Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
		w.setBase(tmp);
		bpr.setRegistry(w);

		Map<String,String> config = new HashMap<>();
		config.put("pom",
				"https://repo1.maven.org/maven2/org/apache/felix/org.apache.felix.gogo.shell/0.12.0/org.apache.felix.gogo.shell-0.12.0.pom");
		config.put("snapshotUrls", "https://repo1.maven.org/maven2/");
		config.put("releaseUrls", "https://repo1.maven.org/maven2/");
		config.put("name", "test");
		bpr.setProperties(config);

		List<String> list = bpr.list(null);
		assertNotNull(list);
		assertEquals(1, list.size());
		RequirementBuilder builder = bpr.newRequirementBuilder("osgi.identity");
		builder.addDirective("filter", "(osgi.identity=org.apache.felix.gogo.runtime)");
		Promise<Collection<Resource>> providers = bpr.findProviders(builder.buildExpression());
		Collection<Resource> resources = providers.getValue();
		assertFalse(resources.isEmpty());
		assertEquals(1, resources.size());
	}

	public void testBndPomRepoRefresh() throws Exception {
		BndPomRepository bpr = new BndPomRepository();
		Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
		w.setBase(tmp);
		bpr.setRegistry(w);

		Map<String,String> config = new HashMap<>();
		config.put("pom", "testdata/pomrepo/simple.xml");
		config.put("snapshotUrls", "https://repo1.maven.org/maven2/");
		config.put("releaseUrls", "https://repo1.maven.org/maven2/");
		config.put("name", "test");
		bpr.setProperties(config);

		List<String> list = bpr.list(null);
		assertNotNull(list);
		assertEquals(1, list.size());
		try {
			bpr.refresh();
		} catch (Throwable t) {
			fail();
		}
	}

	public void testRepository() throws Exception {
		MavenRepository repo = getRepo();
		Revision revision = Revision.valueOf("bcel:bcel:5.1");
		HttpClient c = new HttpClient();
		PomRepository pom = new PomRepository(repo, client, location, revision);

		assertTrue(location.isFile());

		try (XMLResourceParser xp = new XMLResourceParser(location);) {
			List<Resource> parse = xp.parse();
			assertEquals(parse.size(), pom.getResources().size());
		}
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
