package test;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import aQute.bnd.test.*;
import aQute.lib.osgi.*;
import aQute.lib.osgi.Descriptors.PackageRef;

class T0 {
}

abstract class T1 extends T0 {
}

class T2 extends T1 {
}

class T3 extends T2 {
}

public class AnalyzerTest extends BndTestCase {

	
	/**
	 * Fastest way to create a manifest
	 * @throws Exception 
	 */
	
	public void testGenerateManifest() throws Exception {
		Analyzer analyzer = new Analyzer();
		Jar bin = new Jar( new File("jar/osgi.jar"));
		bin.setManifest(new Manifest());
		analyzer.setJar( bin );
		analyzer.addClasspath( new File("jar/spring.jar"));
		analyzer.setProperty("Bundle-SymbolicName","org.osgi.core");
		analyzer.setProperty("Export-Package","org.osgi.framework,org.osgi.service.event");
		analyzer.setProperty("Bundle-Version","1.0");
		analyzer.setProperty("Import-Package","*");
		Manifest manifest = analyzer.calcManifest();
		manifest.write(System.out);
		String export = manifest.getMainAttributes().getValue("Export-Package");
		assertEquals("org.osgi.framework;version=\"1.3\",org.osgi.service.event;uses:=\"org.osgi.framework\";version=\"1.0.1\"",export);
		assertEquals("1.0",manifest.getMainAttributes().getValue("Bundle-Version"));
		assertEquals(analyzer.getErrors().toString(), 0, analyzer.getErrors().size());
		assertEquals(analyzer.getWarnings().toString(), 0, analyzer.getWarnings().size());
	}
	
	/**
	 * 
	 * Make sure packages from embedded directories referenced from
	 * 
	 * Bundle-Classpath are considered during import/export calculation.
	 */

	public void testExportContentsDirectory() throws Exception {
		Builder b = new Builder();
		File embedded = new File("bin/aQute/lib").getCanonicalFile();
		assertTrue(embedded.isDirectory()); // sanity check
		b.setProperty("Bundle-ClassPath", ".,jars/some.jar");
		b.setProperty("-includeresource", "jars/some.jar/aQute/lib=" + embedded.getAbsolutePath());
		b.setProperty("-exportcontents", "aQute.lib.osgi");
		b.build();

		assertTrue(b.getExports().toString(), b.getExports().containsKey("aQute.lib.osgi"));

		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
		assertEquals(b.getErrors().toString(), 0, b.getErrors().size());
		assertEquals(b.getWarnings().toString(), 1, b.getWarnings().size());

	}

	/**
	 * Uses constraints must be filtered by imports or exports.
	 * 
	 * @throws Exception
	 */

