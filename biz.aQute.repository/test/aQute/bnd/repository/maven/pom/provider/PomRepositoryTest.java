package aQute.bnd.repository.maven.pom.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.service.repository.RequirementBuilder;
import org.osgi.util.promise.Promise;

import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.libg.reporter.slf4j.Slf4jReporter;
import aQute.maven.api.Archive;
import aQute.maven.api.Program;
import aQute.maven.api.Revision;
import aQute.maven.provider.MavenBackingRepository;
import aQute.maven.provider.MavenRepository;
import aQute.service.reporter.Reporter;
import junit.framework.TestCase;

public class PomRepositoryTest extends TestCase {
	static final String	MAVEN_REPO_LOCAL	= System.getProperty("maven.repo.local", "~/.m2/repository");

	Reporter			reporter			= new Slf4jReporter(PomRepositoryTest.class);
	private File		tmp;
	private File		localRepo;
	private File		location;
	private HttpClient	client;

	@Override
	protected void setUp() {
		tmp = IO.getFile("generated/tmp/test/" + getClass().getName() + "/" + getName());
		localRepo = IO.getFile(MAVEN_REPO_LOCAL);
		location = IO.getFile(tmp, "index.xml");
		IO.delete(tmp);
		tmp.mkdirs();
		client = new HttpClient();
	}

	@Override
	protected void tearDown() {
		client.close();
	}

	public void testPomTransitive() throws Exception {
		MavenRepository mr = getRepo();

		Revision revision = Program.valueOf("org.apache.aries.blueprint", "org.apache.aries.blueprint.cm")
			.version("1.0.8");

		HttpClient client = new HttpClient();
		Traverser t = new Traverser(mr, client, true).revision(revision);
		Map<Archive, Resource> value = t.getResources()
			.getValue();
		assertEquals(8, value.size());
		assertAllBndCap(value);
	}

	public void testPomNotTransitive() throws Exception {
		MavenRepository mr = getRepo();

		Revision revision = Program.valueOf("org.apache.aries.blueprint", "org.apache.aries.blueprint.cm")
			.version("1.0.8");

		HttpClient client = new HttpClient();
		Traverser t = new Traverser(mr, client, false).revision(revision);
		Map<Archive, Resource> value = t.getResources()
			.getValue();
		assertEquals(1, value.size());
		assertAllBndCap(value);
	}

	public void testDependenciesWithVersionRanges() throws Exception {
		try (BndPomRepository bpr = new BndPomRepository()) {
			Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			w.setBase(tmp);
			bpr.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("revision", "com.mchange:mchange-commons-java:0.2.10");
			config.put("snapshotUrls", "https://repo.maven.apache.org/maven2/");
			config.put("releaseUrls", "https://repo.maven.apache.org/maven2/");
			config.put("name", "test");
			bpr.setProperties(config);

			List<String> list = bpr.list(null);
			for (String bsn : list) {
				SortedSet<Version> versions = bpr.versions(bsn);
				assertEquals(1, versions.size());
				Version v = versions.first();
				switch (bsn) {
					case "log4j:log4j" :
						assertEquals("1.2.14", v.toString());
						break;

					case "com.typesafe.config" :
						assertEquals("1.2.1", v.toString());
						break;

					case "com.mchange:mchange-commons-java" :
						assertEquals("0.2.10", v.toString());
						break;

					case "slf4j.api" :
						assertEquals("1.7.5", v.toString());
						break;

					default :
						fail(bsn);
				}

			}
			assertNotNull(list);
			assertEquals(4, list.size());
		}
	}

	public void testWithSources() throws Exception {
		try (BndPomRepository bpr = new BndPomRepository()) {
			Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			w.setBase(tmp);
			bpr.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("revision", "com.mchange:mchange-commons-java:0.2.10");
			config.put("snapshotUrls", "https://repo.maven.apache.org/maven2/");
			config.put("releaseUrls", "https://repo.maven.apache.org/maven2/");
			config.put("name", "test");
			bpr.setProperties(config);

			File file = bpr.get("slf4j.api", new Version("1.7.5"), null);
			assertNotNull(file);
			assertThat(file).isFile();

			File source = bpr.get("slf4j.api.source", new Version("1.7.5"), null);
			assertNotNull(source);
			assertThat(source).isFile();
		}
	}

