package aQute.bnd.repository.maven.provider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;
import aQute.bnd.service.RepositoryPlugin.PutOptions;
import aQute.bnd.service.RepositoryPlugin.PutResult;
import aQute.bnd.service.maven.PomOptions;
import aQute.bnd.service.progress.ProgressPlugin;
import aQute.bnd.version.Version;
import aQute.http.testservers.HttpTestServer.Config;
import aQute.lib.io.IO;
import aQute.maven.api.MavenScope;
import aQute.maven.api.Revision;
import aQute.maven.provider.FakeNexus;
import aQute.maven.provider.MavenRepository;
import aQute.maven.provider.POM;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public class MavenBndRepoTest extends TestCase {
	String						tmpName;
	File						tmp;
	File						local;
	File						remote;
	File						index;

	private MavenBndRepository	repo;
	private FakeNexus			fnx;
	private Processor			reporter;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		tmpName = "generated/tmp/test/" + getName();
		tmp = IO.getFile(tmpName);
		IO.delete(tmp);
		local = IO.getFile(tmp, "local");
		remote = IO.getFile(tmp, "remote");
		index = IO.getFile(tmp, "index");
		remote.mkdirs();
		local.mkdirs();

		IO.copy(IO.getFile("testresources/mavenrepo"), remote);
		IO.copy(IO.getFile("testresources/mavenrepo/index.maven"), index);

		Config config = new Config();
		fnx = new FakeNexus(config, remote);
		fnx.start();
	}

	public void testPomGenerate() throws Exception {
		config(null);
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		PomOptions po = new PomOptions();
		po.gav = "test:test:1.0";
		po.parent = null;
		po.dependencyManagement = false;

		repo.toPom(bout, po);
		ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
		POM pom = new POM((MavenRepository) repo.storage, bin);

		assertEquals(Revision.valueOf("test:test:1.0"), pom.getRevision());
		assertEquals(3, pom.getDependencies(EnumSet.of(MavenScope.runtime), false).size());
		System.out.println(new String(bout.toByteArray(), StandardCharsets.UTF_8));
	}

	public void testSnapshotGetInRelease() {

	}

	public void testReleaseGetInSnapshot() {

	}

	public void testNoLocalCacheSpecfiied() {

	}

	public void testPutLocalTwiceNoSnapshot() throws Exception {
		Map<String,String> map = new HashMap<>();
		map.put("releaseUrl", remote.toURI().toString());
		config(map);
		File jar = IO.getFile("testresources/release.jar");
		PutResult put = repo.put(new FileInputStream(jar), null);

		assertIsFile(local, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0/biz.aQute.bnd.maven-3.2.0.jar");
		assertIsFile(local, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0/biz.aQute.bnd.maven-3.2.0.pom");
		put = repo.put(new FileInputStream(jar), null);

		Requirement wc = ResourceUtils.createWildcardRequirement();
		Collection<Capability> caps = repo.findProviders(Collections.singleton(wc)).get(wc);
		Set<Resource> resources = ResourceUtils.getResources(caps);
		assertEquals(2, resources.size());
		IdentityCapability bc = ResourceUtils.getIdentityCapability(resources.iterator().next());
		assertEquals("biz.aQute.bnd.maven", bc.osgi_identity());
	}

	public void testPutReleaseAndThenIndex() throws Exception {
		Workspace ws = Workspace.findWorkspace(IO.getFile("testdata/releasews"));
		Project p1 = ws.getProject("p1");
		Project indexProject = ws.getProject("index");

		Map<String,String> map = new HashMap<>();
		map.put("releaseUrl", remote.toURI().toString());
		config(map);

		repo.begin(indexProject);
		File jar = IO.getFile("testresources/release.jar");
		PutOptions po = new PutOptions();
		po.context = p1;
		PutResult put = repo.put(new FileInputStream(jar), po);

		File demoJar = IO.getFile("testresources/demo.jar");
		PutOptions indexPo = new PutOptions();
		indexPo.context = indexProject;
		put = repo.put(new FileInputStream(demoJar), indexPo);

		repo.end(indexProject);

		assertTrue(indexProject.check());
		assertTrue(IO.getFile(remote, "biz/aQute/bnd/demo/1.0.0/demo-1.0.0-index.xml").isFile());
	}

	public void testNoIndexFile() throws Exception {
		Map<String,String> map = new HashMap<>();
		map.put("index", "generated/does_not_exist");
		config(map);
		repo.list(null);
		assertTrue(reporter.check());
	}

	public void testGet() throws Exception {
		config(null);
		File file = repo.get("commons-cli:commons-cli", new Version("1.0.0"), null);
		assertNotNull(file);
		assertTrue(file.isFile());
	}

	public void testGetFileRepo() throws Exception {
		Map<String,String> map = new HashMap<>();
		map.put("releaseUrl", remote.toURI().toString());
		config(map);
		File file = repo.get("commons-cli:commons-cli", new Version("1.0.0"), null);
		assertNotNull(file);
		assertTrue(file.isFile());
	}

	/*
	 * Commons CLI 1.2 is in there as GAV & as BSN
	 */
	public void testGetViaBSNAndGAV() throws Exception {
		config(null);

		assertEquals(1, repo.list("org.apache.commons.cli").size());
		System.out.println(repo.list("org.apache.commons.cli"));
		System.out.println(repo.versions("org.apache.commons.cli"));
		File f12maven = repo.get("commons-cli:commons-cli", new Version("1.2.0"), null);
		File f12osgi = repo.get("org.apache.commons.cli", new Version("1.2.0"), null);

		assertEquals("commons-cli-1.2.jar", f12maven.getName());
		assertEquals(f12maven, f12osgi);
	}

	public void testList() throws Exception {
		config(null);
		List<String> l = repo.list(null);
		System.out.println(l);
		assertEquals(4, l.size());
		assertTrue(l.contains("commons-cli:commons-cli"));

		SortedSet<Version> versions = repo.versions("commons-cli:commons-cli");
		assertEquals("[1.0.0, 1.2.0, 1.4.0.SNAPSHOT]", versions.toString());

		versions = repo.versions("org.apache.commons.cli");
		assertEquals("[1.2.0]", versions.toString());

		Requirement all = ResourceUtils.createWildcardRequirement();
		Collection<Capability> providers = repo.findProviders(Collections.singleton(all)).get(all);
		Set<Resource> resources = ResourceUtils.getResources(providers);

		// there is only one bundle in the store
		assertEquals(1, resources.size());
	}

	public void testPutDefaultLocal() throws Exception {
		Map<String, String> map = new HashMap<>();
		map.put("releaseUrl", null);
		map.put("snapshotUrl", null);
		config(map);

		File jar = IO.getFile("testresources/release.jar");

		PutResult put = repo.put(new FileInputStream(jar), null);

		assertIsFile(local, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0/biz.aQute.bnd.maven-3.2.0.jar");
		assertIsFile(local, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0/biz.aQute.bnd.maven-3.2.0.pom");
		assertIsFile(local, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0/maven-metadata-local.xml");

		String s = IO.collect(index);
		assertTrue(s.contains("biz.aQute.bnd.maven"));
	}

	public void testPutDefaultLocalSnapshot() throws Exception {
		Map<String,String> map = new HashMap<>();
		map.put("releaseUrl", null);
		map.put("snapshotUrl", null);
		config(map);

		File jar = IO.getFile("testresources/snapshot.jar");

		PutResult put = repo.put(new FileInputStream(jar), null);

		assertIsFile(local, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-SNAPSHOT.jar");
		assertIsFile(local, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-SNAPSHOT.pom");
		assertIsFile(local, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/maven-metadata-local.xml");

		String s = IO.collect(index);
		assertFalse(s.contains("biz.aQute.bnd.maven"));
	}

	public void testPutDefaultLocalSnapshotFileRepo() throws Exception {
		Map<String,String> map = new HashMap<>();
		map.put("snapshotUrl", remote.toURI().toString());
		config(map);

		File jar = IO.getFile("testresources/snapshot.jar");

		PutResult put = repo.put(new FileInputStream(jar), null);

		assertIsFile(local, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-SNAPSHOT.jar");
		assertIsFile(local, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-SNAPSHOT.pom");

		String s = IO.collect(index);
		// snapshots not added to index
		assertFalse(s.contains("biz.aQute.bnd.maven"));
	}

	public void testPutRemoteSnapshot() throws Exception {
		Map<String,String> map = new HashMap<>();
		map.put("releaseUrl", null);
		map.put("snapshotUrl", fnx.getBaseURI() + "/repo/");
		config(map);

		File jar = IO.getFile("testresources/snapshot.jar");
		try (Processor context = new Processor();) {
			context.setProperty("-maven-release", "remote;snapshot=1");
			PutOptions put = new PutOptions();
			put.context = context;

			PutResult r = repo.put(new FileInputStream(jar), put);

			assertIsFile(local,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-SNAPSHOT.jar");
			assertIsFile(local,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-SNAPSHOT.pom");
			assertIsFile(local,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-SNAPSHOT-sources.jar");
			assertIsFile(local,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-SNAPSHOT-javadoc.jar");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000000-1.pom");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000000-1.jar");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000000-1-sources.jar");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000000-1-javadoc.jar");
			assertIsFile(remote, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/maven-metadata.xml");

			File f = IO.getFile(remote, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/maven-metadata.xml");
			String s = IO.collect(f);
			assertTrue(s.contains("3.2.0-19700101.000000"));
		}

		//
		// Now try to update it
		//

		try (Processor context = new Processor();) {
			context.setProperty("-maven-release", "remote;snapshot=10000");
			PutOptions put = new PutOptions();
			put.context = context;

			PutResult r = repo.put(new FileInputStream(jar), put);

			assertIsFile(local,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-SNAPSHOT.jar");
			assertIsFile(local,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-SNAPSHOT.pom");
			assertIsFile(local,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-SNAPSHOT-sources.jar");
			assertIsFile(local,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-SNAPSHOT-javadoc.jar");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000000-1.pom");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000000-1.jar");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000000-1-sources.jar");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000000-1-javadoc.jar");
			assertIsFile(remote, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/maven-metadata.xml");

			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000010-2.pom");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000010-2.jar");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000010-2-sources.jar");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000010-2-javadoc.jar");
			assertIsFile(remote, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/maven-metadata.xml");

			File f = IO.getFile(remote, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/maven-metadata.xml");
			String s = IO.collect(f);
			assertTrue(s.contains("<value>3.2.0-19700101.000000-1</value>"));
			assertTrue(s.contains("<value>3.2.0-19700101.000010-2</value>"));
		}

	}

	public void testPutRemoteSnapshotFileRepo() throws Exception {
		Map<String,String> map = new HashMap<>();
		map.put("snapshotUrl", remote.toURI().toString());
		config(map);

		File jar = IO.getFile("testresources/snapshot.jar");
		try (Processor context = new Processor();) {
			context.setProperty("-maven-release", "remote;snapshot=1");
			PutOptions put = new PutOptions();
			put.context = context;

			PutResult r = repo.put(new FileInputStream(jar), put);

			assertIsFile(local,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-SNAPSHOT.jar");
			assertIsFile(local,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-SNAPSHOT.pom");
			assertIsFile(local,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-SNAPSHOT-sources.jar");
			assertIsFile(local,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-SNAPSHOT-javadoc.jar");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000000-1.pom");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000000-1.jar");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000000-1-sources.jar");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000000-1-javadoc.jar");
			assertIsFile(remote, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/maven-metadata.xml");

			File f = IO.getFile(remote, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/maven-metadata.xml");
			String s = IO.collect(f);
			assertTrue(s.contains("3.2.0-19700101.000000"));
		}

		//
		// Now try to update it
		//

		try (Processor context = new Processor();) {
			context.setProperty("-maven-release", "remote;snapshot=10000");
			PutOptions put = new PutOptions();
			put.context = context;

			PutResult r = repo.put(new FileInputStream(jar), put);

			assertIsFile(local,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-SNAPSHOT.jar");
			assertIsFile(local,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-SNAPSHOT.pom");
			assertIsFile(local,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-SNAPSHOT-sources.jar");
			assertIsFile(local,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-SNAPSHOT-javadoc.jar");

			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000000-1.pom");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000000-1.jar");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000000-1-sources.jar");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000000-1-javadoc.jar");
			assertIsFile(remote, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/maven-metadata.xml");

			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000010-2.pom");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000010-2.pom.sha1");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000010-2.pom.md5");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000010-2.jar");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000010-2-sources.jar");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000010-2-javadoc.jar");
			assertIsFile(remote, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/maven-metadata.xml");

			File f = IO.getFile(remote, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/maven-metadata.xml");
			String s = IO.collect(f);
			assertTrue(s.contains("<value>3.2.0-19700101.000000-1</value>"));
			assertTrue(s.contains("<value>3.2.0-19700101.000010-2</value>"));
		}

		//
		// Now try to update it again, should increase build number
		//

		try (Processor context = new Processor();) {
			context.setProperty("-maven-release", "remote;snapshot=20000");
			PutOptions put = new PutOptions();
			put.context = context;

			PutResult r = repo.put(new FileInputStream(jar), put);

			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000020-3.pom");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000020-3.jar");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000020-3-sources.jar");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000020-3-javadoc.jar");
			assertIsFile(remote, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/maven-metadata.xml");

			File f = IO.getFile(remote, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/maven-metadata.xml");
			String s = IO.collect(f);
			assertTrue(s.contains("<value>3.2.0-19700101.000020-3</value>"));
		}

	}

	private void assertIsFile(File dir, String path) {
		if (!IO.getFile(dir, path).isFile())
			throw new AssertionFailedError(path + " does not exist");
	}

	void config(Map<String,String> override) throws Exception {
		Map<String,String> config = new HashMap<>();
		config.put("local", tmpName + "/local");
		config.put("index", tmpName + "/index");
		config.put("releaseUrl", fnx.getBaseURI() + "/repo/");

		if (override != null)
			config.putAll(override);

		reporter = new Processor();
		reporter.addBasicPlugin(new ProgressPlugin() {

			@Override
			public Task startTask(final String name, int size) {
				System.out.println("Starting " + name);
				return new Task() {

					@Override
					public void worked(int units) {
						System.out.println("Worked " + name + " " + units);
					}

					@Override
					public void done(String message, Throwable e) {
						System.out.println("Done " + name + " " + message + " " + e);
					}

					@Override
					public boolean isCanceled() {
						return false;
					}
				};
			}
		});
		HttpClient client = new HttpClient();
		client.setRegistry(reporter);
		Executor executor = Executors.newCachedThreadPool();
		reporter.addBasicPlugin(client);
		reporter.setTrace(true);

		repo = new MavenBndRepository();
		repo.setReporter(reporter);
		repo.setRegistry(reporter);

		repo.setProperties(config);
	}

	public void testPutPlainJarInRepo() throws Exception {

		Map<String,String> map = new HashMap<>();
		map.put("releaseUrl", remote.toURI().toString());
		config(map);

		File f = IO.getFile("testdata/plainjar/olingo-odata2-api-1.2.0.jar");
		try (FileInputStream in = new FileInputStream(f)) {
			repo.put(in, null);
		}
	}
}