	public void testUsesFiltering() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("jar/osgi.jar"));
		b.setProperty("Export-Package", "org.osgi.service.event");
		Jar jar = b.build();

		String exports = jar.getManifest().getMainAttributes().getValue("Export-Package");
		System.out.println(exports);
		assertTrue(exports.contains("uses:=\"org.osgi.framework\""));

		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

		b = new Builder();
		b.addClasspath(new File("jar/osgi.jar"));
		b.setProperty("Import-Package", "");
		b.setProperty("Export-Package", "org.osgi.service.event");
		jar = b.build();
		exports = jar.getManifest().getMainAttributes().getValue("Export-Package");
		assertFalse(exports.contains("uses:=\"org.osgi.framework\""));
		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

	}

	/**
	 * Test if require works
	 * 
	 * @throws Exception
	 */

	public void testRequire() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("jar/osgi.jar"));
		b.setProperty("Private-Package", "org.osgi.framework");
		b.setProperty("-require-bnd", "10000");
		b.build();
		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
		assertEquals(1, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());

	}

	public void testComponentImportReference() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("jar/osgi.jar"));
		b.setProperty("Private-Package", "org.osgi.framework");
		b.setProperty("Import-Package", "not.here,*");
		b.setProperty("Service-Component", "org.osgi.framework.Bundle;ref=not.here.Reference");
		b.build();
		System.out.println(b.getErrors());
		System.out.println(b.getWarnings());
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());
	}

	public void testFindClass() throws Exception {
		Builder a = new Builder();
		a.setProperty("Export-Package", "org.osgi.service.io");
		a.addClasspath(new File("jar/osgi.jar"));
		a.build();
		System.out.println(a.getErrors());
		System.out.println(a.getWarnings());

		Collection<Clazz> c = a.getClasses("", "IMPORTS", "javax.microedition.io");
		System.out.println(c);
	}

	public void testMultilevelInheritance() throws Exception {
		Analyzer a = new Analyzer();
		a.setJar(new File("bin"));
		a.analyze();

		String result = a._classes("cmd", "named", "*T?", "extends", "test.T0", "concrete");
		System.out.println(result);
		assertTrue(result.contains("test.T2"));
		assertTrue(result.contains("test.T3"));
	}

	public void testClassQuery() throws Exception {
		Analyzer a = new Analyzer();
		a.setJar(new File("jar/osgi.jar"));
		a.analyze();

		String result = a._classes("cmd", "named", "org.osgi.service.http.*", "abstract");
		TreeSet<String> r = new TreeSet<String>(Processor.split(result));
		assertEquals(
				new TreeSet<String>(Arrays.asList("org.osgi.service.http.HttpContext",
						"org.osgi.service.http.HttpService")), r);
	}

	/**
	 * Use a private activator, check it is not imported.
	 * 
	 * @throws Exception
	 */
	public void testEmptyHeader() throws Exception {
		Builder a = new Builder();
		a.setProperty("Bundle-Blueprint", "  <<EMPTY>> ");
		a.setProperty("Export-Package", "org.osgi.framework");
		a.addClasspath(new File("jar/osgi.jar"));
		a.build();
		Manifest manifest = a.getJar().getManifest();
		System.out.println(a.getErrors());
		System.out.println(a.getWarnings());
		assertEquals(0, a.getErrors().size());
		assertEquals(0, a.getWarnings().size());
		String bb = manifest.getMainAttributes().getValue("Bundle-Blueprint");
		System.out.println(bb);
		assertNotNull(bb);
		assertEquals("", bb);
	}

	/**
	 * Test name section.
	 */

	public void testNameSection() throws Exception {
		Builder a = new Builder();
		a.setProperty("Export-Package", "org.osgi.service.event, org.osgi.service.io");
		a.addClasspath(new File("jar/osgi.jar"));
		a.setProperty("@org@osgi@service@event@Specification-Title", "spec title");
		a.setProperty("@org@osgi@service@io@Specification-Title", "spec title io");
		a.setProperty("@org@osgi@service@event@Specification-Version", "1.1");
		a.setProperty("@org@osgi@service@event@Implementation-Version", "5.1");
		Jar jar = a.build();
		Manifest m = jar.getManifest();
		Attributes attrs = m.getAttributes("org/osgi/service/event");
		assertNotNull(attrs);
		assertEquals("5.1", attrs.getValue("Implementation-Version"));
		assertEquals("1.1", attrs.getValue("Specification-Version"));
		assertEquals("spec title", attrs.getValue("Specification-Title"));

		attrs = m.getAttributes("org/osgi/service/io");
		assertNotNull(attrs);
		assertEquals("spec title io", attrs.getValue("Specification-Title"));
	}

	/**
	 * Check if calcManifest sets the version
	 */

	/**
	 * Test if mandatory attributes are augmented even when the version is not
	 * set.
	 */
	public void testMandatoryWithoutVersion() throws Exception {
		Builder a = new Builder();
		Properties p = new Properties();
		p.put("Import-Package", "*");
		p.put("Private-Package", "org.apache.mina.management.*");
		a.setClasspath(new Jar[] { new Jar(new File("jar/mandatorynoversion.jar")) });
		a.setProperties(p);
		Jar jar = a.build();
		String imports = jar.getManifest().getMainAttributes().getValue("Import-Package");
		System.out.println(imports);
		assertTrue(imports.indexOf("x=1") >= 0);
		assertTrue(imports.indexOf("y=2") >= 0);
	}

	/**
	 * Use a private activator, check it is not imported.
	 * 
	 * @throws Exception
	 */
	public void testPrivataBundleActivatorNotImported() throws Exception {
		Builder a = new Builder();
		Properties p = new Properties();
		p.put("Import-Package", "*");
		p.put("Private-Package", "org.objectweb.*");
		p.put("Bundle-Activator", "org.objectweb.asm.Item");
		a.setClasspath(new Jar[] { new Jar(new File("jar/asm.jar")) });
		a.setProperties(p);
		a.build();
		Manifest manifest = a.getJar().getManifest();
		System.out.println(a.getErrors());
		System.out.println(a.getWarnings());
		assertEquals(0, a.getErrors().size());
		assertEquals(0, a.getWarnings().size());
		String imports = manifest.getMainAttributes().getValue("Import-Package");
		System.out.println(imports);
		assertNull(imports);
	}

	/**
	 * Use an activator that is not in the bundle but do not allow it to be
	 * imported, this should generate an error.
	 * 
	 * @throws Exception
	 */
	public void testBundleActivatorNotImported() throws Exception {
		Builder a = new Builder();
		Properties p = new Properties();
		p.put("Import-Package", "!org.osgi.framework,*");
		p.put("Private-Package", "org.objectweb.*");
		p.put("Bundle-Activator", "org.osgi.framework.BundleActivator");
		a.setClasspath(new Jar[] { new Jar(new File("jar/asm.jar")),
				new Jar(new File("jar/osgi.jar")) });
		a.setProperties(p);
		a.build();
		Manifest manifest = a.getJar().getManifest();
		System.out.println(a.getErrors());
		System.out.println(a.getWarnings());
		assertEquals(1, a.getErrors().size());
		assertTrue(a.getErrors().get(0).indexOf("Bundle-Activator not found") >= 0);
		// assertTrue(a.getErrors().get(1).indexOf("Unresolved references to")
		// >= 0);
		assertEquals(0, a.getWarnings().size());
		String imports = manifest.getMainAttributes().getValue("Import-Package");
		assertNull(imports);
	}

	/**
	 * Use an activator that is on the class path but that is not in the bundle.
	 * 
	 * @throws Exception
	 */
	public void testBundleActivatorImport() throws Exception {
		Builder a = new Builder();
		Properties p = new Properties();
		p.put("Private-Package", "org.objectweb.*");
		p.put("Bundle-Activator", "org.osgi.framework.BundleActivator");
		a.setClasspath(new Jar[] { new Jar(new File("jar/asm.jar")),
				new Jar(new File("jar/osgi.jar")) });
		a.setProperties(p);
		a.build();
		Manifest manifest = a.getJar().getManifest();
		System.out.println(a.getErrors());
		System.out.println(a.getWarnings());
		assertEquals(0, a.getErrors().size());
		assertEquals(0, a.getWarnings().size());
		String imports = manifest.getMainAttributes().getValue("Import-Package");
		assertNotNull(imports);
		assertTrue(imports.indexOf("org.osgi.framework") >= 0);
	}

	/**
	 * The -removeheaders header removes any necessary after the manifest is
	 * calculated.
	 */

	public void testRemoveheaders() throws Exception {
		Analyzer a = new Analyzer();
		a.setJar(new File("jar/asm.jar"));
		Manifest m = a.calcManifest();
		assertNotNull(m.getMainAttributes().getValue("Implementation-Title"));
		a = new Analyzer();
		a.setJar(new File("jar/asm.jar"));
		a.setProperty("-removeheaders", "Implementation-Title");
		m = a.calcManifest();
		assertNull(m.getMainAttributes().getValue("Implementation-Title"));
	}

	/**
	 * There was an export generated for a jar file.
	 * 
	 * @throws Exception
	 */
	public void testExportForJar() throws Exception {
		Jar jar = new Jar("dot");
		jar.putResource("target/aopalliance.jar", new FileResource(new File("jar/asm.jar")));
		Analyzer an = new Analyzer();
		an.setJar(jar);
		Properties p = new Properties();
		p.put("Export-Package", "*");
		an.setProperties(p);
		Manifest manifest = an.calcManifest();
		String exports = manifest.getMainAttributes().getValue(Analyzer.EXPORT_PACKAGE);
		Map<String, Map<String, String>> map = Analyzer.parseHeader(exports, null);
		assertEquals(1, map.size());
		assertEquals("target", map.keySet().iterator().next());
	}

	/**
	 * Test if version works
	 */

	public void testVersion() {
		Analyzer a = new Analyzer();
		String v = a.getBndVersion();
		assertNotNull(v);
	}

	/**
	 * asm is a simple library with two packages. No imports are done.
	 * 
	 */
	public void testAsm() throws Exception {
		Properties base = new Properties();
		base.put(Analyzer.IMPORT_PACKAGE, "*");
		base.put(Analyzer.EXPORT_PACKAGE, "*;-noimport:=true");

		Analyzer analyzer = new Analyzer();
		analyzer.setJar(new File("jar/asm.jar"));
		analyzer.setProperties(base);
		analyzer.calcManifest().write(System.out);
		assertTrue(analyzer.getExports().containsKey("org.objectweb.asm.signature"));
		assertTrue(analyzer.getExports().containsKey("org.objectweb.asm"));
		assertFalse(analyzer.getImports().containsKey("org.objectweb.asm.signature"));
		assertFalse(analyzer.getImports().containsKey("org.objectweb.asm"));
		assertEquals("Expected size", 2, analyzer.getExports().size());
	}

	/**
	 * See if we set attributes on export
	 * 
	 * @throws IOException
	 */
	public void testAsm2() throws Exception {
		Properties base = new Properties();
		base.put(Analyzer.IMPORT_PACKAGE, "*");
		base.put(Analyzer.EXPORT_PACKAGE,
				"org.objectweb.asm;name=short, org.objectweb.asm.signature;name=long");
		Analyzer h = new Analyzer();
		h.setJar(new File("jar/asm.jar"));
		h.setProperties(base);
		h.calcManifest().write(System.out);
		assertPresent(h.getExports().keySet(), "org.objectweb.asm.signature, org.objectweb.asm");
		assertTrue(Arrays.asList("org.objectweb.asm", "org.objectweb.asm.signature").removeAll(
				h.getImports().keySet()) == false);
		assertEquals("Expected size", 2, h.getExports().size());
		assertEquals("short", get(h.getExports(), h.getPackageRef("org.objectweb.asm"), "name"));
		assertEquals("long", get(h.getExports(), h.getPackageRef("org.objectweb.asm.signature"), "name"));
	}

	public void testDs() throws Exception {
		Properties base = new Properties();
		base.put(Analyzer.IMPORT_PACKAGE, "*");
		base.put(Analyzer.EXPORT_PACKAGE, "*;-noimport:=true");
		File tmp = new File("jar/ds.jar");
		Analyzer analyzer = new Analyzer();
		analyzer.setJar(tmp);
		analyzer.setProperties(base);
		System.out.println(analyzer.calcManifest());
		assertPresent(analyzer.getImports().keySet(), "org.osgi.service.packageadmin, "
				+ "org.xml.sax, org.osgi.service.log," + " javax.xml.parsers,"
				+ " org.xml.sax.helpers," + " org.osgi.framework," + " org.eclipse.osgi.util,"
				+ " org.osgi.util.tracker, " + "org.osgi.service.component, "
				+ "org.osgi.service.cm");
		assertPresent(analyzer.getExports().keySet(), "org.eclipse.equinox.ds.parser, "
				+ "org.eclipse.equinox.ds.tracker, " + "org.eclipse.equinox.ds, "
				+ "org.eclipse.equinox.ds.instance, " + "org.eclipse.equinox.ds.model, "
				+ "org.eclipse.equinox.ds.resolver, " + "org.eclipse.equinox.ds.workqueue");

	}

	public void testDsSkipOsgiImport() throws Exception {
		Properties base = new Properties();
		base.put(Analyzer.IMPORT_PACKAGE, "!org.osgi.*, *");
		base.put(Analyzer.EXPORT_PACKAGE, "*;-noimport:=true");
		File tmp = new File("jar/ds.jar");
		Analyzer h = new Analyzer();
		h.setJar(tmp);
		h.setProperties(base);
		h.calcManifest().write(System.out);
		assertPresent(h.getImports().keySet(), "org.xml.sax, " + " javax.xml.parsers,"
				+ " org.xml.sax.helpers," + " org.eclipse.osgi.util");

		System.out.println("IMports " + h.getImports());
		assertNotPresent(h.getImports().keySet(), "org.osgi.service.packageadmin, "
				+ "org.osgi.service.log," + " org.osgi.framework," + " org.osgi.util.tracker, "
				+ "org.osgi.service.component, " + "org.osgi.service.cm");
		assertPresent(h.getExports().keySet(), "org.eclipse.equinox.ds.parser, "
				+ "org.eclipse.equinox.ds.tracker, " + "org.eclipse.equinox.ds, "
				+ "org.eclipse.equinox.ds.instance, " + "org.eclipse.equinox.ds.model, "
				+ "org.eclipse.equinox.ds.resolver, " + "org.eclipse.equinox.ds.workqueue");
	}

	public void testDsNoExport() throws Exception {
		Properties base = new Properties();
		base.put(Analyzer.IMPORT_PACKAGE, "*");
		base.put(Analyzer.EXPORT_PACKAGE, "!*");
		File tmp = new File("jar/ds.jar");
		Analyzer h = new Analyzer();
		h.setJar(tmp);
		h.setProperties(base);
		h.calcManifest().write(System.out);
		assertPresent(h.getImports().keySet(), "org.osgi.service.packageadmin, "
				+ "org.xml.sax, org.osgi.service.log," + " javax.xml.parsers,"
				+ " org.xml.sax.helpers," + " org.osgi.framework," + " org.eclipse.osgi.util,"
				+ " org.osgi.util.tracker, " + "org.osgi.service.component, "
				+ "org.osgi.service.cm");
		assertNotPresent(h.getExports().keySet(), "org.eclipse.equinox.ds.parser, "
				+ "org.eclipse.equinox.ds.tracker, " + "org.eclipse.equinox.ds, "
				+ "org.eclipse.equinox.ds.instance, " + "org.eclipse.equinox.ds.model, "
				+ "org.eclipse.equinox.ds.resolver, " + "org.eclipse.equinox.ds.workqueue");
		System.out.println(h.getUnreachable());
	}

	public void testClasspath() throws Exception {
		Properties base = new Properties();
		base.put(Analyzer.IMPORT_PACKAGE, "*");
		base.put(Analyzer.EXPORT_PACKAGE, "*;-noimport:=true");
		File tmp = new File("jar/ds.jar");
		File osgi = new File("jar/osgi.jar");
		Analyzer h = new Analyzer();
		h.setJar(tmp);
		h.setProperties(base);
		h.setClasspath(new File[] { osgi });
		h.calcManifest().write(System.out);
		assertEquals("Version from osgi.jar", "[1.2,2)",
				get(h.getImports(), h.getPackageRef("org.osgi.service.packageadmin"), "version"));
		assertEquals("Version from osgi.jar", "[1.3,2)",
				get(h.getImports(), h.getPackageRef("org.osgi.util.tracker"), "version"));
		assertEquals("Version from osgi.jar", null, get(h.getImports(), h.getPackageRef("org.xml.sax"), "version"));

	}

	/**
	 * We detect that there are instruction on im/export package headers that
	 * are never used. This usually indicates a misunderstanding or a change in
	 * the underlying classpath. These are reflected as warnings. If there is an
	 * extra import, and it contains no wildcards, then it is treated as a
	 * wildcard
	 * 
	 * @throws IOException
	 */
	public void testSuperfluous() throws Exception {
		Properties base = new Properties();
		base.put(Analyzer.IMPORT_PACKAGE, "*, com.foo, com.foo.bar.*");
		base.put(Analyzer.EXPORT_PACKAGE, "*, com.bar");
		File tmp = new File("jar/ds.jar");
		Analyzer h = new Analyzer();
		h.setJar(tmp);
		h.setProperties(base);
		h.calcManifest().write(System.out);
		List<String> warnings = h.getWarnings();
		assertEquals(warnings.size(), 2);
		assertEquals("Superfluous export-package instructions: [com.bar]", warnings.get(0));
		assertEquals("Did not find matching referal for com.foo.bar.*", warnings.get(1));
		assertTrue(h.getImports().containsKey("com.foo"));
	}

	void assertNotPresent(Collection<?> map, String string) {
		Collection<String> ss = new HashSet<String>();
		for ( Object o : map)
			ss.add(o+"");
		
		StringTokenizer st = new StringTokenizer(string, ", ");
		while (st.hasMoreTokens()) {
			String member = st.nextToken();
			assertFalse("Must not contain  " + member, map.contains(member));
		}
	}

	void assertPresent(Collection<?> map, String string) {
		Collection<String> ss = new HashSet<String>();
		for ( Object o : map)
			ss.add(o+"");
		
		StringTokenizer st = new StringTokenizer(string, ", ");
		while (st.hasMoreTokens()) {
			String member = st.nextToken();
			assertTrue("Must contain  " + member, map.contains(member));
		}
	}

	<K,V> V get(Map<K, Map<String,V>> headers, K key, String attr) {
		Map<String, V> clauses = headers.get(key);
		if (clauses == null)
			return null;
		return clauses.get(attr);
	}
}