	public void testBndPomRepoFile() throws Exception {
		try (BndPomRepository bpr = new BndPomRepository()) {
			Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			w.setBase(tmp);
			bpr.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("pom", "testdata/pomrepo/simple.xml");
			config.put("snapshotUrls", "https://repo.maven.apache.org/maven2/");
			config.put("releaseUrls", "https://repo.maven.apache.org/maven2/");
			config.put("name", "test");
			bpr.setProperties(config);

			List<String> list = bpr.list(null);
			assertNotNull(list);
			assertEquals(1, list.size());
		}
	}

	public void testBndPomRepoFileExistingParent() throws Exception {
		try (BndPomRepository bpr = new BndPomRepository()) {
			Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			w.setBase(tmp);
			bpr.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("pom", "testdata/pomrepo/existing-parent.xml");
			config.put("snapshotUrls", "https://repo.maven.apache.org/maven2/");
			config.put("releaseUrls", "https://repo.maven.apache.org/maven2/");
			config.put("name", "test");
			bpr.setProperties(config);

			List<String> list = bpr.list(null);
			assertNotNull(list);
			assertEquals(1, list.size());
		}
	}

	public void testBndPomRepoFileMissingParent() throws Exception {
		try (BndPomRepository bpr = new BndPomRepository()) {
			Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			w.setBase(tmp);
			bpr.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("pom", "testdata/pomrepo/missing-parent.xml");
			config.put("snapshotUrls", "https://repo.maven.apache.org/maven2/");
			config.put("releaseUrls", "https://repo.maven.apache.org/maven2/");
			config.put("name", "test");
			bpr.setProperties(config);

			try {
				bpr.list(null);
				fail("Should throw IllegalArgumentException on missing parent pom.");
			} catch (Exception iae) {
				// This exception is expected!
			}
		}
	}

	public void testEntityPom() throws Exception {
		try (BndPomRepository bpr = new BndPomRepository()) {
			Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			w.setBase(tmp);
			bpr.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("pom", "testdata/pomrepo/entity.xml");
			config.put("snapshotUrls", "https://repo.maven.apache.org/maven2/");
			config.put("releaseUrls", "https://repo.maven.apache.org/maven2/");
			config.put("name", "test");
			bpr.setProperties(config);

			List<String> list = bpr.list(null);
			assertNotNull(list);
			assertEquals(1, list.size());
		}
	}

	public void testBndPomRepoFileNoDeps() throws Exception {
		try (BndPomRepository bpr = new BndPomRepository()) {
			Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			w.setBase(tmp);
			bpr.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("pom", "testdata/pomrepo/simple-nodeps.xml");
			config.put("snapshotUrls", "https://repo.maven.apache.org/maven2/");
			config.put("releaseUrls", "https://repo.maven.apache.org/maven2/");
			config.put("name", "test");
			bpr.setProperties(config);

			List<String> list = bpr.list(null);
			assertNotNull(list);
			assertEquals(0, list.size());
		}
	}

