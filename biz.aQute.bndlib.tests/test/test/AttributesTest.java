package test;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.lib.io.IO;
import junit.framework.TestCase;

@SuppressWarnings("resource")
public class AttributesTest extends TestCase {

	/**
	 * Remove a version attribute A mandatory attribute adds the common and tst
	 * properties to the import. We remove them using remove:=*
	 *
	 * @throws Exception
	 */
	public static void testRemoveDirective() throws Exception {
		Jar javax = new Jar("test");
		Manifest m = new Manifest();
		m.getMainAttributes()
			.putValue("Export-Package",
				"javax.microedition.io;a1=exp-1;a2=exp-2;a3=exp-3;x1=x1;x2=x2;x3=x3;mandatory:=\"a1,a2,a3,x1,x2,x3\"");
		javax.setManifest(m);

		Jar cp[] = {
			javax, new Jar(IO.getFile("jar/osgi.jar"))
		};
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.put("Import-Package", "javax.microedition.io;-remove-attribute:=a1|x?;a2=imp-2,*");
		p.put("Export-Package", "org.osgi.service.io");
		bmaker.setClasspath(cp);
		bmaker.setProperties(p);
		Jar jar = bmaker.build();
		assertTrue(bmaker.check());
		assertNotNull(bmaker.getImports());
		assertNotNull(bmaker.getImports()
			.getByFQN("javax.microedition.io"));
		assertNotNull(bmaker.getImports()
			.getByFQN("javax.microedition.io")
			.get("a2"));
		jar.getManifest()
			.write(System.err);
		Manifest manifest = jar.getManifest();
		Attributes main = manifest.getMainAttributes();
		String imprt = main.getValue("Import-Package");
		assertNotNull("Import package header", imprt);
		Parameters map = Processor.parseHeader(imprt, null);
		System.err.println("** " + map);
		Map<String, String> attrs = map.get("javax.microedition.io");
		assertNotNull(attrs);
		assertNull(attrs.get("a1"));
		assertNull(attrs.get("x1"));
		assertNull(attrs.get("x2"));
		assertNull(attrs.get("x3"));
		assertEquals("imp-2", attrs.get("a2"));
		assertEquals("exp-3", attrs.get("a3"));
	}

	/**
	 * Remove a version attribute
	 *
	 * @throws Exception
	 */
	public static void testRemoveAttribute() throws Exception {
		Jar javax = new Jar("test");
		Manifest m = new Manifest();
		m.getMainAttributes()
			.putValue("Export-Package", "javax.microedition.io;common=split;test=abc;mandatory:=\"common,test\"");
		javax.setManifest(m);

		Jar cp[] = {
			javax, new Jar(IO.getFile("jar/osgi.jar"))
		};
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.put("Import-Package", "javax.microedition.io;common=!;test=abc,*");
		p.put("Export-Package", "org.osgi.service.io");
		bmaker.setClasspath(cp);
		bmaker.setProperties(p);
		Jar jar = bmaker.build();
		assertTrue(bmaker.check());

		jar.getManifest()
			.write(System.err);
		Manifest manifest = jar.getManifest();
		Attributes main = manifest.getMainAttributes();
		String imprt = main.getValue("Import-Package");
		assertNotNull("Import package header", imprt);
		Parameters map = Processor.parseHeader(imprt, null);
		Map<String, String> attrs = map.get("javax.microedition.io");
		assertNotNull(attrs);
		assertNull(attrs.get("common"));
	}

	/**
	 * Override a version attribute
	 *
	 * @throws Exception
	 */
	public static void testOverrideAttribute() throws Exception {
		File cp[] = {
			IO.getFile("jar/osgi.jar")
		};
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.put("Export-Package", "org.osgi.framework;version=1.1");
		bmaker.setClasspath(cp);
		bmaker.setProperties(p);
		Jar jar = bmaker.build();
		System.err.println(jar.getResources());
		// System.err.println(bmaker.getExports());
		System.err.println("Warnings: " + bmaker.getWarnings());
		System.err.println("Errors  : " + bmaker.getErrors());
		jar.getManifest()
			.write(System.err);
		Manifest manifest = jar.getManifest();
		Attributes main = manifest.getMainAttributes();
		String export = main.getValue("Export-Package");
		assertNotNull("Export package header", export);
		Parameters map = Processor.parseHeader(export, null);
		assertEquals("1.1", map.get("org.osgi.framework")
			.get("version"));
	}

	/**
	 * See if we inherit the version from the osgi.jar file.
	 *
	 * @throws Exception
	 */
	public static void testSimple() throws Exception {
		File cp[] = {
			IO.getFile("jar/osgi.jar")
		};
		Builder bmaker = new Builder();
		Properties p = new Properties();
		p.put("Export-Package", "org.osgi.framework");
		bmaker.setClasspath(cp);
		bmaker.setProperties(p);
		Jar jar = bmaker.build();
		System.err.println(jar.getResources());
		// System.err.println(bmaker.getExports());
		System.err.println("Warnings: " + bmaker.getWarnings());
		System.err.println("Errors  : " + bmaker.getErrors());
		jar.getManifest()
			.write(System.err);
		Manifest manifest = jar.getManifest();
		Attributes main = manifest.getMainAttributes();
		String export = main.getValue("Export-Package");
		assertNotNull("Export package header", export);
		Parameters map = Processor.parseHeader(export, null);
		assertEquals("1.3", map.get("org.osgi.framework")
			.get("version"));
	}

}
