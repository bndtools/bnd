package aQute.maven.api;

import aQute.bnd.version.MavenVersion;
import junit.framework.TestCase;

public class ArchiveTest extends TestCase {

	public void testFilePath() {
		String s = "com/netflix/governator/governator-commons-cli/1.12.10/governator-commons-cli-1.12.10.pom";
		Archive a = Archive.fromFilepath(s);

		assertEquals("com.netflix.governator", a.revision.program.group);
		assertEquals("governator-commons-cli", a.revision.program.artifact);
		assertEquals("1.12.10", a.revision.version.toString());
		assertEquals("pom", a.extension);
		assertEquals("", a.classifier);
	}

	public void testFilePathWithClassifier() {
		String s = "com/netflix/governator/governator-commons-cli/1.12.10/governator-commons-cli-1.12.10-classifier.pom";
		Archive a = Archive.fromFilepath(s);

		assertEquals("com.netflix.governator", a.revision.program.group);
		assertEquals("governator-commons-cli", a.revision.program.artifact);
		assertEquals("1.12.10", a.revision.version.toString());
		assertEquals("pom", a.extension);
		assertEquals("classifier", a.classifier);
	}

	public void testEquals() {
		Archive a = Archive.valueOf("a.b.c:def:jar:1.3");
		Archive b = Archive.valueOf("a.b.c:def:1.3");
		assertTrue(a.equals(b));
		assertTrue(a.hashCode() == b.hashCode());
	}

	public void testEqualsDefault() {
		Archive a = new Archive("org.slf4j:slf4j-api:1.7.5");
		Archive b = Archive.valueOf("org.slf4j:slf4j-api:1.7.5");
		assertTrue(a.equals(b));
		assertTrue(a.hashCode() == b.hashCode());
	}

	public void testValueOf() {
		Archive a = Archive.valueOf("a.b.c:def:1.3");
		assertEquals("a.b.c", a.revision.program.group);
		assertEquals("def", a.revision.program.artifact);
		assertEquals(new MavenVersion("1.3"), a.revision.version);
		assertEquals("jar", a.extension);
		assertEquals("", a.classifier);
		assertEquals("a/b/c/def/1.3/def-1.3.jar", a.localPath);
		assertEquals("a/b/c/def/1.3/def-1.3.jar", a.remotePath);
	}

	public void testValueOfWithExtension() {
		Archive a = Archive.valueOf("a.b.c:def:ext:1.3");
		assertEquals("a.b.c", a.revision.program.group);
		assertEquals("def", a.revision.program.artifact);
		assertEquals(new MavenVersion("1.3"), a.revision.version);
		assertEquals("ext", a.extension);
		assertEquals("", a.classifier);
		assertEquals("a/b/c/def/1.3/def-1.3.ext", a.localPath);
		assertEquals("a/b/c/def/1.3/def-1.3.ext", a.remotePath);
	}

	public void testValueOfWithExtensionAndClassifier() {
		Archive a = Archive.valueOf("a.b.c:def:ext:class:1.3");
		assertEquals("a.b.c", a.revision.program.group);
		assertEquals("def", a.revision.program.artifact);
		assertEquals(new MavenVersion("1.3"), a.revision.version);
		assertEquals("ext", a.extension);
		assertEquals("class", a.classifier);
		assertEquals("a/b/c/def/1.3/def-1.3-class.ext", a.localPath);
		assertEquals("a/b/c/def/1.3/def-1.3-class.ext", a.remotePath);
	}

	public void testDefaultExtension() {
		Archive a = Archive.valueOf("a.b.c:def:jar:1.3");
		Archive b = Archive.valueOf("a.b.c:def:1.3");
		assertTrue(a.equals(b));
	}

	public void testName() {
		Archive a = Archive.valueOf("a.b.c:def:1.3");
		assertEquals("def-1.3.jar", a.getName());
	}

	public void testPom() {
		Archive a = Archive.valueOf("a.b.c:def:1.3");
		Archive pomArchive = a.getPomArchive();
		assertEquals("pom", pomArchive.extension);
		assertEquals("", pomArchive.classifier);
		assertEquals("a/b/c/def/1.3/def-1.3.pom", pomArchive.localPath);
		assertEquals("a/b/c/def/1.3/def-1.3.pom", pomArchive.remotePath);

		assertTrue(pomArchive.isPom());
		assertFalse(a.isPom());
	}

	public void testSnapshot() {
		Archive a = Archive.valueOf("a.b.c:def:1.3-SNAPSHOT");
		assertTrue(a.isSnapshot());
		assertFalse(a.isResolved());

		Archive resolveSnapshot = a.resolveSnapshot(new MavenVersion("123456789"));
		assertTrue(resolveSnapshot.isSnapshot());
		assertTrue(resolveSnapshot.isResolved());

		assertEquals("a/b/c/def/1.3-SNAPSHOT/def-1.3-SNAPSHOT.jar", resolveSnapshot.localPath);
		assertEquals("a/b/c/def/1.3-SNAPSHOT/def-123456789.jar", resolveSnapshot.remotePath);
	}
}
