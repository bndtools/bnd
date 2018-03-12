package test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.lib.io.IO;
import junit.framework.TestCase;

@SuppressWarnings("resource")

public class JarTest extends TestCase {

	public void testDeletePrefix() {
		Resource r = new EmbeddedResource(new byte[1], 0);

		Jar jar = new Jar("test");
		jar.putResource("META-INF/maven/org/osgi/test/test.pom", r);
		jar.putResource("META-INF/maven/org/osgi/test/test.properties", r);
		jar.putResource("META-INF/MANIFEST.MF", r);
		jar.putResource("com/example/foo.jar", r);

		assertTrue(jar.getDirectories().containsKey("META-INF/maven/org/osgi/test"));
		assertTrue(jar.getDirectories().containsKey("META-INF/maven"));
		jar.removePrefix("META-INF/maven/");

		assertNotNull(jar.getResource("META-INF/MANIFEST.MF"));
		assertNotNull(jar.getResource("com/example/foo.jar"));
		assertNull(jar.getResource("META-INF/maven/org/osgi/test/test.pom"));
		assertNull(jar.getResource("META-INF/maven/org/osgi/test/test.properties"));

		assertFalse(jar.getDirectories().containsKey("META-INF/maven"));
		assertFalse(jar.getDirectories().containsKey("META-INF/maven/org/osgi/test"));
	}

	public static void testWriteFolder() throws Exception {
		File tmp = IO.getFile("generated/tmp");
		IO.delete(tmp);
		tmp.mkdirs();

		Builder b = new Builder();
		b.setIncludeResource("/a/b.txt;literal='ab', /a/c.txt;literal='ac', /a/c/d/e.txt;literal='acde'");
		b.build();
		assertTrue(b.check());

		b.getJar().writeFolder(tmp);

		assertTrue(IO.getFile(tmp, "META-INF/MANIFEST.MF").isFile());
		assertEquals("ab", IO.collect(IO.getFile(tmp, "a/b.txt")));
		assertEquals("acde", IO.collect(IO.getFile(tmp, "a/c/d/e.txt")));

		IO.delete(tmp);
	}

	public static void testNoManifest() throws Exception {
		Jar jar = new Jar("dot");
		jar.setManifest(new Manifest());
		jar.setDoNotTouchManifest();
		jar.putResource("a/b", new FileResource(IO.getFile("testresources/bnd.jar")));

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		jar.write(bout);

		Jar jin = new Jar("dotin", new ByteArrayInputStream(bout.toByteArray()));
		Resource m = jin.getResource("META-INF/MANIFEST.MF");
		assertNull(m);
		Resource r = jin.getResource("a/b");
		assertNotNull(r);
	}

	public static void testManualManifest() throws Exception {
		Jar jar = new Jar("dot");
		jar.setManifest(new Manifest());
		jar.setDoNotTouchManifest();
		jar.putResource("a/b", new FileResource(IO.getFile("testresources/bnd.jar")));
		jar.putResource("META-INF/MANIFEST.MF",
				new EmbeddedResource("Manifest-Version: 1\r\nX: 1\r\n\r\n".getBytes(), 0));

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		jar.write(bout);

		JarInputStream jin = new JarInputStream(new ByteArrayInputStream(bout.toByteArray()));
		Manifest m = jin.getManifest();
		assertNotNull(m);
		assertEquals("1", m.getMainAttributes().getValue("X"));
		jin.close();
	}

	public static void testRenameManifest() throws Exception {
		Jar jar = new Jar("dot");
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().putValue("X", "1");
		jar.setManifest(manifest);
		jar.setManifestName("META-INF/FESTYMAN.MF");

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		jar.write(bout);

		ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(bout.toByteArray()));
		ZipEntry firstEntry = zin.getNextEntry();
		if (firstEntry.getName()
			.equalsIgnoreCase("META-INF/")) {
			firstEntry = zin.getNextEntry();
		}
		assertEquals("META-INF/FESTYMAN.MF", firstEntry.getName());
		manifest = new Manifest(zin);

		assertEquals("1", manifest.getMainAttributes().getValue("X"));
		zin.close();
	}

	public static void testSimple() throws ZipException, IOException {
		File file = IO.getFile("jar/asm.jar");
		Jar jar = new Jar("asm.jar", file);
		long jarTime = jar.lastModified();
		long fileTime = file.lastModified();
		long now = System.currentTimeMillis();

		// Sanity check
		assertTrue(jarTime < fileTime);
		assertTrue(fileTime <= now);

		// TODO see if we can improve this test case
		// // We should use the highest modification time
		// // of the files in the JAR not the JAR (though
		// // this is a backup if time is not set in the jar)
		// assertEquals(1144412850000L, jarTime);

		// Now add the file and check that
		// the modification time has changed
		jar.putResource("asm", new FileResource(file));
		assertEquals(file.lastModified(), jar.lastModified());
	}

	public static void testNewLine() throws Exception {
		Jar jar = new Jar("dot");
		Manifest manifest = new Manifest();
		jar.setManifest(manifest);

		String value = "Test\nTest\nTest\nTest";
		String expectedValue = "Test Test Test Test";

		manifest.getMainAttributes().putValue(Constants.BUNDLE_DESCRIPTION, value);

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		jar.write(bout);

		JarInputStream jin = new JarInputStream(new ByteArrayInputStream(bout.toByteArray()));
		Manifest m = jin.getManifest();
		assertNotNull(m);

		String parsedValue = m.getMainAttributes().getValue(Constants.BUNDLE_DESCRIPTION);

		assertEquals(expectedValue, parsedValue);
	}
}
