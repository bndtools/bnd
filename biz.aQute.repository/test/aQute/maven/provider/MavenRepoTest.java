package aQute.maven.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import aQute.bnd.http.HttpClient;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.bnd.version.MavenVersion;
import aQute.http.testservers.HttpTestServer.Config;
import aQute.lib.io.IO;
import aQute.libg.reporter.ReporterAdapter;
import aQute.maven.api.Archive;
import aQute.maven.api.Program;
import aQute.maven.api.Release;
import aQute.maven.api.Revision;

public class MavenRepoTest {
	File							aFile		= IO.getFile("testresources/empty");
	File							local;
	File							remote;
	FakeNexus						fnx;
	List<MavenBackingRepository>	repo;
	MavenRepository					storage;
	ReporterAdapter					reporter	= new ReporterAdapter(System.err);
	HttpClient						client		= new HttpClient();

	@BeforeEach
	protected void setUp(@InjectTemporaryDirectory
	File tmp) throws Exception {
		local = IO.getFile(tmp, "local");
		remote = IO.getFile(tmp, "remote");
		remote.mkdirs();
		local.mkdirs();
		reporter.setTrace(true);
		Config config = new Config();
		fnx = new FakeNexus(config, remote);
		fnx.start();
		IO.copy(IO.getFile("testresources/mavenrepo"), remote);
		repo = MavenBackingRepository.create(fnx.getBaseURI() + "/repo/", reporter, local, client);
		storage = new MavenRepository(local, "fnexus", this.repo, this.repo, client.promiseFactory()
			.executor(), null);
	}

	@AfterEach
	protected void tearDown() throws Exception {
		fnx.close();
	}

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	/**
	 * Nexus has a long outstanding bug that it can create multiple staging
	 * repositories. If something has staging in the URL, we wait 5 seconds
	 * after the first upload to mitigate this bug.
	 */

	@Test
	public void testStagingRelease() throws Exception {
		List<MavenBackingRepository> repo = MavenBackingRepository.create(fnx.getBaseURI() + "/staging/", reporter,
			local, client);
		try (MavenRepository storage = new MavenRepository(local, "fnexus", repo, repo, client.promiseFactory()
			.executor(), null)) {
			Program program = Program.valueOf("org.osgi", "org.osgi.dto");
			Revision revision = program.version("1.0.0");
			Archive apom = revision.archive(Archive.POM_EXTENSION, null);

			File fpom = IO.getFile(local, apom.localPath);
			File rpom = IO.getFile(remote, apom.remotePath);
			assertFalse(fpom.exists());

			long now = System.currentTimeMillis();
			Release r = storage.release(revision, new Properties());
			r.add(Archive.POM_EXTENSION, null, new ByteArrayInputStream(new byte[0]));

			r.close();
			assertThat(System.currentTimeMillis() - now).isGreaterThan(5000L);
			assertTrue(fpom.isFile());
			assertTrue(rpom.isFile());
		}
	}

}
