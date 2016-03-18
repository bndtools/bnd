package aQute.maven.repo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;

import aQute.bnd.http.HttpClient;
import aQute.bnd.version.MavenVersion;
import aQute.lib.io.IO;
import aQute.libg.reporter.ReporterAdapter;
import aQute.maven.repo.api.Archive;
import aQute.maven.repo.api.Program;
import aQute.maven.repo.api.Release;
import aQute.maven.repo.api.Revision;
import aQute.maven.repo.provider.MavenStorage;
import aQute.maven.repo.provider.RemoteRepo;
import biz.aQute.http.testservers.HttpTestServer.Config;
import junit.framework.TestCase;

public class MavenRepoTest extends TestCase {
	File aFile = IO.getFile("testresources/empty");

	File			local	= IO.getFile("generated/local");
	File			remote	= IO.getFile("generated/remote");
	FakeNexus		fnx;
	RemoteRepo		repo;
	MavenStorage	storage;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Config config = new Config();
		fnx = new FakeNexus(config, remote);
		fnx.start();
		IO.delete(remote);
		IO.delete(local);
		IO.copy(IO.getFile("testresources/mavenrepo"), remote);
		remote.mkdirs();
		local.mkdirs();
		repo = new RemoteRepo(new HttpClient(), fnx.getBaseURI() + "/repo/");
		storage = new MavenStorage(local, "fnexus", this.repo, null, null, new ReporterAdapter(System.out), null);
	}

	@Override
	protected void tearDown() throws Exception {
		fnx.close();
		super.tearDown();
	}

	public void testBasic() throws Exception {
		File pmeta = IO.getFile(local, "commons-cli/commons-cli/maven-metadata-fnexus.xml");
		File fmeta = IO.getFile(local, "commons-cli/commons-cli/1.4-SNAPSHOT/maven-metadata-fnexus.xml");

		assertFalse(pmeta.isFile());

		Program program = Program.valueOf("commons-cli", "commons-cli");
		List<Revision> revisions = storage.getRevisions(program);
		assertNotNull(revisions);
		assertEquals(3, revisions.size());
		Revision revision = program.version(new MavenVersion("1.4-SNAPSHOT"));
		assertTrue(revisions.contains(revision));

		assertTrue(pmeta.isFile());

		List<Archive> snapshotArchives = storage.getSnapshotArchives(revision);
		assertNotNull(snapshotArchives);
		assertEquals(10, snapshotArchives.size());

		assertTrue(fmeta.isFile());

		Archive archive = storage.getResolvedArchive(revision, "pom", null);
		assertNotNull(archive);
		assertEquals("1.4-20160119.062305-9", archive.snapshot.toString());

		File file = storage.get(archive).getValue();
		assertNotNull(file);
		assertEquals(10373L, file.length());

	}

	public void testCachesAreUsed() throws Exception {
		File pmeta = IO.getFile(local, "commons-cli/commons-cli/maven-metadata-fnexus.xml");
		File fmeta = IO.getFile(local, "commons-cli/commons-cli/1.4-SNAPSHOT/maven-metadata-fnexus.xml");
		Program program = Program.valueOf("commons-cli", "commons-cli");
		Revision revision = Program.valueOf("commons-cli", "commons-cli").version("1.4-SNAPSHOT");

		//
		// Read meta
		//

		assertFalse(pmeta.isFile());
		storage.getRevisions(program);
		assertTrue(pmeta.isFile());
		assertRecent(pmeta);
		long plastModified = pmeta.lastModified();

		assertFalse(fmeta.isFile());
		storage.getSnapshotArchives(revision);
		assertTrue(fmeta.isFile());
		assertRecent(fmeta);
		long flastModified = fmeta.lastModified();

		//
		// Delay a bit to make sure we get a new modified time
		//

		Thread.sleep(1001);

		storage.getRevisions(program);
		assertEquals(plastModified, pmeta.lastModified());

		storage.getSnapshotArchives(revision);
		assertEquals(flastModified, fmeta.lastModified());

		//
		// Set modtime in the past
		//

		pmeta.setLastModified(10000);
		storage.getRevisions(program);
		assertRecent(pmeta);

		fmeta.setLastModified(10000);
		storage.getSnapshotArchives(revision);
		assertRecent(fmeta);

	}

	public void testSnapshotCaches() throws Exception {
		File fpom = IO.getFile(local, "commons-cli/commons-cli/1.4-SNAPSHOT/commons-cli-1.4-SNAPSHOT.pom");
		Program program = Program.valueOf("commons-cli", "commons-cli");
		Revision revision = Program.valueOf("commons-cli", "commons-cli").version("1.4-SNAPSHOT");
		Archive apom = revision.archive("pom", null);
		assertFalse(fpom.exists());
		assertFalse(apom.isResolved());

		File f = storage.get(apom).getValue();
		assertEquals(fpom.getAbsolutePath(), f.getAbsolutePath());
		assertRecent(f);
		long flastModified = f.lastModified();
		Thread.sleep(1001);

		f = storage.get(apom).getValue();
		assertEquals(flastModified, f.lastModified());

		f.setLastModified(10000);
		assertFalse(Math.abs(System.currentTimeMillis() - f.lastModified()) <= 2000);
		f = storage.get(apom).getValue();
		assertRecent(f);
	}

	void assertRecent(File f) {
		assertTrue(Math.abs(System.currentTimeMillis() - f.lastModified()) <= 2000);
	}

	public void testImmutable() throws Exception {
		File fpom = IO.getFile(local, "commons-cli/commons-cli/1.2/commons-cli-1.2.pom");
		Program program = Program.valueOf("commons-cli", "commons-cli");
		Revision revision = Program.valueOf("commons-cli", "commons-cli").version("1.2");
		Archive apom = revision.archive("pom", null);
		assertFalse(fpom.exists());

		Archive rapom = storage.resolveSnapshot(apom);
		assertTrue(rapom.isResolved());
		assertEquals(rapom, apom);

		File f = storage.get(rapom).getValue();
		assertEquals(fpom, f);
		assertRecent(f);

		f.setLastModified(10000);
		f = storage.get(rapom).getValue();
		assertEquals(fpom, f);
		assertEquals(10000L, f.lastModified());

	}

	public void testBasicSnapshotRelease() throws Exception {
		File fpom = IO.getFile(local, "commons-cli/commons-cli/1.4-SNAPSHOT/commons-cli-1.4-SNAPSHOT.pom");
		File rpom = IO.getFile(remote, "commons-cli/commons-cli/1.4-SNAPSHOT/commons-cli-1.4-19700101.000010.pom");
		Program program = Program.valueOf("commons-cli", "commons-cli");
		Revision revision = Program.valueOf("commons-cli", "commons-cli").version("1.4-SNAPSHOT");
		Archive apom = revision.archive("pom", null);
		assertFalse(fpom.exists());

		Release r = storage.release(revision);
		r.setBuild(10000, null);
		r.add("pom", null, new ByteArrayInputStream(new byte[0]));

		r.close();

		assertTrue(fpom.isFile());
		assertTrue(rpom.isFile());

	}
}
