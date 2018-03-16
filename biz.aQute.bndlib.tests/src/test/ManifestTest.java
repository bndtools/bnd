package test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.lib.io.IO;
import junit.framework.TestCase;

@SuppressWarnings("resource")
public class ManifestTest extends TestCase {

	public static void testNameSection() throws Exception {
		Builder b = new Builder();
		b.setProperty("Export-Package", "org.osgi.framework");
		b.addClasspath(IO.getFile("jar/osgi.jar"));

		Jar jar = b.build();
		jar.calcChecksums(null);
		File f = File.createTempFile("abc", ".jar");
		f.deleteOnExit();
		jar.write(f);

		jar = new Jar(f);
		f.delete();
		assertTrue(b.check());

		Resource r = jar.getResource("META-INF/MANIFEST.MF");
		assertNotNull(r);

		// String ms = IO.collect( r.openInputStream());

		Manifest m = new Manifest(r.openInputStream());

		assertEquals(31, m.getEntries()
			.size());

		Attributes ba = m.getAttributes("org/osgi/framework/BundleActivator.class");
		assertNotNull(ba);
		assertEquals("RTRhr3kadnulINegRhpmog==", ba.getValue("MD5-Digest"));

		Attributes bundle = m.getAttributes("org/osgi/framework/Bundle.class");
		assertNotNull(bundle);
		assertEquals("fpQdL60w3CQK+7xlXtM6oA==", bundle.getValue("MD5-Digest"));

		Attributes sl = m.getAttributes("org/osgi/framework/ServiceListener.class");
		assertNotNull(sl);
		assertEquals("nzDRN19MrTJG+LP8ayKZITZ653g=", sl.getValue("SHA-Digest"));

	}

	public static void testUnicode() throws Exception {
		Builder b = new Builder();
		String longSentence = "\u1401\u1402\u1403\u1404\u1405\u1406\u1407\u1408\u1409\u140A\u140B\u140C\u140D\u140E\u140F\u1410\u1411\u1412\u1413\u1414\u1415\u1416\u1417\u1418\u1419\u141A\u141B\u141C\u141D\u141E\u141F\u1420\u1421\u1422\u1422\u1423\u1424\u1425\u1426\u1427\u1428\u1429\u1429\u142A\u142B\u142C\u142D\u142E\u142F\u1430\u1431\u1432\u1433\u1434\u1435\u1436\u1437\u1438\u1439\u143A\u143B\u143C\u143D\u143E\u143F\u1440\u1441\u1442\u1443\u1444\u1444\u1445\u1446\u1447\u1448\u1449\u144A\u144B\u144C\u144D";
		String shortSentence = "\u1401\u1402\u1403\u1404\u1405\u1406\u1407\u1408\u1409\u140A\u140B\u140C\u140D\u140E\u140F\u1410\u1411\u1412\u1413\u1414\u1415\u1416";
		assertEquals(66, shortSentence.getBytes("UTF-8").length);
		assertEquals(22, shortSentence.length());

		b.setProperty("A1", shortSentence);
		b.setProperty("A11", shortSentence);
		b.setProperty("A111", shortSentence);
		b.setProperty("A1111", shortSentence);
		b.setProperty("Long", longSentence);

		b.setProperty("-resourceonly", "true");
		b.setProperty("Include-Resource", "jar/osgi.jar");
		Jar jar = b.build();
		File f = File.createTempFile("abc", ".jar");
		f.deleteOnExit();
		jar.write(f);

		jar = new Jar(f);
		f.delete();

		assertEquals(0, b.getErrors()
			.size());
		assertEquals(0, b.getWarnings()
			.size());
		Resource r = jar.getResource("META-INF/MANIFEST.MF");
		assertNotNull(r);

		Manifest m = new Manifest(r.openInputStream());
		// String ms = IO.collect(r.openInputStream());

		assertEquals(shortSentence, m.getMainAttributes()
			.getValue("A1"));
		assertEquals(shortSentence, m.getMainAttributes()
			.getValue("A11"));
		assertEquals(shortSentence, m.getMainAttributes()
			.getValue("A111"));
		assertEquals(shortSentence, m.getMainAttributes()
			.getValue("A1111"));
		assertEquals(longSentence, m.getMainAttributes()
			.getValue("Long"));

	}

