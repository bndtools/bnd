package aQute.maven.provider;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;
import java.util.Properties;

import aQute.bnd.http.HttpClient;
import aQute.bnd.version.MavenVersion;
import aQute.http.testservers.HttpTestServer.Config;
import aQute.lib.io.IO;
import aQute.libg.reporter.ReporterAdapter;
import aQute.maven.api.Archive;
import aQute.maven.api.Program;
import aQute.maven.api.Release;
import aQute.maven.api.Revision;
import junit.framework.TestCase;

public class MavenRepoTest extends TestCase {
	File							aFile		= IO.getFile("testresources/empty");

	String							tmpName;
	File							local;
	File							remote;
	FakeNexus						fnx;
	List<MavenBackingRepository>	repo;
	MavenRepository					storage;
	ReporterAdapter					reporter	= new ReporterAdapter(System.err);

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		tmpName = "generated/tmp/test/" + getClass().getName() + "/" + getName();
		local = IO.getFile(tmpName + "/local");
		remote = IO.getFile(tmpName + "/remote");
		reporter.setTrace(true);
		Config config = new Config();
		fnx = new FakeNexus(config, remote);
		fnx.start();
		IO.delete(remote);
		IO.delete(local);
		IO.copy(IO.getFile("testresources/mavenrepo"), remote);
		remote.mkdirs();
		local.mkdirs();
		HttpClient client = new HttpClient();
		repo = MavenBackingRepository.create(fnx.getBaseURI() + "/repo/", reporter, local, client);
		storage = new MavenRepository(local, "fnexus", this.repo, this.repo, client.promiseFactory()
			.executor(), null);
	}

	@Override
	protected void tearDown() throws Exception {
		fnx.close();
		super.tearDown();
	}

	public void testBasic() throws Exception {
		Program program = Program.valueOf("commons-cli", "commons-cli");
		List<Revision> revisions = storage.getRevisions(program);
		assertNotNull(revisions);
		assertEquals(3, revisions.size());
		Revision revision = program.version(new MavenVersion("1.4-SNAPSHOT"));
		assertTrue(revisions.contains(revision));

		List<Archive> snapshotArchives = storage.getSnapshotArchives(revision);
		assertNotNull(snapshotArchives);
		assertEquals(10, snapshotArchives.size());

		Archive archive = storage.getResolvedArchive(revision, Archive.POM_EXTENSION, null);
		assertNotNull(archive);
		assertEquals("1.4-20160119.062305-9", archive.snapshot.toString());

		File file = storage.get(archive)
			.getValue();
		assertNotNull(file);
		assertEquals(10373L, file.length());

	}

	public void testSnapshotCaches() throws Exception {
		File fpom = IO.getFile(local, "commons-cli/commons-cli/1.4-SNAPSHOT/commons-cli-1.4-SNAPSHOT.pom");
		Program program = Program.valueOf("commons-cli", "commons-cli");
		Revision revision = Program.valueOf("commons-cli", "commons-cli")
			.version("1.4-SNAPSHOT");
		Archive apom = revision.archive(Archive.POM_EXTENSION, null);
		assertFalse(fpom.exists());
		assertFalse(apom.isResolved());

		File f = storage.get(apom)
			.getValue();
		assertEquals(fpom.getAbsolutePath(), f.getAbsolutePath());
		assertRecent(f);
		long flastModified = f.lastModified();
		Thread.sleep(1001);

		f = storage.get(apom)
			.getValue();
		assertEquals(flastModified, f.lastModified());

		f.setLastModified(0);
		assertFalse(Math.abs(System.currentTimeMillis() - f.lastModified()) <= 2000);
		f = storage.get(apom)
			.getValue();
		assertFalse(f.lastModified() != 0);
	}

	void assertRecent(File f) {
		assertTrue(Math.abs(System.currentTimeMillis() - f.lastModified()) <= 20000);
	}

	public void testImmutable() throws Exception {
		File fpom = IO.getFile(local, "commons-cli/commons-cli/1.2/commons-cli-1.2.pom");
		Program program = Program.valueOf("commons-cli", "commons-cli");
		Revision revision = Program.valueOf("commons-cli", "commons-cli")
			.version("1.2");
		Archive apom = revision.archive("pom", null);
		assertFalse(fpom.exists());

		Archive rapom = storage.resolveSnapshot(apom);
		assertTrue(rapom.isResolved());
		assertEquals(rapom, apom);

		File f = storage.get(rapom)
			.getValue();
		assertEquals(fpom, f);
		assertRecent(f);

		f.setLastModified(10000);
		f = storage.get(rapom)
			.getValue();
		assertEquals(fpom, f);
		assertEquals(10000L, f.lastModified());

	}

	public void testBasicSnapshotRelease() throws Exception {
		File fpom = IO.getFile(local, "commons-cli/commons-cli/1.4-SNAPSHOT/commons-cli-1.4-SNAPSHOT.pom");
		File rpom = IO.getFile(remote, "commons-cli/commons-cli/1.4-SNAPSHOT/commons-cli-1.4-19700101.000010-10.pom");
		Program program = Program.valueOf("commons-cli", "commons-cli");
		Revision revision = Program.valueOf("commons-cli", "commons-cli")
			.version("1.4-SNAPSHOT");
		Archive apom = revision.archive(Archive.POM_EXTENSION, null);
		assertFalse(fpom.exists());

		Release r = storage.release(revision, new Properties());
		r.setBuild(10000, null);
		r.add(Archive.POM_EXTENSION, null, new ByteArrayInputStream(new byte[0]));

		r.close();

		assertTrue(fpom.isFile());
		assertTrue(rpom.isFile());

	}
}