	public void testBndPomRepoURI() throws Exception {
		try (BndPomRepository bpr = new BndPomRepository()) {
			Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			w.setBase(tmp);
			bpr.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("pom",
				"https://repo.maven.apache.org/maven2/org/apache/felix/org.apache.felix.gogo.shell/0.12.0/org.apache.felix.gogo.shell-0.12.0.pom");
			config.put("snapshotUrls", "https://repo.maven.apache.org/maven2/");
			config.put("releaseUrls", "https://repo.maven.apache.org/maven2/");
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
	}

	public void testBndPomRepoRefresh() throws Exception {
		try (BndPomRepository bpr = new BndPomRepository()) {
			Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			w.setBase(tmp);
			bpr.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("pom", "testdata/pomrepo/simple.xml");
			config.put("snapshotUrls", "https://repo.maven.apache.org/maven2/");
			config.put("releaseUrls", "https://repo.maven.apache.org/maven2/");
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
	}

	public void testRepository() throws Exception {
		MavenRepository repo = getRepo();
		List<Revision> revisions = Arrays.asList(Revision.valueOf("bcel:bcel:5.1"));
		PomRepository pom = new PomRepository(repo, client, location).revisions(revisions);

		assertTrue(location.isFile());

		try (XMLResourceParser xp = new XMLResourceParser(location);) {
			List<Resource> parse = xp.parse();
			assertEquals(parse.size(), pom.getResources()
				.size());
		}
		assertFalse(pom.isStale());
	}

	public void testSearchRepoSimple() throws Exception {
		try (BndPomRepository mcsr = new BndPomRepository()) {
			Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			w.setBase(tmp);
			mcsr.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("query", "q=g:biz.aQute.bnd+a:biz.aQute.bnd+AND+v:3.2.0");
			config.put("snapshotUrls", "https://repo.maven.apache.org/maven2/");
			config.put("releaseUrls", "https://repo.maven.apache.org/maven2/");
			config.put("name", "test");
			mcsr.setProperties(config);

			List<String> list = mcsr.list(null);
			assertNotNull(list);
			assertEquals(1, list.size());
		}
	}

	public void testSearchRepoNoUrls() throws Exception {
		try (BndPomRepository mcsr = new BndPomRepository()) {
			Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			w.setBase(tmp);
			mcsr.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("query", "q=g:biz.aQute.bnd+a:biz.aQute.bnd+AND+v:3.2.0");
			config.put("name", "test");
			mcsr.setProperties(config);

			List<String> list = mcsr.list(null);
			assertNotNull(list);
			assertEquals(1, list.size());
		}
	}

	public void testSearchRepoMultipleConfigurationsDontBreak() throws Exception {
		Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
		w.setBase(tmp);

		try (BndPomRepository mcsrBnd320 = new BndPomRepository();
			BndPomRepository mcsrBnd330 = new BndPomRepository()) {
			mcsrBnd320.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("query", "q=g:biz.aQute.bnd+a:biz.aQute.bnd+AND+v:3.2.0");
			config.put("name", "bnd320");
			mcsrBnd320.setProperties(config);

			mcsrBnd330.setRegistry(w);

			config = new HashMap<>();
			config.put("query", "q=g:biz.aQute.bnd+a:biz.aQute.bnd+AND+v:3.3.0");
			config.put("name", "bnd330");
			mcsrBnd330.setProperties(config);

			List<String> list320 = mcsrBnd320.list(null);
			assertNotNull(list320);
			assertEquals(1, list320.size());

			List<String> list330 = mcsrBnd330.list(null);
			assertNotNull(list330);
			assertEquals(1, list330.size());

			// check the first repo to make sure it's there.
			RequirementBuilder builder = mcsrBnd320.newRequirementBuilder("osgi.identity");
			builder.addDirective("filter", "(&(osgi.identity=biz.aQute.bnd)(version>=3.2.0)(!(version>=3.3.0)))");
			Promise<Collection<Resource>> providers = mcsrBnd320.findProviders(builder.buildExpression());
			Collection<Resource> resources = providers.getValue();
			assertFalse(resources.isEmpty());
			assertEquals(1, resources.size());

			// make sure it's not in the second repo, otherwise the caches are
			// messed up.
			providers = mcsrBnd330.findProviders(builder.buildExpression());
			resources = providers.getValue();
			assertTrue(resources.isEmpty());
			assertEquals(0, resources.size());
		}
	}

	/**
	 * This test occasionally fails on Travis.
	 *
	 * <pre>
	aQute.bnd.repository.maven.pom.provider.PomRepositoryTest > testSearchRepoAllVersions FAILED
	    junit.framework.AssertionFailedError: expected:<1> but was:<2>
	        at junit.framework.Assert.fail(Assert.java:57)
	        at junit.framework.Assert.failNotEquals(Assert.java:329)
	        at junit.framework.Assert.assertEquals(Assert.java:78)
	        at junit.framework.Assert.assertEquals(Assert.java:234)
	        at junit.framework.Assert.assertEquals(Assert.java:241)
	        at junit.framework.TestCase.assertEquals(TestCase.java:409)
	        at aQute.bnd.repository.maven.pom.provider.PomRepositoryTest.testSearchRepoAllVersions(PomRepositoryTest.java:220)
	 * </pre>
	 */
	// public void testSearchRepoAllVersions() throws Exception {
	// BndPomRepository mcsr = new BndPomRepository();
	// Workspace w = Workspace.createStandaloneWorkspace(new Processor(),
	// tmp.toURI());
	// w.setBase(tmp);
	// mcsr.setRegistry(w);
	//
	// Map<String,String> config = new HashMap<>();
	// config.put("query",
	// "q=g:biz.aQute.bnd+AND+a:biz.aQute.bnd&core=gav&rows=100");
	// config.put("name", "test");
	// mcsr.setProperties(config);
	//
	// List<String> list = mcsr.list(null);
	// assertNotNull(list);
	// // All the results are represented by a single bsn
	// assertEquals(1, list.size());
	// SortedSet<Version> versions = mcsr.versions("biz.aQute.bnd");
	// assertTrue(versions.size() >= 4);
	// }

	public void testSearchRepoFailNoQuery() throws Exception {
		try (BndPomRepository mcsr = new BndPomRepository()) {
			Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			w.setBase(tmp);
			mcsr.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("name", "test");
			try {
				mcsr.setProperties(config);
				fail();
			} catch (Exception e) {
				assertEquals("Neither pom, revision nor query property are set", e.getMessage());
			}
		}
	}

	public void testSearchRepoFailNoName() throws Exception {
		try (BndPomRepository mcsr = new BndPomRepository()) {
			Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			w.setBase(tmp);
			mcsr.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("query", "q=g:biz.aQute.bnd+a:biz.aQute.bnd+AND+v:3.2.0");
			try {
				mcsr.setProperties(config);
				fail();
			} catch (Exception e) {
				assertEquals("Must get a name", e.getMessage());
			}
		}
	}

	public void testNonStandardClassifierDependencies() throws Exception {
		try (BndPomRepository mcsr = new BndPomRepository()) {
			Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			w.setBase(tmp);
			mcsr.setRegistry(w);

			File local = new File(tmp, "m2-repository");
			local.mkdirs();

			Map<String, String> config = new HashMap<>();
			config.put("name", "pmd");
			config.put("revision", "net.sourceforge.pmd:pmd-java:5.2.3");
			config.put("snapshotUrls", "https://repo.maven.apache.org/maven2/");
			config.put("releaseUrls", "https://repo.maven.apache.org/maven2/");
			config.put("local", local.getAbsolutePath());
			mcsr.setProperties(config);

			List<String> list = mcsr.list(null);
			assertNotNull(list);

			URL url = new URL(tmp.toURI()
				.toURL(), "m2-repository/net/sourceforge/saxon/saxon/9.1.0.8/saxon-9.1.0.8-dom.jar");

			File dom = new File(url.getFile());
			assertTrue(dom.exists());

			// I'm assuming because we don't have a way of currently "getting"
			// such
			// a classified artifact from the repo that we'd have to encode the
			// classifier in the bsn of the classified jar if it's not a bundle.
			// Something like:

			// File file = mcsr.get("net.sourceforge.pmd:pmd-java:dom", new
			// Version("5.2.3"), null);
			// assertNotNull(file);
		}
	}

	public void testMultipleRevisions() throws Exception {
		try (BndPomRepository mcsr = new BndPomRepository()) {
			Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			w.setBase(tmp);
			mcsr.setRegistry(w);

			File local = new File(tmp, "m2-repository");
			local.mkdirs();

			Map<String, String> config = new HashMap<>();
			config.put("name", "test-dependencies");

			String revisions = Strings.join(new String[] {
				"biz.aQute.bnd:biz.aQute.junit:3.3.0", "biz.aQute.bnd:biz.aQute.launcher:3.3.0",
				"biz.aQute.bnd:biz.aQute.remote.launcher:3.3.0", "biz.aQute.bnd:biz.aQute.tester:3.3.0"
			});

			config.put("revision", revisions);
			config.put("snapshotUrls", "https://repo.maven.apache.org/maven2/");
			config.put("releaseUrls", "https://repo.maven.apache.org/maven2/");
			config.put("local", local.getAbsolutePath());
			config.put("transitive", "true");
			mcsr.setProperties(config);

			List<String> list = mcsr.list(null);
			assertNotNull(list);

			RequirementBuilder builder = mcsr.newRequirementBuilder("osgi.identity");
			builder.addAttribute("filter", "(osgi.identity=biz.aQute.tester)");

			Promise<Collection<Resource>> providers = mcsr.findProviders(builder.buildExpression());
			Collection<Resource> resources = providers.getValue();
			assertFalse(resources.isEmpty());
		}
	}

	public void testMultiplePomFiles() throws Exception {
		try (BndPomRepository mcsr = new BndPomRepository()) {
			Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			w.setBase(tmp);
			mcsr.setRegistry(w);

			File local = new File(tmp, "m2-repository");
			local.mkdirs();

			Map<String, String> config = new HashMap<>();
			config.put("name", "test-dependencies");

			String pomFiles = Strings.join(new String[] {
				"testdata/pomrepo/simple.xml",
				"https://repo.maven.apache.org/maven2/org/apache/felix/org.apache.felix.gogo.shell/0.12.0/org.apache.felix.gogo.shell-0.12.0.pom"
			});

			config.put("pom", pomFiles);
			config.put("snapshotUrls", "https://repo.maven.apache.org/maven2/");
			config.put("releaseUrls", "https://repo.maven.apache.org/maven2/");
			config.put("local", local.getAbsolutePath());
			mcsr.setProperties(config);

			List<String> list = mcsr.list(null);
			assertNotNull(list);
			assertEquals(2, list.size());

			RequirementBuilder builder = mcsr.newRequirementBuilder("osgi.identity");
			builder.addAttribute("filter", "(osgi.identity=org.apache.felix.gogo.shell)");

			Promise<Collection<Resource>> providers = mcsr.findProviders(builder.buildExpression());
			Collection<Resource> resources = providers.getValue();
			assertFalse(resources.isEmpty());

			builder = mcsr.newRequirementBuilder("osgi.identity");
			builder.addAttribute("filter", "(osgi.identity=osgi.core)");

			providers = mcsr.findProviders(builder.buildExpression());
			resources = providers.getValue();
			assertFalse(resources.isEmpty());
		}
	}

	/**
	 * Copies the POM from testdata/pomfiles/simple-nodeps.xml to a temporary
	 * file which is deleted at the end of the test. This copied POM is added to
	 * a Repo and updated with a new dependency to test the polling for changes.
	 * Furthermore a remote POM is added to the config to check run through the
	 * stale-check
	 */
	public void testBndPomRepoFilePolling() throws Exception {
		Path path = IO.getFile(tmp, "pom.xml")
			.toPath();
		Path source = Paths.get("testdata/pomrepo/simple-nodeps.xml");
		Files.copy(source, path, StandardCopyOption.REPLACE_EXISTING);
		Files.setLastModifiedTime(path, Files.getLastModifiedTime(source));

		try (BndPomRepository bpr = new BndPomRepository()) {
			Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			w.setBase(tmp);
			bpr.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("pom",
				path.toString() + ",https://repo.maven.apache.org/maven2/javax/enterprise/cdi-api/2.0/cdi-api-2.0.pom");
			config.put("snapshotUrls", "https://repo.maven.apache.org/maven2/");
			config.put("releaseUrls", "https://repo.maven.apache.org/maven2/");
			config.put("name", "test");
			config.put("poll.time", "1");
			bpr.setProperties(config);

			List<String> list = bpr.list(null);
			assertEquals(3, list.size());

			CountDownLatch refreshed = new CountDownLatch(1);
			w.addBasicPlugin(new RepositoryListenerPlugin() {
				@Override
				public void repositoryRefreshed(RepositoryPlugin repository) {
					if (repository == bpr) {
						refreshed.countDown();
					}
				}

				@Override
				public void repositoriesRefreshed() {}

				@Override
				public void bundleRemoved(RepositoryPlugin repository, Jar jar, File file) {}

				@Override
				public void bundleAdded(RepositoryPlugin repository, Jar jar, File file) {}
			});

			Files.copy(Paths.get("testdata/pomrepo/simple.xml"), path, StandardCopyOption.REPLACE_EXISTING);
			// Make sure changed pomfile timestamp is later than PomRepository
			// location file
			Files.setLastModifiedTime(path, FileTime.fromMillis(System.currentTimeMillis() + 1000L));
			assertTrue(refreshed.await(10, TimeUnit.SECONDS));
			list = bpr.list(null);
			assertEquals(4, list.size());
		}
	}

	MavenRepository getRepo() throws Exception {
		List<MavenBackingRepository> central = MavenBackingRepository.create("https://repo.maven.apache.org/maven2/",
			reporter, localRepo, client);
		List<MavenBackingRepository> apache = MavenBackingRepository
			.create("https://repository.apache.org/content/groups/snapshots/", reporter, localRepo, client);

		MavenRepository mr = new MavenRepository(localRepo, "test", central, apache, client.promiseFactory()
			.executor(), reporter);
		return mr;
	}

	void assertAllBndCap(Map<Archive, Resource> value) {
		for (Resource resource : value.values()) {
			List<Capability> capabilities = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
			assertNotNull(capabilities);
			assertEquals(1, capabilities.size());

			capabilities = resource.getCapabilities("bnd.info");
			Capability c = capabilities.get(0);
			String a = (String) c.getAttributes()
				.get("name");
			Archive archive = Archive.valueOf(a);
			assertNotNull(archive);
		}
	}
}