	public static void test72() throws Exception {
		Builder b = new Builder();
		b.setProperty("H65", "01234567890123456789012345678901234567890123456789012345678901234");
		b.setProperty("H66", "012345678901234567890123456789012345678901234567890123456789012345");
		b.setProperty("H67", "0123456789012345678901234567890123456789012345678901234567890123456");
		b.setProperty("H68", "01234567890123456789012345678901234567890123456789012345678901234567");
		b.setProperty("H69", "012345678901234567890123456789012345678901234567890123456789012345678");
		b.setProperty("-resourceonly", "true");
		b.setProperty("Include-Resource", "jar/osgi.jar");
		Jar jar = b.build();
		File f = File.createTempFile("abc", ".jar");
		f.deleteOnExit();
		jar.write(f);

		jar = new Jar(f);
		f.delete();

		assertEquals(0, b.getErrors()
			.size());
		assertEquals(0, b.getWarnings()
			.size());
		Resource r = jar.getResource("META-INF/MANIFEST.MF");
		assertNotNull(r);

		Manifest m = new Manifest(r.openInputStream());
		String ms = IO.collect(r.openInputStream());

		assertEquals(65, m.getMainAttributes()
			.getValue("H65")
			.length());
		assertEquals(66, m.getMainAttributes()
			.getValue("H66")
			.length());
		assertEquals(67, m.getMainAttributes()
			.getValue("H67")
			.length());
		assertEquals(68, m.getMainAttributes()
			.getValue("H68")
			.length());
		assertEquals(69, m.getMainAttributes()
			.getValue("H69")
			.length());

		assertTrue(Pattern.compile("H65: \\d{65}\r\n")
			.matcher(ms)
			.find());
		assertTrue(Pattern.compile("H66: \\d{65}\r\n \\d{1}\r\n")
			.matcher(ms)
			.find());
		assertTrue(Pattern.compile("H67: \\d{65}\r\n \\d{2}\r\n")
			.matcher(ms)
			.find());
		assertTrue(Pattern.compile("H68: \\d{65}\r\n \\d{3}\r\n")
			.matcher(ms)
			.find());
		assertTrue(Pattern.compile("H69: \\d{65}\r\n \\d{4}\r\n")
			.matcher(ms)
			.find());
	}

	public static void testNoManifest() throws Exception {
		Builder b = new Builder();
		b.setProperty("-nomanifest", "true");
		b.setProperty("Export-Package", "org.osgi.service.event.*");
		b.addClasspath(IO.getFile("jar/osgi.jar"));
		Jar jar = b.build();
		assertNull(jar.getResource("META-INF/MANIFEST.MF"));
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		jar.write(bout);
		ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
		JarInputStream in = new JarInputStream(bin);
		ZipEntry entry = in.getNextEntry();
		assertNotNull(entry);
		assertNull(entry.getExtra());
	}

	public static void testRenameManifest() throws Exception {
		Builder b = new Builder();
		b.setProperty("-manifest-name", "META-INF/FESTYMAN.MF");
		b.setProperty("Subsystem-Wibble", "hullo");
		b.setProperty("Export-Package", "org.osgi.service.event.*");

		b.addClasspath(IO.getFile("jar/osgi.jar"));
		Jar jar = b.build();

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		jar.write(bout);
		b.close();
		jar.close();

		ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
		ZipInputStream zin = new ZipInputStream(bin);
		ZipEntry firstEntry = zin.getNextEntry();
		if (firstEntry.getName()
			.equalsIgnoreCase("META-INF/")) {
			firstEntry = zin.getNextEntry();
		}

		assertEquals("META-INF/FESTYMAN.MF", firstEntry.getName());
		Manifest manifest = new Manifest(zin);
		assertEquals("hullo", manifest.getMainAttributes()
			.getValue("Subsystem-Wibble"));
		zin.close();
	}

	public static void testNames() throws Exception {
		Manifest m = new Manifest();
		m.getMainAttributes()
			.putValue("Manifest-Version", "1.0");
		m.getMainAttributes()
			.putValue("x", "Loïc Cotonéa");
		m.getMainAttributes()
			.putValue("y", "Loïc Cotonéa");
		m.getMainAttributes()
			.putValue("z", "Loïc Cotonéa");

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		Jar.writeManifest(m, bout);
		byte[] result = bout.toByteArray();

		System.err.println(new String(result));
	}

	public static void testUTF8() throws Exception {
		Manifest m = new Manifest();
		m.getMainAttributes()
			.putValue("Manifest-Version", "1.0");
		m.getMainAttributes()
			.putValue("x", "Loïc Cotonéa");
		m.getMainAttributes()
			.putValue("y", "Loïc Cotonéa");
		m.getMainAttributes()
			.putValue("z", "Loïc Cotonéa");

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		Jar.writeManifest(m, bout);
		byte[] result = bout.toByteArray();

		System.err.println(new String(result));
	}

	public static void testQuotes() throws IOException {
		Map<String, Map<String, String>> map = new HashMap<>();
		Map<String, String> clause = new HashMap<>();
		clause.put("version1", "0");
		clause.put("version2", "0.0");
		clause.put("version3", "\"0.0\"");
		clause.put("version4", "   \"0.0\"    ");
		clause.put("version5", "   0.0    ");
		map.put("alpha", clause);
		String s = Processor.printClauses(map);
		assertTrue(s.contains("version1=0"));
		assertTrue(s.contains("version2=\"0.0\""));
		assertTrue(s.contains("version3=\"0.0\""));
		assertTrue(s.contains("version4=\"0.0\""));
		assertTrue(s.contains("version5=\"0.0\""));
	}
}
