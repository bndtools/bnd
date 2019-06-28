package org.bndtools.utils.jar;

import java.io.File;
import java.util.jar.Manifest;

import aQute.lib.io.IO;
import junit.framework.TestCase;

public class PseudoJarTest extends TestCase {

	public void testReadJarFile() throws Exception {

		PseudoJar pj = new PseudoJar(new File("testdata/hello.jar"));
		try {
			Manifest mf = pj.readManifest();
			assertEquals("jellyfish", mf.getMainAttributes()
				.getValue("Arbitrary-Header"));

			assertEquals("OSGI-INF/", pj.nextEntry());

			assertEquals("OSGI-INF/goodbye.txt", pj.nextEntry());
			assertEquals("Goodbye World", IO.collect(pj.openEntry()));

			assertEquals("OSGI-INF/hello.txt", pj.nextEntry());
			assertEquals("Hello World", IO.collect(pj.openEntry()));
		} finally {
			pj.close();
		}
	}

	public void testReadDir() throws Exception {
		PseudoJar pj = new PseudoJar(new File("testdata/hello_jar_dir"));
		try {
			Manifest mf = pj.readManifest();
			assertEquals("jellyfish", mf.getMainAttributes()
				.getValue("Arbitrary-Header"));

			assertEquals("META-INF/", pj.nextEntry());
			assertEquals("META-INF/MANIFEST.MF", pj.nextEntry());
			assertEquals("OSGI-INF/", pj.nextEntry());

			assertEquals("OSGI-INF/goodbye.txt", pj.nextEntry());
			assertEquals("Goodbye World", IO.collect(pj.openEntry()));

			assertEquals("OSGI-INF/hello.txt", pj.nextEntry());
			assertEquals("Hello World", IO.collect(pj.openEntry()));
		} finally {
			pj.close();
		}
	}
}
