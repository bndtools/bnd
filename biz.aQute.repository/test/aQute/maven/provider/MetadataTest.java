package aQute.maven.provider;

import java.io.FileInputStream;
import java.io.InputStream;

import aQute.bnd.version.MavenVersion;
import aQute.lib.io.IO;
import aQute.maven.provider.MetadataParser.ProgramMetadata;
import aQute.maven.provider.MetadataParser.RevisionMetadata;
import aQute.maven.provider.MetadataParser.SnapshotVersion;
import junit.framework.TestCase;

public class MetadataTest extends TestCase {

	public void testProgramParsing() throws Exception {
		try (InputStream in = new FileInputStream(IO.getFile("testresources/parser/commons-dbcp.xml"))) {

			ProgramMetadata parse = MetadataParser.parseProgramMetadata(in);
			assertNotNull(parse);
			assertEquals("commons.dbcp", parse.group);
			assertEquals("commons-dbcp", parse.artifact);
			assertEquals(2, parse.versions.size());
			assertTrue(parse.versions.contains(new MavenVersion("1.4.1-SNAPSHOT")));
			assertTrue(parse.versions.contains(new MavenVersion("1.5-SNAPSHOT")));
		}
	}

	public void testSnapshotParsing() throws Exception {

		try (InputStream in = new FileInputStream(IO.getFile("testresources/parser/commons-dbcp-1.4.1-SNAPSHOT.xml"))) {

			RevisionMetadata parse = MetadataParser.parseRevisionMetadata(in);
			assertNotNull(parse);
			assertEquals("commons.dbcp", parse.group);
			assertEquals("commons-dbcp", parse.artifact);
			assertEquals(MavenVersion.parseMavenString("1.4.1-SNAPSHOT"), parse.version);

			assertNotNull(parse.snapshot);
			assertEquals("13", parse.snapshot.buildNumber);
			assertEquals("20140107.141700", parse.snapshot.timestamp);

			assertEquals(10, parse.snapshotVersions.size());

			SnapshotVersion snapshotVersion = parse.snapshotVersions.get(0);
			assertNotNull(snapshotVersion);
			assertEquals("bin", snapshotVersion.classifier);
			assertEquals("tar.gz", snapshotVersion.extension);
			assertEquals(MavenVersion.parseMavenString("1.4.1-20140107.141700-13"), snapshotVersion.value);
			assertEquals(1389104220000L, snapshotVersion.updated);

			snapshotVersion = parse.snapshotVersions.get(9);
			assertNotNull(snapshotVersion);
			assertNull(snapshotVersion.classifier);
			assertEquals("pom", snapshotVersion.extension);
			assertEquals(MavenVersion.parseMavenString("1.4.1-20140107.141700-13"), snapshotVersion.value);
			assertEquals(1389104220000L, snapshotVersion.updated);
		}
	}
}
