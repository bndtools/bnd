package aQute.bnd.repository.maven.provider;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.RepositoryPlugin.PutOptions;
import aQute.bnd.service.RepositoryPlugin.PutResult;
import aQute.bnd.version.Version;
import aQute.http.testservers.HttpTestServer.Config;
import aQute.lib.io.IO;
import aQute.maven.provider.FakeNexus;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public class MavenBndRepoTest extends TestCase {
	File	tmp		= IO.getFile("generated/tmp");
	File	local	= IO.getFile(tmp, "local");
	File	remote	= IO.getFile(tmp, "remote");
	File	index	= IO.getFile(tmp, "index");

	private MavenBndRepository	repo;
	private FakeNexus			fnx;
	private Processor			reporter;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IO.delete(tmp);
		remote.mkdirs();
		local.mkdirs();

		IO.copy(IO.getFile("testresources/mavenrepo"), remote);
		IO.copy(IO.getFile("testresources/mavenrepo/index.maven"), index);

		Config config = new Config();
		fnx = new FakeNexus(config, remote);
		fnx.start();
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
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000010-1.pom");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000010-1.jar");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000010-1-sources.jar");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000010-1-javadoc.jar");
			assertIsFile(remote, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/maven-metadata.xml");

			File f = IO.getFile(remote, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/maven-metadata.xml");
			String s = IO.collect(f);
			assertTrue(s.contains("<value>3.2.0-19700101.000000-1</value>"));
			assertTrue(s.contains("<value>3.2.0-19700101.000010-1</value>"));
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
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000010-1.pom");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000010-1.pom.sha1");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000010-1.pom.md5");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000010-1.jar");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000010-1-sources.jar");
			assertIsFile(remote,
					"biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/biz.aQute.bnd.maven-3.2.0-19700101.000010-1-javadoc.jar");
			assertIsFile(remote, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/maven-metadata.xml");

			File f = IO.getFile(remote, "biz/aQute/bnd/biz.aQute.bnd.maven/3.2.0-SNAPSHOT/maven-metadata.xml");
			String s = IO.collect(f);
			assertTrue(s.contains("<value>3.2.0-19700101.000000-1</value>"));
			assertTrue(s.contains("<value>3.2.0-19700101.000010-1</value>"));
		}

	}
	private void assertIsFile(File dir, String path) {
		if (!IO.getFile(dir, path).isFile())
			throw new AssertionFailedError(path + " does not exist");
	}

	void config(Map<String,String> override) throws Exception {
		Map<String,String> config = new HashMap<>();
		config.put("local", "generated/tmp/local");
		config.put("index", "generated/tmp/index");
		config.put("releaseUrl", fnx.getBaseURI() + "/repo/");

		if (override != null)
			config.putAll(override);

		reporter = new Processor();
		reporter.setTrace(true);
		reporter.trace("test");
		HttpClient client = new HttpClient();
		Executor executor = Executors.newCachedThreadPool();
		reporter.addBasicPlugin(client);
		reporter.setTrace(true);

		repo = new MavenBndRepository();
		repo.setReporter(reporter);
		repo.setRegistry(reporter);

		repo.setProperties(config);
	}
}
