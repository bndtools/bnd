package test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.lib.io.IO;
import junit.framework.TestCase;

@SuppressWarnings("resource")
public class CorruptManifest extends TestCase {
	static String	ltext	= "bla bla \nbla bla bla bla \nbla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla ";
	static String	rtext	= "bla bla  bla bla bla bla  bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla ";

	public static void testCorruptJar() throws Exception {
		Builder b = new Builder();
		b.setProperty("NL1", "\n");
		b.setProperty("NL2", "\r\n");
		b.setProperty("NL3", ".");
		b.setProperty("NL4", ".\n.\n");
		b.setProperty("NL5", ltext);
		b.setProperty("Export-Package", "*");
		b.setClasspath(new File[] {
			IO.getFile("jar/asm.jar")
		});
		Jar jar = b.build();
		Manifest manifest = jar.getManifest();
		jar.writeManifest(System.err);

		Attributes main = manifest.getMainAttributes();
		assertNull(main.getValue("NL1"));
		assertNull(main.getValue("NL2"));
		assertEquals(".", main.getValue("NL3"));
		assertEquals(".\n.\n", main.getValue("NL4"));
		assertEquals(ltext, main.getValue("NL5"));

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		jar.writeManifest(bout);
		bout.flush();
		System.err.println("-----");
		System.err.write(bout.toByteArray());
		System.err.println("-----");
		ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
		manifest = new Manifest(bin);

		main = manifest.getMainAttributes();
		assertNull(main.getValue("NL1"));
		assertNull(main.getValue("NL2"));
		assertEquals(".", main.getValue("NL3"));
		assertEquals(". . ", main.getValue("NL4"));
		assertEquals(rtext, main.getValue("NL5"));
	}
}
